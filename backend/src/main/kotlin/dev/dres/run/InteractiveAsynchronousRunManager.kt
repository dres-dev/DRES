package dev.dres.run

import dev.dres.api.rest.types.WebSocketConnection
import dev.dres.api.rest.types.evaluation.ApiSubmission
import dev.dres.api.rest.types.evaluation.websocket.ClientMessage
import dev.dres.api.rest.types.evaluation.websocket.ClientMessageType
import dev.dres.api.rest.types.evaluation.websocket.ServerMessage
import dev.dres.api.rest.types.evaluation.websocket.ServerMessageType
import dev.dres.data.model.admin.DbRole
import dev.dres.data.model.audit.DbAuditLogSource
import dev.dres.data.model.template.DbEvaluationTemplate
import dev.dres.data.model.template.task.DbTaskTemplate
import dev.dres.data.model.run.*
import dev.dres.data.model.run.interfaces.TaskRun
import dev.dres.data.model.submissions.*
import dev.dres.data.model.template.team.TeamId
import dev.dres.run.audit.DbAuditLogger
import dev.dres.run.exceptions.IllegalRunStateException
import dev.dres.run.exceptions.IllegalTeamIdException
import dev.dres.run.score.ScoreTimePoint
import dev.dres.run.score.scoreboard.Scoreboard
import dev.dres.run.updatables.*
import dev.dres.run.validation.interfaces.JudgementValidator
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.*
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
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
class InteractiveAsynchronousRunManager(override val evaluation: InteractiveAsynchronousEvaluation, override val store: TransientEntityStore): InteractiveRunManager {

    companion object {
        private val LOGGER = LoggerFactory.getLogger(InteractiveAsynchronousRunManager::class.java)
        private const val SCOREBOARD_UPDATE_INTERVAL_MS = 1000L // TODO make configurable
        private const val MAXIMUM_ERROR_COUNT = 5
    }

    /** Generates and returns [RunProperties] for this [InteractiveAsynchronousRunManager]. */
    override val runProperties: RunProperties
        get() = RunProperties(this.evaluation.participantCanView, false, this.evaluation.allowRepeatedTasks, this.evaluation.limitSubmissionPreviews)

    /** Tracks the current [DbTaskTemplate] per [TeamId]. */
    private val statusMap: MutableMap<TeamId, RunManagerStatus> = HashMap()

    /** A [Map] of all viewers, i.e., DRES clients currently registered with this [InteractiveAsynchronousRunManager]. */
    private val viewers = ConcurrentHashMap<WebSocketConnection, Boolean>()

    /** A lock for state changes to this [InteractiveAsynchronousRunManager]. */
    private val stateLock = ReentrantReadWriteLock()

    /** The internal [ScoreboardsUpdatable] instance for this [InteractiveSynchronousRunManager]. */
    private val scoreboardsUpdatable = ScoreboardsUpdatable(this, SCOREBOARD_UPDATE_INTERVAL_MS)

    /** The internal [ScoresUpdatable] instance for this [InteractiveSynchronousRunManager]. */
    private val scoresUpdatable = ScoresUpdatable(this)

    /** List of [Updatable] held by this [InteractiveAsynchronousRunManager]. */
    private val updatables = mutableListOf<Updatable>()

    /** Run ID of this [InteractiveAsynchronousEvaluation]. */
    override val id: SubmissionId
        get() = this.evaluation.id

    /** Name of this [InteractiveAsynchronousEvaluation]. */
    override val name: String
        get() = this.evaluation.name

    /** The [DbEvaluationTemplate] executed by this [InteractiveSynchronousRunManager]. */
    override val template: DbEvaluationTemplate
        get() = this.evaluation.description

    /** The global [RunManagerStatus] of this [InteractiveAsynchronousRunManager]. */
    @Volatile
    override var status: RunManagerStatus = if (this.evaluation.hasStarted) {
        RunManagerStatus.ACTIVE
    } else {
        RunManagerStatus.CREATED
    }
        private set

    override val judgementValidators: List<JudgementValidator>
        get() = this.evaluation.tasks.mapNotNull { if (it.hasStarted && it.validator is JudgementValidator) it.validator else null }

    /** The list of [Scoreboard]s maintained by this [InteractiveAsynchronousEvaluation]. */
    override val scoreboards: List<Scoreboard>
        get() = this.evaluation.scoreboards

    /** The score history for this [InteractiveAsynchronousEvaluation]. */
    override val scoreHistory: List<ScoreTimePoint>
        get() = this.scoreboardsUpdatable.timeSeries

    init {
        /* Register relevant Updatables. */
        this.updatables.add(this.scoresUpdatable)
        this.updatables.add(this.scoreboardsUpdatable)

        this.store.transactional(true) {
            this.template.teams.asSequence().forEach {
                val teamContext = RunActionContext("<EMPTY>", it.teamId, setOf(DbRole.ADMIN))
                this.updatables.add(EndTaskUpdatable(this, teamContext))

                /* Initialize map and set all tasks pointers to the first task. */
                this.statusMap[it.teamId] = if (this.evaluation.hasStarted) {
                    RunManagerStatus.ACTIVE
                } else {
                    RunManagerStatus.CREATED
                }

                /** End ongoing runs upon initialization (in case server crashed during task execution). */
                if (this.evaluation.tasksForTeam(it.teamId).lastOrNull()?.isRunning == true) {
                    this.evaluation.tasksForTeam(it.teamId).last().end()
                }
            }

            /** Trigger score updates and re-enqueue pending submissions for judgement (if any). */
            this.evaluation.tasks.forEach { task ->
                task.getSubmissions().forEach { sub ->
                    this.scoresUpdatable.enqueue(task)
                    sub.answerSets().filter { v -> v.status() == VerdictStatus.INDETERMINATE }.asSequence().forEach {
                        task.validator.validate(it)
                    }
                }
            }
        }
    }

    /**
     * Starts this [InteractiveAsynchronousEvaluation] moving [RunManager.status] from [RunManagerStatus.CREATED] to
     * [RunManagerStatus.ACTIVE] for the selected team.
     *
     * As all state affecting methods, this method throws an [IllegalStateException] if invocation
     * does not match the current state.
     *
     * @param context The [RunActionContext] for this invocation.
     * @throws IllegalStateException If [RunManager] was not in status [RunManagerStatus.CREATED]
     */
    override fun start(context: RunActionContext) = this.stateLock.write {
        checkGlobalStatus(RunManagerStatus.CREATED)
        if (context.isAdmin) {
            /* Start the run. */
            this.evaluation.start()

            /* Update status. */
            this.statusMap.forEach { (t, _) -> this.statusMap[t] = RunManagerStatus.ACTIVE }
            this.status = RunManagerStatus.ACTIVE

            /* Enqueue WS message for sending */
            RunExecutor.broadcastWsMessage(ServerMessage(this.id, ServerMessageType.COMPETITION_START))

            LOGGER.info("Run manager ${this.id} started")
        }
    }

    /**
     * Ends this [InteractiveAsynchronousEvaluation] moving [RunManager.status] from [RunManagerStatus.ACTIVE] to
     * [RunManagerStatus.TERMINATED] for selected team.
     *
     * As all state affecting methods, this method throws an [IllegalStateException] if invocation
     * does not match the current state.
     *
     * @param context The [RunActionContext] for this invocation.
     * @throws IllegalStateException If [RunManager] was not in status [RunManagerStatus.ACTIVE]
     */
    override fun end(context: RunActionContext) {
        checkGlobalStatus(RunManagerStatus.CREATED, RunManagerStatus.ACTIVE)//, RunManagerStatus.TASK_ENDED)
        if (context.isAdmin) {
            /* End the run. */
            this.evaluation.end()

            /* Update status. */
            this.statusMap.forEach { (t, _) -> this.statusMap[t] = RunManagerStatus.TERMINATED }
            this.status = RunManagerStatus.TERMINATED

            /* Enqueue WS message for sending */
            RunExecutor.broadcastWsMessage(ServerMessage(this.id, ServerMessageType.COMPETITION_END))

            LOGGER.info("SynchronousRunManager ${this.id} terminated")
        }
    }

    /**
     *
     */
    override fun updateProperties(properties: RunProperties) {
        TODO("Not yet implemented")
    }

    /**
     * Returns the currently active [DbTaskTemplate] for the given team. Requires [RunManager.status] for the
     * requesting team to be [RunManagerStatus.ACTIVE].
     *
     * @param context The [RunActionContext] used for the invocation.
     * @return The [DbTaskTemplate] for the given team.
     */
    override fun currentTaskTemplate(context: RunActionContext): DbTaskTemplate {
        require(context.teamId != null) { "TeamId missing from RunActionContext, which is required for interaction with InteractiveAsynchronousRunManager." }
        return this.evaluation.currentTaskDescription(context.teamId)
    }

    /**
     * Prepares this [InteractiveAsynchronousEvaluation] for the execution of previous [DbTaskTemplate]
     * as per order defined in [DbEvaluationTemplate.tasks]. Requires [RunManager.status] for the requesting team
     * to be [RunManagerStatus.ACTIVE].
     *
     * As all state affecting methods, this method throws an [IllegalStateException] if invocation
     * does not match the current state.
     *
     * @param context The [RunActionContext] used for the invocation.
     * @return True if [DbTaskTemplate] was moved, false otherwise. Usually happens if last [DbTaskTemplate] has been reached.
     * @throws IllegalStateException If [RunManager] was not in status [RunManagerStatus.ACTIVE]
     */
    override fun previous(context: RunActionContext): Boolean = this.stateLock.write {
        val newIndex = this.template.tasks.indexOf(this.currentTaskTemplate(context)) - 1
        return try {
            this.goTo(context, newIndex)
            true
        } catch (e: IndexOutOfBoundsException) {
            false
        }
    }

    /**
     * Prepares this [InteractiveAsynchronousEvaluation] for the execution of next [DbTaskTemplate]
     * as per order defined in [DbEvaluationTemplate.tasks]. Requires [RunManager.status] for the requesting
     * team to be [RunManagerStatus.ACTIVE].
     *
     * As all state affecting methods, this method throws an [IllegalStateException] if invocation does not match the current state.
     *
     * @param context The [RunActionContext] used for the invocation.
     * @return True if [DbTaskTemplate] was moved, false otherwise. Usually happens if last [DbTaskTemplate] has been reached.
     * @throws IllegalStateException If [RunManager] was not in status [RunManagerStatus.ACTIVE]
     */
    override fun next(context: RunActionContext): Boolean = this.stateLock.write {
        val newIndex = this.template.tasks.indexOf(this.currentTaskTemplate(context)) + 1
        return try {
            this.goTo(context, newIndex)
            true
        } catch (e: IndexOutOfBoundsException) {
            false
        }
    }

    /**
     * Prepares this [InteractiveAsynchronousEvaluation] for the execution of [DbTaskTemplate] with the given [index]
     * as per order defined in [DbEvaluationTemplate.tasks]. Requires [RunManager.status] for the requesting
     * team to be [RunManagerStatus.ACTIVE].
     *
     * As all state affecting methods, this method throws an [IllegalStateException] if invocation does not match the current state.
     *
     * @param context The [RunActionContext] used for the invocation.
     * @return True if [DbTaskTemplate] was moved, false otherwise. Usually happens if last [DbTaskTemplate] has been reached.
     * @throws IllegalStateException If [RunManager] was not in status [RunManagerStatus.ACTIVE]
     */
    override fun goTo(context: RunActionContext, index: Int) = this.stateLock.write {
        require(context.teamId != null) { "TeamId is missing from action context, which is required for interaction with run manager." }
        checkTeamStatus(context.teamId, RunManagerStatus.ACTIVE)//, RunManagerStatus.TASK_ENDED)
        require(!teamHasRunningTask(context.teamId)) { "Cannot change task while task is active" }

        val idx = (index + this.template.tasks.size()) % this.template.tasks.size()


        /* Update active task. */
        //this.run.navigationMap[context.teamId] = this.description.tasks[index]
        this.evaluation.goTo(context.teamId, idx)
        //FIXME since task run and competition run states are separated, this is not actually a state change
        this.statusMap[context.teamId] = RunManagerStatus.ACTIVE

        /* Mark scoreboards for update. */
        this.scoreboardsUpdatable.dirty = true

        /* Enqueue WS message for sending */
        RunExecutor.broadcastWsMessage(context.teamId, ServerMessage(this.id, ServerMessageType.COMPETITION_UPDATE, this.evaluation.currentTaskForTeam(context.teamId)?.taskId))

        LOGGER.info("SynchronousRunManager ${this.id} set to task $idx")


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
        require(context.teamId != null) { "TeamId is missing from action context, which is required for interaction with run manager." }
        checkTeamStatus(context.teamId, RunManagerStatus.ACTIVE)

        /* Create task and update status. */
        val currentTaskTemplate = this.evaluation.currentTaskDescription(context.teamId)

        /* Check for duplicate task runs */
        if (!this.evaluation.allowRepeatedTasks && this.evaluation.tasksForTeam(context.teamId).any { it.template.id == currentTaskTemplate.id }) {
            throw IllegalStateException("Task '${currentTaskTemplate.name}' has already been used")
        }

        val currentTaskRun = this.evaluation.IATaskRun(currentTaskTemplate, context.teamId)
        currentTaskRun.prepare()

        /* Mark scoreboards and DAO for update. */
        this.scoreboardsUpdatable.dirty = true

        /* Enqueue WS message for sending */
        RunExecutor.broadcastWsMessage(context.teamId, ServerMessage(this.id, ServerMessageType.TASK_PREPARE, currentTaskRun.taskId))

        LOGGER.info("Run manager  ${this.id} started task $currentTaskTemplate.")
    }

    /**
     * Force-abort the [currentTask] and thus moves the [InteractiveAsynchronousRunManager.status] for the given team from
     * [RunManagerStatus.ACTIVE] or [RunManagerStatus.ACTIVE] to [RunManagerStatus.ACTIVE]
     *
     * TODO: Do we want users to be able to do this? If yes, I'd argue that the ability to repeat a task is a requirement.
     *
     * As all state affecting methods, this method throws an [IllegalStateException] if invocation does not match the current state.
     *
     * @param context The [RunActionContext] used for the invocation.
     * @throws IllegalStateException If [InteractiveRunManager] was not in status [RunManagerStatus.ACTIVE].
     */
    override fun abortTask(context: RunActionContext) = this.stateLock.write {
        require(context.teamId != null) { "TeamId is missing from action context, which is required for interaction with run manager." }
        require(teamHasRunningTask(context.teamId)) { "No running task for Team ${context.teamId}" }

        /* End TaskRun and update status. */
        val currentTask = this.currentTask(context) ?: throw IllegalStateException("Could not find active task for team ${context.teamId} despite status of the team being ${this.statusMap[context.teamId]}. This is a programmer's error!")
        currentTask.end()

        /* Mark scoreboards and DAO for update. */
        this.scoreboardsUpdatable.dirty = true

        /* Enqueue WS message for sending */
        RunExecutor.broadcastWsMessage(context.teamId, ServerMessage(this.id, ServerMessageType.TASK_END, currentTask.taskId))

        LOGGER.info("Run manager ${this.id} aborted task $currentTask.")
    }

    /**
     * Returns the time  in milliseconds that is left until the end of the currently running task for the given team.
     * Only works if the [InteractiveAsynchronousRunManager] is in state [RunManagerStatus.ACTIVE]. If no task is running,
     * this method returns -1L.
     *
     * @param context The [RunActionContext] used for the invocation.
     * @return Time remaining until the task will end or -1, if no task is running.
     */
    override fun timeLeft(context: RunActionContext): Long = this.stateLock.read {
        require(context.teamId != null) { "TeamId is missing from action context, which is required for interaction with run manager." }
        val currentTaskRun = this.currentTask(context)

        return if (currentTaskRun?.isRunning == true) {
            max(
                0L,
                currentTaskRun.duration * 1000L - (System.currentTimeMillis() - currentTaskRun.started!!) + InteractiveRunManager.COUNTDOWN_DURATION
            )
        } else {
            -1L
        }
    }

    /**
     * Returns the time in milliseconds that has elapsed since the start of the current [DbTask].
     * Only works if the [RunManager] is in state [RunManagerStatus.ACTIVE]. If no task is running, this method returns -1L.
     *
     * @return Time remaining until the task will end or -1, if no task is running.
     */
    override fun timeElapsed(context: RunActionContext): Long = this.stateLock.read {
        val currentTaskRun = this.currentTask(context)
        return if (currentTaskRun?.isRunning == true) {
            System.currentTimeMillis() - (currentTaskRun.started!! + InteractiveRunManager.COUNTDOWN_DURATION)
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
        if (context.isAdmin) return this.evaluation.tasks.size
        require(context.teamId != null) { "TeamId is missing from action context, which is required for interaction with run manager." }
        return this.evaluation.tasksForTeam(context.teamId).size
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
        if (context.isAdmin) return this.evaluation.tasks
        require(context.teamId != null) { "TeamId is missing from action context, which is required for interaction with run manager." }
        return this.evaluation.tasksForTeam(context.teamId)
    }

    /**
     * Returns [AbstractInteractiveTask]s for a specific task [SubmissionId]. May be empty.
     *
     * @param context The [RunActionContext] used for the invocation.
     * @param taskId The [SubmissionId] of the [AbstractInteractiveTask].
     */
    override fun taskForId(context: RunActionContext, taskId: SubmissionId): AbstractInteractiveTask? =
        this.tasks(context).find { it.taskId == taskId }

    /**
     * Returns a reference to the currently active [InteractiveAsynchronousEvaluation.IATaskRun].
     *
     * @param context The [RunActionContext] used for the invocation.
     * @return [InteractiveAsynchronousEvaluation.IATaskRun] that is currently active or null, if no such task is active.
     */
    override fun currentTask(context: RunActionContext): InteractiveAsynchronousEvaluation.IATaskRun? = this.stateLock.read {
        require(context.teamId != null) { "TeamId is missing from action context, which is required for interaction with run manager." }
        return this.evaluation.currentTaskForTeam(context.teamId)
    }

    /**
     * List of all [DbSubmission]s for this [InteractiveAsynchronousRunManager], irrespective of the [DbTask] it belongs to.
     *
     * @param context The [RunActionContext] used for the invocation.
     * @return List of [DbSubmission]s.
     */
    override fun allSubmissions(context: RunActionContext): List<DbSubmission> = this.stateLock.read {
        this.evaluation.tasks.flatMap { it.getSubmissions() }
    }

    /**
     * Returns the [DbSubmission]s for all currently active [AbstractInteractiveTask]s or an empty [List], if no such task is active.
     *
     * @param context The [RunActionContext] used for the invocation.
     * @return List of [DbSubmission]s.
     */
    override fun currentSubmissions(context: RunActionContext): List<DbSubmission> = this.stateLock.read {
        this.currentTask(context)?.getSubmissions()?.toList() ?: emptyList()
    }

    /**
     * Adjusting task durations is not supported by the [InteractiveAsynchronousRunManager]s.
     *
     */
    override fun adjustDuration(context: RunActionContext, s: Int): Long {
        require(context.teamId != null) { "TeamId is missing from action context, which is required for interaction with run manager." }
        require(teamHasRunningTask(context.teamId)) { "No running task for Team ${context.teamId}" }

        val currentTaskRun = this.currentTask(context)
            ?: throw IllegalStateException("No active TaskRun found. This is a serious error!")
        val newDuration = currentTaskRun.duration + s
        check((newDuration * 1000L - (System.currentTimeMillis() - currentTaskRun.started!!)) > 0) { "New duration $s can not be applied because too much time has already elapsed." }
        currentTaskRun.duration = newDuration
        return (currentTaskRun.duration * 1000L - (System.currentTimeMillis() - currentTaskRun.started!!))

    }

    /**
     * Overriding the ready state is not supported by the [InteractiveAsynchronousRunManager]s.
     *
     * @return false
     */
    override fun overrideReadyState(context: RunActionContext, viewerId: String): Boolean {
        return false
    }

    /**
     * Invoked by an external caller to post a new [DbSubmission] for the [TaskRun] that is currently being
     * executed by this [InteractiveAsynchronousRunManager]. [DbSubmission]s usually cause updates to the
     * internal state and/or the [Scoreboard] of this [InteractiveRunManager].
     *
     * This method will not throw an exception and instead returns false if a [DbSubmission] was
     * ignored for whatever reason (usually a state mismatch). It is up to the caller to re-invoke
     * this method again.
     *
     * @param context The [RunActionContext] used for the invocation
     * @param submission The [DbSubmission] to be posted.
     *
     * @return [DbVerdictStatus] of the [DbSubmission]
     * @throws IllegalStateException If [InteractiveRunManager] was not in status [RunManagerStatus.ACTIVE].
     */
    override fun postSubmission(context: RunActionContext, submission: ApiSubmission) = this.stateLock.read {
        require(context.teamId != null) { "TeamId is missing from action context, which is required for interaction with run manager." }
        require(teamHasRunningTask(context.teamId)) { "No running task for Team ${context.teamId}" }
        require(submission.answerSets().count() == 1) { "Only single verdict per submission is allowed for InteractiveAsynchronousRunManager." } /* TODO: Do we want this restriction? */

        /* Register submission. */
        val task = this.currentTask(context) ?: throw IllegalStateException("Could not find ongoing task in run manager, despite being in status ${this.statusMap[context.teamId]}. This is a programmer's error!")

        /* Sanity check. */
        check(task.isRunning) { "Task run '${this.name}.${task.position}' is currently not running. This is a programmer's error!" }
        check(task.teamId == submission.teamId) { "Team ${submission.teamId} is not eligible to submit to this task. This is a programmer's error!" }

        /* Check if ApiSubmission meets formal requirements. */
        task.filter.acceptOrThrow(submission)

        /* Apply transformations to submissions */
        val transformedSubmission = task.transformer.transform(submission)

        /* Check if there are answers left after transformation */
        if (transformedSubmission.answers.isEmpty()) {
            throw IllegalStateException("Submission contains no valid answer sets")
        }

        /* At this point, the submission is considered valid and is persisted */
        /* Validator is applied to each answer set */
        transformedSubmission.answerSets().forEach {
            task.validator.validate(it)
        }

        /* Persist the submission. */
        transformedSubmission.toNewDb()

        /* Enqueue submission for post-processing. */
        this.scoresUpdatable.enqueue(task)

        /* Enqueue WS message for sending */
        RunExecutor.broadcastWsMessage(context.teamId, ServerMessage(this.id, ServerMessageType.TASK_UPDATED, task.taskId))

    }

    /**
     * Invoked by an external caller to update an existing [DbSubmission] by its [DbSubmission.submissionId] with a new [DbVerdictStatus].
     * [DbSubmission]s usually cause updates to the internal state and/or the [Scoreboard] of this [InteractiveAsynchronousRunManager].
     *
     * This method will not throw an exception and instead returns false if a [DbSubmission] was
     * ignored for whatever reason (usually a state mismatch). It is up to the caller to re-invoke
     * this method again.
     *
     * @param context The [RunActionContext] used for the invocation
     * @param submissionId The [SubmissionId] of the [DbSubmission] to update.
     * @param submissionStatus The new [DbVerdictStatus]
     *
     * @return Whether the update was successful or not
     */
    override fun updateSubmission(context: RunActionContext, submissionId: SubmissionId, submissionStatus: DbVerdictStatus): Boolean = this.stateLock.read {
        val answerSet = DbAnswerSet.filter { it.submission.submissionId eq submissionId }.singleOrNull() ?: return false
        val task = this.taskForId(context, answerSet.task.id) ?: return false

        /* Actual update - currently, only status update is allowed */
        if (answerSet.status != submissionStatus) {
            answerSet.status = submissionStatus

            /* Enqueue submission for post-processing. */
            this.scoresUpdatable.enqueue(task)

            /* Enqueue WS message for sending */
            RunExecutor.broadcastWsMessage(context.teamId!!, ServerMessage(this.id,  ServerMessageType.TASK_UPDATED, task.taskId))
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
            else -> { /* No op. */
            }
        }
        return true
    }

    /**
     *
     */
    override fun run() {
        /** Sort list of by [Phase] in ascending order. */
        this.updatables.sortBy { it.phase }

        /** Start [InteractiveSynchronousRunManager] . */
        var errorCounter = 0
        while (this.status != RunManagerStatus.TERMINATED) {
            try {
                this.stateLock.read {
                    /* 1) Invoke all relevant [Updatable]s. */
                    this.invokeUpdatables()

                    /* 2) Process internal state updates (if necessary). */
                    this.internalStateUpdate()

                    /* 3) Reset error counter and yield to other threads. */
                    errorCounter = 0
                }
                Thread.sleep(250)
            } catch (ie: InterruptedException) {
                LOGGER.info("Interrupted run manager thread; exiting...")
                return
            } catch (e: Throwable) {
                LOGGER.error(
                    "Uncaught exception in run loop for competition run ${this.id}. Loop will continue to work but this error should be handled!",
                    e
                )

                // oh shit, something went horribly, horribly wrong
                if (errorCounter >= MAXIMUM_ERROR_COUNT) {
                    LOGGER.error("Reached maximum consecutive error count of  $MAXIMUM_ERROR_COUNT; terminating loop...")
                    RunExecutor.dump(this.evaluation)
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
        this.statusMap.values.toSet().forEach { status -> //call update once for every possible status which is currently set for any team
            this.updatables.forEach {
                if (it.shouldBeUpdated(status)) {
                    try {
                        it.update(status)
                    } catch (e: Throwable) {
                        LOGGER.error("Uncaught exception while updating ${it.javaClass.simpleName} for competition run ${this.id}. Loop will continue to work but this error should be handled!", e)
                    }
                }
            }
        }
    }

    /**
     * This is an internal method that facilitates internal state updates to this [InteractiveSynchronousRunManager],
     * i.e., status updates that are not triggered by an outside interaction.
     */
    private fun internalStateUpdate() = this.stateLock.read {
        for (team in this.evaluation.description.teams.asSequence()) {
            if (teamHasRunningTask(team.teamId)) {
                this.stateLock.write {
                    this.store.transactional {
                        val task = this.evaluation.currentTaskForTeam(team.teamId) ?: throw IllegalStateException("Could not find active task for team ${team.teamId} despite status of the team being ${this.statusMap[team.teamId]}. This is a programmer's error!")
                        val timeLeft = max(0L, task.duration * 1000L - (System.currentTimeMillis() - task.started!!) + InteractiveRunManager.COUNTDOWN_DURATION)
                        if (timeLeft <= 0) {
                            task.end()
                            DbAuditLogger.taskEnd(this.id, task.taskId, DbAuditLogSource.INTERNAL, null)

                            /* Enqueue WS message for sending */
                            RunExecutor.broadcastWsMessage(team.teamId, ServerMessage(this.id, ServerMessageType.TASK_END, task.taskId))
                        }
                    }
                }
            } else if (teamHasPreparingTask(team.teamId)) {
                this.stateLock.write {
                    this.store.transactional {
                        val task = this.evaluation.currentTaskForTeam(team.teamId)
                            ?: throw IllegalStateException("Could not find active task for team ${team.teamId} despite status of the team being ${this.statusMap[team.teamId]}. This is a programmer's error!")
                        task.start()
                        DbAuditLogger.taskStart(this.id, task.teamId, task.template, DbAuditLogSource.REST, null)
                        RunExecutor.broadcastWsMessage(team.teamId, ServerMessage(this.id, ServerMessageType.TASK_START, task.taskId))
                    }
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
        if (s !in status) throw IllegalRunStateException(s)
    }

    /**
     * Checks if the team for the given [TeamId] has an active and running task.
     *
     * @param teamId The [TeamId] to check.
     * @return True if task is running for team, false otherwise.
     */
    private fun teamHasRunningTask(teamId: TeamId) = this.evaluation.currentTaskForTeam(teamId)?.isRunning == true

    /**
     *
     */
    private fun teamHasPreparingTask(teamId: TeamId) =
        this.evaluation.currentTaskForTeam(teamId)?.status == TaskStatus.PREPARING
}
