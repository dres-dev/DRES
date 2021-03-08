package dev.dres.run

import dev.dres.api.rest.types.WebSocketConnection
import dev.dres.api.rest.types.run.websocket.ClientMessage
import dev.dres.api.rest.types.run.websocket.ClientMessageType
import dev.dres.api.rest.types.run.websocket.ServerMessage
import dev.dres.api.rest.types.run.websocket.ServerMessageType
import dev.dres.data.model.UID
import dev.dres.data.model.competition.CompetitionDescription
import dev.dres.data.model.competition.TaskDescription
import dev.dres.data.model.competition.TeamId
import dev.dres.data.model.run.*
import dev.dres.data.model.run.interfaces.Task
import dev.dres.data.model.submissions.Submission
import dev.dres.data.model.submissions.SubmissionStatus
import dev.dres.run.audit.AuditLogger
import dev.dres.run.audit.LogEventSource
import dev.dres.run.eventstream.EventStreamProcessor
import dev.dres.run.eventstream.TaskEndEvent
import dev.dres.run.exceptions.IllegalRunStateException
import dev.dres.run.exceptions.IllegalTeamIdException
import dev.dres.run.score.ScoreTimePoint
import dev.dres.run.score.scoreboard.Scoreboard
import dev.dres.run.updatables.*
import dev.dres.run.validation.interfaces.JudgementValidator
import dev.dres.utilities.extensions.UID
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.collections.HashMap
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.math.max

/**
 * An implementation of a [RunManager] aimed at distributed execution having a single DRES Server instance and
 * multiple clients connected via WebSocket.
 *
 * As opposed to the [InteractiveSynchronousRunManager], competitions in the [InteractiveAsynchronousRunManager]
 * can take place at different points in time for different teams and tasks, i.e., the competitions are executed
 * asynchronously.
 *
 * @version 1.0.0
 * @author Ralph Gasser
 */
class InteractiveAsynchronousRunManager(private val run: InteractiveAsynchronousCompetition) : InteractiveRunManager {

    companion object {
        private val LOGGER = LoggerFactory.getLogger(InteractiveAsynchronousRunManager::class.java)
        private const val SCOREBOARD_UPDATE_INTERVAL_MS = 1000L // TODO make configurable
        private const val MAXIMUM_ERROR_COUNT = 5
    }

    /** Tracks the current [TaskDescription] per [TeamId]. */
    private val navigationMap: MutableMap<TeamId, TaskDescription> = HashMap()

    /** Tracks the current [TaskDescription] per [TeamId]. */
    private val statusMap: MutableMap<TeamId, RunManagerStatus> = HashMap()

    /** A [Map] of all viewers, i.e., DRES clienst currently registered with this [InteractiveAsynchronousRunManager]. */
    private val viewers = ConcurrentHashMap<WebSocketConnection,Boolean>()

    /** A lock for state changes to this [InteractiveAsynchronousRunManager]. */
    private val stateLock = ReentrantReadWriteLock()

    /** The internal [ScoreboardsUpdatable] instance for this [InteractiveSynchronousRunManager]. */
    private val scoreboardsUpdatable = ScoreboardsUpdatable(this.description.generateDefaultScoreboards(), SCOREBOARD_UPDATE_INTERVAL_MS, this.run)

    /** The internal [MessageQueueUpdatable] instance used by this [InteractiveSynchronousRunManager]. */
    private val messageQueueUpdatable = MessageQueueUpdatable(RunExecutor)

    /** The internal [ScoresUpdatable] instance for this [InteractiveSynchronousRunManager]. */
    private val scoresUpdatable = ScoresUpdatable(this.id, this.scoreboardsUpdatable, this.messageQueueUpdatable)

    /** The internal [DAOUpdatable] instance used by this [InteractiveSynchronousRunManager]. */
    private val daoUpdatable = DAOUpdatable(RunExecutor.runs, this.run)

    /** The internal [DAOUpdatable] used to end a task once no more submissions are possible */
    private val endTaskUpdatable = EndTaskUpdatable(this, RunActionContext.INTERNAL)

    /** List of [Updatable] held by this [InteractiveAsynchronousRunManager]. */
    private val updatables = mutableListOf<Updatable>()

    /** Run ID of this [InteractiveAsynchronousCompetition]. */
    override val id: UID
        get() = this.run.id

    /** Name of this [InteractiveAsynchronousCompetition]. */
    override val name: String
        get() = this.run.name

    /** The [CompetitionDescription] executed by this [InteractiveSynchronousRunManager]. */
    override val description: CompetitionDescription
        get() = this.run.description

    /** The global [RunManagerStatus] of this [InteractiveAsynchronousRunManager]. */
    @Volatile
    override var status: RunManagerStatus = if (this.run.hasStarted) { RunManagerStatus.ACTIVE } else { RunManagerStatus.CREATED }
        private set

    override val judgementValidators: List<JudgementValidator>
        get() = this.run.tasks.mapNotNull { if (it.hasStarted && it.validator is JudgementValidator) it.validator else null }

    override val scoreboards: List<Scoreboard>
        get() = this.scoreboardsUpdatable.scoreboards

    override val scoreHistory: List<ScoreTimePoint>
        get() = this.scoreboardsUpdatable.timeSeries

    override val allSubmissions: List<Submission>
        get() = this.stateLock.read { this.run.tasks.flatMap { it.submissions } }

    init {
        /* Register relevant Updatables. */
        this.updatables.add(this.scoresUpdatable)
        this.updatables.add(this.scoreboardsUpdatable)
        this.updatables.add(this.messageQueueUpdatable)
        this.updatables.add(this.daoUpdatable)
        this.updatables.add(this.endTaskUpdatable)

        /* Initialize map and set all tasks pointers to the first task. */
        this.description.teams.forEach {
            this.navigationMap[it.uid] = this.description.tasks[0]
            this.statusMap[it.uid] = if (this.run.hasStarted) { RunManagerStatus.ACTIVE } else { RunManagerStatus.CREATED }

            /** End ongoing runs upon initialization (in case server crashed during task execution). */
            if (this.run.tasksForTeam(it.uid).last().isRunning) {
                this.run.tasksForTeam(it.uid).last().end()
            }
        }

        /** Re-enqueue pending submissions for judgement (if any). */
        this.run.tasks.forEach { run ->
            run.submissions.filter { it.status == SubmissionStatus.INDETERMINATE }.forEach {
                run.validator.validate(it)
            }
        }

        /** Re-calculate all the relevant scores. */
        this.run.tasks.forEach { run ->
            run.submissions.forEach { sub ->
                this.scoresUpdatable.enqueue(Pair(run, sub))
            }
        }
        this.scoresUpdatable.update(this.status)
    }

    /**
     * Starts this [InteractiveAsynchronousCompetition] moving [RunManager.status] from [RunManagerStatus.CREATED] to
     * [RunManagerStatus.ACTIVE] for all teams. This can only be executed by an administrator.
     *
     * As all state affecting methods, this method throws an [IllegalStateException] if invocation does not match the current state.
     *
     * @param context The [RunActionContext] for this invocation.
     * @throws IllegalStateException If [RunManager] was not in status [RunManagerStatus.CREATED]
     */
    override fun start(context: RunActionContext) = this.stateLock.write {
        checkGlobalStatus(RunManagerStatus.CREATED)
        if (context.isAdmin) {
            /* Start the run. */
            this.run.start()

            /* Update status. */
            this.statusMap.forEach { (t, _) -> this.statusMap[t] = RunManagerStatus.ACTIVE }
            this.status = RunManagerStatus.ACTIVE

            /* Mark DAO for update. */
            this.daoUpdatable.dirty = true

            /* Enqueue WS message for sending */
            this.messageQueueUpdatable.enqueue(ServerMessage(this.id.string, ServerMessageType.COMPETITION_START))

            LOGGER.info("Run manager ${this.id} started")
        }
    }

    /**
     * Ends this [InteractiveAsynchronousCompetition] moving [RunManager.status] from [RunManagerStatus.ACTIVE] to
     * [RunManagerStatus.TERMINATED] for all teams.  This can only be executed by an administrator.
     *
     * As all state affecting methods, this method throws an [IllegalStateException] if invocation
     * does not match the current state.
     *
     * @param context The [RunActionContext] for this invocation.
     * @throws IllegalStateException If [RunManager] was not in status [RunManagerStatus.ACTIVE]
     */
    override fun end(context: RunActionContext) {
        checkGlobalStatus(RunManagerStatus.CREATED, RunManagerStatus.ACTIVE)
        if (context.isAdmin) {
            /* End the run. */
            this.run.end()

            /* Update status. */
            this.statusMap.forEach { (t, _) -> this.statusMap[t] = RunManagerStatus.TERMINATED }
            this.status = RunManagerStatus.TERMINATED

            /* Mark DAO for update. */
            this.daoUpdatable.dirty = true

            /* Enqueue WS message for sending */
            this.messageQueueUpdatable.enqueue(ServerMessage(this.id.string, ServerMessageType.COMPETITION_END))

            LOGGER.info("SynchronousRunManager ${this.id} terminated")
        }
    }

    /**
     * Returns the currently active [TaskDescription] for the given team. Requires [RunManager.status] for the requesting team
     * to be [RunManagerStatus.ACTIVE].
     *
     * @param context The [RunActionContext] used for the invocation.
     * @return The [TaskDescription] for the given team.
     */
    override fun currentTaskDescription(context: RunActionContext): TaskDescription {
        require(context.teamId != null) { "TeamId missing from RunActionContext, which is required for interaction with InteractiveAsynchronousRunManager."}
        return this.navigationMap[context.teamId] ?: throw IllegalTeamIdException(context.teamId)
    }

    /**
     * Prepares this [InteractiveAsynchronousCompetition] for the execution of previous [TaskDescription]
     * as per order defined in [CompetitionDescription.tasks]. Requires [RunManager.status] for the requesting team
     * to be [RunManagerStatus.ACTIVE].
     *
     * As all state affecting methods, this method throws an [IllegalStateException] if invocation
     * does not match the current state.
     *
     * @param context The [RunActionContext] used for the invocation.
     * @return True if [TaskDescription] was moved, false otherwise. Usually happens if last [TaskDescription] has been reached.
     * @throws IllegalStateException If [RunManager] was not in status [RunManagerStatus.ACTIVE]
     */
    override fun previous(context: RunActionContext): Boolean = this.stateLock.write {
        val newIndex = this.description.tasks.indexOf(this.currentTaskDescription(context)) + 1
        return try {
            this.goTo(context, newIndex)
            true
        } catch (e: IndexOutOfBoundsException) {
            false
        }
    }

    /**
     * Prepares this [InteractiveAsynchronousCompetition] for the execution of next [TaskDescription]
     * as per order defined in [CompetitionDescription.tasks]. Requires [RunManager.status] for the requesting
     * team to be [RunManagerStatus.ACTIVE].
     *
     * As all state affecting methods, this method throws an [IllegalStateException] if invocation does not match the current state.
     *
     * @param context The [RunActionContext] used for the invocation.
     * @return True if [TaskDescription] was moved, false otherwise. Usually happens if last [TaskDescription] has been reached.
     * @throws IllegalStateException If [RunManager] was not in status [RunManagerStatus.ACTIVE]
     */
    override fun next(context: RunActionContext): Boolean = this.stateLock.write  {
        val newIndex = this.description.tasks.indexOf(this.currentTaskDescription(context)) + 1
        return try {
            this.goTo(context, newIndex)
            true
        } catch (e: IndexOutOfBoundsException) {
            false
        }
    }

    /**
     * Prepares this [InteractiveAsynchronousCompetition] for the execution of [TaskDescription] with the given [index]
     * as per order defined in [CompetitionDescription.tasks]. Requires [RunManager.status] for the requesting
     * team to be [RunManagerStatus.ACTIVE].
     *
     * As all state affecting methods, this method throws an [IllegalStateException] if invocation does not match the current state.
     *
     * @param context The [RunActionContext] used for the invocation.
     * @return True if [TaskDescription] was moved, false otherwise. Usually happens if last [TaskDescription] has been reached.
     * @throws IllegalStateException If [RunManager] was not in status [RunManagerStatus.ACTIVE]
     */
    override fun goTo(context: RunActionContext, index: Int) = this.stateLock.write {
        require(context.teamId != null) { "TeamId is missing from action context, which is required for interaction with run manager."}
        checkTeamStatus(context.teamId, RunManagerStatus.ACTIVE, RunManagerStatus.TASK_ENDED)
        if (index >= 0 && index < this.description.tasks.size) {

            /* Update active task. */
            this.navigationMap[context.teamId] = this.description.tasks[index]
            this.statusMap[context.teamId] = RunManagerStatus.ACTIVE

            /* Mark scoreboards for update. */
            this.scoreboardsUpdatable.dirty = true

            /* Enqueue WS message for sending */
            this.messageQueueUpdatable.enqueue(ServerMessage(this.id.string, ServerMessageType.COMPETITION_UPDATE))

            LOGGER.info("SynchronousRunManager ${this.id} set to task $index")
        } else {
            throw IndexOutOfBoundsException("Index $index is out of bounds for the number of available tasks.")
        }

    }
    /**
     * Starts the [currentTask] for the given team and thus moves the [InteractiveAsynchronousRunManager.status] from [RunManagerStatus.ACTIVE] to
     * either [RunManagerStatus.PREPARING_TASK] or [RunManagerStatus.RUNNING_TASK]
     *
     * TODO: Should a team be able to start the same task multiple times? How would this affect scoring?
     *
     * As all state affecting methods, this method throws an [IllegalStateException] if invocation oes not match the current state.
     *
     * @param context The [RunActionContext] used for the invocation.
     * @throws IllegalStateException If [InteractiveRunManager] was not in status [RunManagerStatus.ACTIVE] or [currentTask] is not set.
     */
    override fun startTask(context: RunActionContext) = this.stateLock.write {
        require(context.teamId != null) { "TeamId is missing from action context, which is required for interaction with run manager."}
        checkTeamStatus(context.teamId, RunManagerStatus.ACTIVE)

        /* Create task and update status. */
        val currentTask = this.navigationMap[context.teamId] ?: throw IllegalStateException("Could not find active task for team ${context.teamId} despite status of the team being ${this.statusMap[context.teamId]}. This is a programmer's error!")
        this.run.Task(teamId = context.teamId, descriptionId = this.navigationMap[context.teamId]!!.id)
        this.statusMap[context.teamId] = RunManagerStatus.PREPARING_TASK

        /* Mark scoreboards and DAO for update. */
        this.scoreboardsUpdatable.dirty = true
        this.daoUpdatable.dirty = true

        LOGGER.info("Run manager  ${this.id} started task $currentTask.")
    }

    /**
     * Force-abort the [currentTask] and thus moves the [InteractiveAsynchronousRunManager.status] for the given team from
     * [RunManagerStatus.PREPARING_TASK] or [RunManagerStatus.RUNNING_TASK] to [RunManagerStatus.ACTIVE]
     *
     * TODO: Do we want users to be able to do this? If yes, I'd argue that the ability to repeat a task is a requirement.
     *
     * As all state affecting methods, this method throws an [IllegalStateException] if invocation does not match the current state.
     *
     * @param context The [RunActionContext] used for the invocation.
     * @throws IllegalStateException If [InteractiveRunManager] was not in status [RunManagerStatus.RUNNING_TASK].
     */
    override fun abortTask(context: RunActionContext) = this.stateLock.write {
        require(context.teamId != null) { "TeamId is missing from action context, which is required for interaction with run manager."}
        checkTeamStatus(context.teamId, RunManagerStatus.PREPARING_TASK, RunManagerStatus.RUNNING_TASK)

        /* End TaskRun and update status. */
        val currentTask = this.currentTask(context) ?: throw IllegalStateException("Could not find active task for team ${context.teamId} despite status of the team being ${this.statusMap[context.teamId]}. This is a programmer's error!")
        currentTask.end()
        this.statusMap[context.teamId] = RunManagerStatus.TASK_ENDED

        /* Mark scoreboards and DAO for update. */
        this.scoreboardsUpdatable.dirty = true
        this.daoUpdatable.dirty = true

        /* Enqueue WS message for sending */
        this.messageQueueUpdatable.enqueue(ServerMessage(this.id.string, ServerMessageType.TASK_END))

        LOGGER.info("Run manager ${this.id} aborted task $currentTask.")
    }

    /**
     * Returns the time  in milliseconds that is left until the end of the currently running task for the given team.
     * Only works if the [InteractiveAsynchronousRunManager] is in state [RunManagerStatus.RUNNING_TASK]. If no task is running,
     * this method returns -1L.
     *
     * @param context The [RunActionContext] used for the invocation.
     * @return Time remaining until the task will end or -1, if no task is running.
     */
    override fun timeLeft(context: RunActionContext): Long = this.stateLock.read {
        require(context.teamId != null) { "TeamId is missing from action context, which is required for interaction with run manager."}
        if (this.statusMap[context.teamId] == RunManagerStatus.RUNNING_TASK) {
            val currentTaskRun = this.currentTask(context) ?: throw IllegalStateException("Run manager is in status ${this.status} but has no active task. This is a programmer's error!")
            return max(0L, currentTaskRun.duration * 1000L - (System.currentTimeMillis() - currentTaskRun.started!!))
        } else {
            -1L
        }
    }

    /**
     * Returns the number of  [AbstractInteractiveTask]s for this [InteractiveAsynchronousRunManager].
     *
     * Depending on the [RunActionContext], the number may vary.
     *
     * @param context The [RunActionContext] used for the invocation.
     * @return [List] of [AbstractInteractiveTask]s
     */
    override fun taskCount(context: RunActionContext): Int {
        if (context.isAdmin) return this.run.tasks.size
        require(context.teamId != null) { "TeamId is missing from action context, which is required for interaction with run manager."}
        return this.run.tasksForTeam(context.teamId).size
    }

    /**
     * Returns a [List] of all [AbstractInteractiveTask]s for this [InteractiveAsynchronousRunManager].
     *
     * Depending on the [RunActionContext], the list may vary.
     *
     * @param context The [RunActionContext] used for the invocation.
     * @return [List] of [AbstractInteractiveTask]s
     */
    override fun tasks(context: RunActionContext): List<AbstractInteractiveTask> {
        if (context.isAdmin) return this.run.tasks
        require(context.teamId != null) { "TeamId is missing from action context, which is required for interaction with run manager."}
        return this.run.tasksForTeam(context.teamId)
    }

    /**
     * Returns [AbstractInteractiveTask]s for a specific task [UID]. May be empty.
     *
     * @param context The [RunActionContext] used for the invocation.
     * @param taskId The [UID] of the [AbstractInteractiveTask].
     */
    override fun taskForId(context: RunActionContext, taskId: UID): AbstractInteractiveTask? = this.tasks(context).find { it.uid == taskId }

    /**
     * Returns a reference to the currently active [AbstractInteractiveTask].
     *
     * @param context The [RunActionContext] used for the invocation.
     * @return [AbstractInteractiveTask] that is currently active or null, if no such task is active.
     */
    override fun currentTask(context: RunActionContext): AbstractInteractiveTask? = this.stateLock.read {
        require(context.teamId != null) { "TeamId is missing from action context, which is required for interaction with run manager."}
        return this.run.currentTaskForTeam(context.teamId)
    }

    /**
     * Returns the [Submission]s for all currently active [AbstractInteractiveTask]s or an empty [List], if no such task is active.
     *
     * @param context The [RunActionContext] used for the invocation.
     * @return List of [Submission]s for the currently active [AbstractInteractiveTask]
     */
    override fun submissions(context: RunActionContext): List<Submission> = this.currentTask(context)?.submissions ?: emptyList()

    /**
     * Adjusting task durations is not supported by the [InteractiveAsynchronousRunManager]s.
     *
     * @return Time left (see [timeLeft]).
     */
    override fun adjustDuration(context: RunActionContext, s: Int): Long = this.timeLeft(context)

    /**
     * Overriding the ready state is not supported by the [InteractiveAsynchronousRunManager]s.
     *
     * @return false
     */
    override fun overrideReadyState(context: RunActionContext, viewerId: String): Boolean {
        return false
    }

    /**
     * Invoked by an external caller to post a new [Submission] for the [Task] that is currently being
     * executed by this [InteractiveAsynchronousRunManager]. [Submission]s usually cause updates to the
     * internal state and/or the [Scoreboard] of this [InteractiveRunManager].
     *
     * This method will not throw an exception and instead returns false if a [Submission] was
     * ignored for whatever reason (usually a state mismatch). It is up to the caller to re-invoke
     * this method again.
     *
     * @param context The [RunActionContext] used for the invocation
     * @param sub The [Submission] to be posted.
     *
     * @return [SubmissionStatus] of the [Submission]
     * @throws IllegalStateException If [InteractiveRunManager] was not in status [RunManagerStatus.RUNNING_TASK].
     */
    override fun postSubmission(context: RunActionContext, sub: Submission): SubmissionStatus = this.stateLock.read {
        require(context.teamId != null) { "TeamId is missing from action context, which is required for interaction with run manager." }
        checkTeamStatus(context.teamId, RunManagerStatus.RUNNING_TASK)

        /* Register submission. */
        val task = this.currentTask(context) ?: throw IllegalStateException("Could not find ongoing task in run manager, despite being in status ${this.statusMap[context.teamId]}. This is a programmer's error!")
        task.addSubmission(sub)

        /* Mark dao for update. */
        this.daoUpdatable.dirty = true

        /* Enqueue submission for post-processing. */
        this.scoresUpdatable.enqueue(Pair(task, sub))

        /* Enqueue WS message for sending */
        this.messageQueueUpdatable.enqueue(ServerMessage(this.id.string, ServerMessageType.TASK_UPDATED))

        return sub.status
    }

    /**
     * Invoked by an external caller to update an existing [Submission] by its [Submission.uid] with a new [SubmissionStatus].
     * [Submission]s usually cause updates to the internal state and/or the [Scoreboard] of this [InteractiveAsynchronousRunManager].
     *
     * This method will not throw an exception and instead returns false if a [Submission] was
     * ignored for whatever reason (usually a state mismatch). It is up to the caller to re-invoke
     * this method again.
     *
     * @param context The [RunActionContext] used for the invocation
     * @param submissionId The [UID] of the [Submission] to update.
     * @param submissionStatus The new [SubmissionStatus]
     *
     * @return Whether the update was successful or not
     * @throws IllegalStateException If [InteractiveRunManager] was not in status [RunManagerStatus.RUNNING_TASK].
     */
    override fun updateSubmission(context: RunActionContext, submissionId: UID, submissionStatus: SubmissionStatus): Boolean = this.stateLock.read {
        /* Sanity check. TODO: Do we indeed only want to be able to update submissions for the current task? */
        val found = this.submissions(context).find { it.uid == submissionId}  ?: return false

        /* Actual update - currently, only status update is allowed */
        if (found.status != submissionStatus) {
            found.status = submissionStatus

            /* Mark DAO for update. */
            this.daoUpdatable.dirty = true

            /* Enqueue submission for post-processing. */
            this.scoresUpdatable.enqueue(Pair(found.task!!, found))

            /* Enqueue WS message for sending */
            this.messageQueueUpdatable.enqueue(ServerMessage(this.id.string, ServerMessageType.TASK_UPDATED))

            return true
        }

        return false
    }

    /**
     * Returns a list of viewer [WebSocketConnection]s for this [InteractiveAsynchronousRunManager] alongside with their respective ready
     * state, which is always true for [InteractiveAsynchronousRunManager]
     *
     * @return List of viewer [WebSocketConnection]s for this [RunManager].
     */
    override fun viewers(): Map<WebSocketConnection, Boolean> = Collections.unmodifiableMap(this.viewers)

    /**
     * Processes WebSocket [ClientMessage] received by the [InteractiveAsynchronousRunManager].
     *
     * @param connection The [WebSocketConnection] through which the message was received.
     * @param message The [ClientMessage] received.
     */
    override fun wsMessageReceived(connection: WebSocketConnection, message: ClientMessage): Boolean {
        when (message.type) {
            ClientMessageType.REGISTER -> this.viewers[connection] = true
            ClientMessageType.UNREGISTER -> this.viewers.remove(connection)
            else -> { /* No op. */}
        }
        return true
    }

    override fun run() {
        /** Sort list of by [Phase] in ascending order. */
        this.updatables.sortBy { it.phase }

        /** Start [InteractiveSynchronousRunManager] . */
        var errorCounter = 0
        while (this.status != RunManagerStatus.TERMINATED) {
            try {
                /* 1) Invoke all relevant [Updatable]s. */
                this.invokeUpdatables()

                /* 2) Process internal state updates (if necessary). */
                this.internalStateUpdate()

                /* 3) Reset error counter and yield to other threads. */
                errorCounter = 0
                Thread.sleep(10)
            } catch (ie: InterruptedException) {
                LOGGER.info("Interrupted run manager thread; exiting...")
                return
            } catch (e: Throwable) {
                LOGGER.error("Uncaught exception in run loop for competition run ${this.id}. Loop will continue to work but this error should be handled!", e)

                // oh shit, something went horribly horribly wrong
                if (errorCounter >= MAXIMUM_ERROR_COUNT) {
                    LOGGER.error("Reached maximum consecutive error count of  $MAXIMUM_ERROR_COUNT; terminating loop...")
                    RunExecutor.dump(this.run)
                    break
                } else {
                    LOGGER.error("This is the ${++errorCounter}-th in a row. Run manager will terminate loop after $MAXIMUM_ERROR_COUNT errors")
                }
            }
        }

        /** Invoke [Updatable]s one last time. */
        this.invokeUpdatables()
        LOGGER.info("Run manager ${this.id} has reached end of run logic.")
    }

    /**
     * Invokes all [Updatable]s registered with this [InteractiveSynchronousRunManager].
     */
    private fun invokeUpdatables() = this.stateLock.read {
        this.updatables.forEach {
            if (it.shouldBeUpdated(this.status)) { /* TODO: This mechanism needs checking, since the status may be different for different teams. */
                try {
                    it.update(this.status)
                } catch (e: Throwable) {
                    LOGGER.error("Uncaught exception while updating ${it.javaClass.simpleName} for competition run ${this.id}. Loop will continue to work but this error should be handled!", e)
                }
            }
        }
    }

    /**
     * This is an internal method that facilitates internal state updates to this [InteractiveSynchronousRunManager],
     * i.e., status updates that are not triggered by an outside interaction.
     */
    private fun internalStateUpdate() = this.stateLock.read {
        for (teamId in this.run.description.teams.map { it.uid }) {
            if (this.statusMap[teamId] == RunManagerStatus.RUNNING_TASK) {
                val task = this.run.currentTaskForTeam(teamId) ?: throw IllegalStateException("Could not find active task for team ${teamId} despite status of the team being ${this.statusMap[teamId]}. This is a programmer's error!")
                val timeLeft = max(0L, task.duration * 1000L - (System.currentTimeMillis() - task.started!!))
                if (timeLeft <= 0) {
                    this.stateLock.write {
                        task.end()
                        this.statusMap[teamId] = RunManagerStatus.TASK_ENDED
                        AuditLogger.taskEnd(this.id, task.description.name, LogEventSource.INTERNAL, null)
                        EventStreamProcessor.event(TaskEndEvent(this.id, task.uid))
                    }

                    /* Mark DAO for update. */
                    this.daoUpdatable.dirty = true

                    /* Enqueue WS message for sending */
                    this.messageQueueUpdatable.enqueue(ServerMessage(this.id.string, ServerMessageType.TASK_END))
                }
            }
        }
    }

    /**
     * Checks if the [InteractiveAsynchronousRunManager] is in one of the given [RunManagerStatus] and throws an exception, if not.
     *
     * @param status List of expected [RunManagerStatus].
     */
    private fun checkGlobalStatus(vararg status: RunManagerStatus) {
        if (this.status !in status) throw IllegalRunStateException(this.status)
    }

    /**
     * Checks if the team status for the given [TeamId] and this [InteractiveAsynchronousRunManager]
     * is in one of the given [RunManagerStatus] and throws an exception, if not.
     *
     * @param teamId The [TeamId] to check.
     * @param status List of expected [RunManagerStatus].
     */
    private fun checkTeamStatus(teamId: TeamId, vararg status: RunManagerStatus) {
        val s = this.statusMap[teamId] ?: throw IllegalTeamIdException(teamId)
        if (s !in status) throw IllegalRunStateException(this.status)
    }
}