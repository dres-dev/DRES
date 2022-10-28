package dev.dres.run

import dev.dres.api.rest.types.WebSocketConnection
import dev.dres.api.rest.types.run.websocket.ClientMessage
import dev.dres.api.rest.types.run.websocket.ClientMessageType
import dev.dres.api.rest.types.run.websocket.ServerMessage
import dev.dres.api.rest.types.run.websocket.ServerMessageType
import dev.dres.data.model.UID
import dev.dres.data.model.audit.AuditLogSource
import dev.dres.data.model.competition.CompetitionDescription
import dev.dres.data.model.competition.task.TaskDescription
import dev.dres.data.model.competition.options.ConfiguredOption
import dev.dres.data.model.competition.options.Option
import dev.dres.data.model.competition.options.SimpleOption
import dev.dres.data.model.competition.options.SimpleOptionParameters
import dev.dres.data.model.run.InteractiveSynchronousCompetition
import dev.dres.data.model.run.RunActionContext
import dev.dres.data.model.run.RunProperties
import dev.dres.data.model.submissions.Submission
import dev.dres.data.model.submissions.SubmissionStatus
import dev.dres.run.audit.AuditLogger
import dev.dres.run.eventstream.EventStreamProcessor
import dev.dres.run.eventstream.TaskEndEvent
import dev.dres.run.exceptions.IllegalRunStateException
import dev.dres.run.score.ScoreTimePoint
import dev.dres.run.score.scoreboard.Scoreboard
import dev.dres.run.updatables.*
import dev.dres.run.validation.interfaces.JudgementValidator
import dev.dres.utilities.ReadyLatch
import dev.dres.utilities.extensions.UID
import org.slf4j.LoggerFactory
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.math.max

/**
 * An implementation of [RunManager] aimed at distributed execution having a single DRES Server instance and multiple
 * viewers connected via WebSocket. Before starting a [InteractiveSynchronousCompetition.Task], all viewer instances are synchronized.
 *
 * @version 2.1.0
 * @author Ralph Gasser
 */
class InteractiveSynchronousRunManager(
    val run: InteractiveSynchronousCompetition
) : InteractiveRunManager {

    private val VIEWER_TIME_OUT = 30L //TODO make configurable

    private val SCOREBOARD_UPDATE_INTERVAL_MS = 1000L // TODO make configurable

    private val LOGGER = LoggerFactory.getLogger(this.javaClass)

    /** Number of consecutive errors which have to occur within the main execution loop before it tries to gracefully terminate */
    private val maxErrorCount = 5

    /**
     * Alternative constructor from existing [InteractiveSynchronousCompetition].
     */
    constructor(description: CompetitionDescription, name: String, runProperties: RunProperties) : this(
        InteractiveSynchronousCompetition(UID.EMPTY, name, description, runProperties).apply {
            RunExecutor.runs.append(this)
        }
    )

    override val runProperties: RunProperties
    get() = run.properties

    /** Run ID of this [InteractiveSynchronousRunManager]. */
    override val id: UID
        get() = this.run.id

    /** Name of this [InteractiveSynchronousRunManager]. */
    override val name: String
        get() = this.run.name

    /** The [CompetitionDescription] executed by this [InteractiveSynchronousRunManager]. */
    override val description: CompetitionDescription
        get() = this.run.description

    /** The list of all [Submission]s tracked ever received by this [InteractiveSynchronousRunManager]. */
    override val allSubmissions: List<Submission>
        get() = this.stateLock.read {
            this.run.tasks.flatMap { it.submissions }
        }

    /** The status of this [RunManager]. */
    @Volatile
    override var status: RunManagerStatus = if (this.run.hasStarted) {
        RunManagerStatus.ACTIVE
    } else {
        RunManagerStatus.CREATED
    }
        get() = this.stateLock.read {
            return field
        }
        private set

    /** Returns list [JudgementValidator]s associated with this [InteractiveSynchronousRunManager]. May be empty*/
    override val judgementValidators: List<JudgementValidator>
        get() = this.run.tasks.mapNotNull { if (it.hasStarted && it.validator is JudgementValidator) it.validator else null }

    /** List of [Scoreboard]s for this [InteractiveSynchronousRunManager]. */
    override val scoreboards: List<Scoreboard>
        get() = this.scoreboardsUpdatable.scoreboards

    /** List of [ScoreTimePoint]s tracking the states of the different [Scoreboard]s over time. */
    override val scoreHistory: List<ScoreTimePoint>
        get() = this.scoreboardsUpdatable.timeSeries

    /** Internal data structure that tracks all [WebSocketConnection]s and their ready state (for [RunManagerStatus.PREPARING_TASK]) */
    private val readyLatch = ReadyLatch<WebSocketConnection>()

    /** The internal [ScoreboardsUpdatable] instance for this [InteractiveSynchronousRunManager]. */
    private val scoreboardsUpdatable =
        ScoreboardsUpdatable(this.description.generateDefaultScoreboards(), SCOREBOARD_UPDATE_INTERVAL_MS, this.run)

    /** The internal [MessageQueueUpdatable] instance used by this [InteractiveSynchronousRunManager]. */
    private val messageQueueUpdatable = MessageQueueUpdatable(RunExecutor)

    /** The internal [DAOUpdatable] instance used by this [InteractiveSynchronousRunManager]. */
    private val daoUpdatable = DAOUpdatable(RunExecutor.runs, this.run)

    /** The internal [ScoresUpdatable] instance for this [InteractiveSynchronousRunManager]. */
    private val scoresUpdatable =
        ScoresUpdatable(this.id, this.scoreboardsUpdatable, this.messageQueueUpdatable, this.daoUpdatable)

    /** The internal [DAOUpdatable] used to end a task once no more submissions are possible */
    private val endTaskUpdatable = EndTaskUpdatable(this, RunActionContext.INTERNAL)

    /** List of [Updatable] held by this [InteractiveSynchronousRunManager]. */
    private val updatables = mutableListOf<Updatable>()


    /** A lock for state changes to this [InteractiveSynchronousRunManager]. */
    private val stateLock = ReentrantReadWriteLock()

    private fun checkContext(context: RunActionContext) {
        if (!context.isAdmin)
            throw IllegalAccessError("functionality of SynchronousInteractiveRunManager only available to administrators")
    }

    init {
        /* Register relevant Updatables. */
        this.updatables.add(this.scoresUpdatable)
        this.updatables.add(this.scoreboardsUpdatable)
        this.updatables.add(this.messageQueueUpdatable)
        this.updatables.add(this.daoUpdatable)
        this.updatables.add(this.endTaskUpdatable)


        /** End ongoing runs upon initialization (in case server crashed during task execution). */
        if (this.run.currentTask?.isRunning == true) {
            this.run.currentTask?.end()
        }

        /** Re-enqueue pending submissions (if any). */
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

    override fun start(context: RunActionContext) = this.stateLock.write {
        checkStatus(RunManagerStatus.CREATED)
        checkContext(context)

        /* Start the run. */
        this.run.start()

        /* Update status. */
        this.status = RunManagerStatus.ACTIVE

        /* Mark DAO for update. */
        this.daoUpdatable.dirty = true

        /* Enqueue WS message for sending */
        this.messageQueueUpdatable.enqueue(ServerMessage(this.id.string, ServerMessageType.COMPETITION_START))

        LOGGER.info("SynchronousRunManager ${this.id} started")
    }

    override fun end(context: RunActionContext) = this.stateLock.write {
        checkStatus(RunManagerStatus.CREATED, RunManagerStatus.ACTIVE /*RunManagerStatus.TASK_ENDED*/)
        checkContext(context)

        /* End the run. */
        this.run.end()

        /* Update status. */
        this.status = RunManagerStatus.TERMINATED

        /* Mark DAO for update. */
        this.daoUpdatable.dirty = true

        /* Enqueue WS message for sending */
        this.messageQueueUpdatable.enqueue(ServerMessage(this.id.string, ServerMessageType.COMPETITION_END))

        LOGGER.info("SynchronousRunManager ${this.id} terminated")
    }

    override fun currentTaskDescription(context: RunActionContext): TaskDescription = this.stateLock.write {
        checkStatus(
            RunManagerStatus.CREATED,
            RunManagerStatus.ACTIVE/*, RunManagerStatus.PREPARING_TASK, RunManagerStatus.RUNNING_TASK, RunManagerStatus.TASK_ENDED*/
        )
        this.run.currentTaskDescription
    }

    override fun previous(context: RunActionContext): Boolean = this.stateLock.write {
        checkContext(context)
        val newIndex = this.description.tasks.indexOf(this.run.currentTaskDescription) - 1
        return try {
            this.goTo(context, newIndex)
            true
        } catch (e: IndexOutOfBoundsException) {
            false
        }
    }

    override fun next(context: RunActionContext): Boolean = this.stateLock.write {
        checkContext(context)
        val newIndex = this.description.tasks.indexOf(this.run.currentTaskDescription) + 1
        return try {
            this.goTo(context, newIndex)
            true
        } catch (e: IndexOutOfBoundsException) {
            false
        }
    }

    override fun goTo(context: RunActionContext, index: Int) {
        checkStatus(RunManagerStatus.ACTIVE)
        assureNoRunningTask()
        if (index >= 0 && index < this.description.tasks.size) {

            /* Update active task. */
            this.run.goTo(index)

            //FIXME since task run and competition run states are separated, this is not actually a state change
//            /* Update RunManager status. */
//            this.status = RunManagerStatus.ACTIVE

            /* Mark scoreboards for update. */
            this.scoreboardsUpdatable.dirty = true

            /* Enqueue WS message for sending */
            this.messageQueueUpdatable.enqueue(ServerMessage(this.id.string, ServerMessageType.COMPETITION_UPDATE))

            LOGGER.info("SynchronousRunManager ${this.id} set to task $index")
        } else {
            throw IndexOutOfBoundsException("Index $index is out of bounds for the number of available tasks.")
        }
    }

    override fun startTask(context: RunActionContext) = this.stateLock.write {
        checkStatus(RunManagerStatus.ACTIVE)
        assureNoRunningTask()
        checkContext(context)

        val currentTaskDescription = this.currentTaskDescription(context)

        /* Check for duplicate task runs */
        if (!runProperties.allowRepeatedTasks && this.run.tasks.any { it.description.id == currentTaskDescription.id }) {
            throw IllegalStateException("Task '${currentTaskDescription.name}' has already been used")
        }

        /* Create and prepare pipeline for submission. */
        this.run.Task(taskDescriptionId = currentTaskDescription.id)

        /* Update status. */
        //this.status = RunManagerStatus.PREPARING_TASK
        this.run.currentTask!!.prepare()

        /* Mark scoreboards and dao for update. */
        this.scoreboardsUpdatable.dirty = true
        this.daoUpdatable.dirty = true

        /* Reset the ReadyLatch. */
        this.readyLatch.reset(VIEWER_TIME_OUT)

        /* Enqueue WS message for sending */
        this.messageQueueUpdatable.enqueue(ServerMessage(this.id.string, ServerMessageType.TASK_PREPARE))

        LOGGER.info("SynchronousRunManager ${this.id} started task task ${this.run.currentTaskDescription}")
    }

    override fun abortTask(context: RunActionContext) = this.stateLock.write {
        //checkStatus(RunManagerStatus.PREPARING_TASK, RunManagerStatus.RUNNING_TASK)
        checkStatus(RunManagerStatus.ACTIVE)
        assureTaskPreparingOrRunning()
        checkContext(context)

        /* End TaskRun and persist. */
        this.currentTask(context)?.end()

//        /* Update state. */
//        this.status = RunManagerStatus.TASK_ENDED

        /* Mark scoreboards and dao for update. */
        this.scoreboardsUpdatable.dirty = true
        this.daoUpdatable.dirty = true

        /* Enqueue WS message for sending */
        this.messageQueueUpdatable.enqueue(ServerMessage(this.id.string, ServerMessageType.TASK_END))

        LOGGER.info("SynchronousRunManager ${this.id} aborted task task ${this.run.currentTaskDescription}")
    }

    /** List of [InteractiveSynchronousCompetition.Task] for this [InteractiveSynchronousRunManager]. */
    override fun tasks(context: RunActionContext): List<InteractiveSynchronousCompetition.Task> = this.run.tasks

    /**
     * Returns the currently active [InteractiveSynchronousCompetition.Task]s or null, if no such task is active.
     *
     * @param context The [RunActionContext] used for the invocation.
     * @return [InteractiveSynchronousCompetition.Task] or null
     */
    override fun currentTask(context: RunActionContext) = this.stateLock.read {
//        when (this.status) {
//            RunManagerStatus.PREPARING_TASK,
//            RunManagerStatus.RUNNING_TASK,
//            RunManagerStatus.TASK_ENDED -> this.run.lastTask
//            else -> null
//        }

        when (this.run.currentTask?.status) {
            TaskRunStatus.PREPARING,
            TaskRunStatus.RUNNING,
            TaskRunStatus.ENDED -> this.run.currentTask
            else -> null
        }

    }

    /**
     * Returns [InteractiveSynchronousCompetition.Task]s for a specific task [UID]. May be empty.
     *
     * @param taskId The [UID] of the [InteractiveSynchronousCompetition.Task].
     */
    override fun taskForId(context: RunActionContext, taskId: UID): InteractiveSynchronousCompetition.Task? =
        this.run.tasks.find { it.uid == taskId }

    /**
     * Returns the [Submission]s for all currently active [InteractiveSynchronousCompetition.Task]s or an empty [List], if no such task is active.
     *
     * @param context The [RunActionContext] used for the invocation.
     * @return List of [Submission]s for the currently active [InteractiveSynchronousCompetition.Task]
     */
    override fun submissions(context: RunActionContext): List<Submission> =
        this.currentTask(context)?.submissions?.toList() ?: emptyList()

    /**
     * Returns the number of [InteractiveSynchronousCompetition.Task]s held by this [RunManager].
     *
     * @return The number of [InteractiveSynchronousCompetition.Task]s held by this [RunManager]
     */
    override fun taskCount(context: RunActionContext): Int = this.run.tasks.size

    /**
     * Adjusts the duration of the current [InteractiveSynchronousCompetition.Task] by the specified amount. Amount can be either positive or negative.
     *
     * @param s The number of seconds to adjust the duration by.
     * @return Time remaining until the task will end in milliseconds
     *
     * @throws IllegalArgumentException If the specified correction cannot be applied.
     * @throws IllegalStateException If [RunManager] was not in status [RunManagerStatus.RUNNING_TASK].
     */
    override fun adjustDuration(context: RunActionContext, s: Int): Long = this.stateLock.read {
        assureTaskRunning()
        checkContext(context)

        val currentTaskRun = this.currentTask(context)
            ?: throw IllegalStateException("SynchronizedRunManager is in status ${this.status} but has no active TaskRun. This is a serious error!")
        val newDuration = currentTaskRun.duration + s
        check((newDuration * 1000L - (System.currentTimeMillis() - currentTaskRun.started!!)) > 0) { "New duration $s can not be applied because too much time has already elapsed." }
        currentTaskRun.duration = newDuration
        return (currentTaskRun.duration * 1000L - (System.currentTimeMillis() - currentTaskRun.started!!))
    }

    /**
     * Returns the time in milliseconds that is left until the end of the current [InteractiveSynchronousCompetition.Task].
     * Only works if the [RunManager] is in state [RunManagerStatus.RUNNING_TASK]. If no task is running,
     * this method returns -1L.
     *
     * @return Time remaining until the task will end or -1, if no task is running.
     */
    override fun timeLeft(context: RunActionContext): Long = this.stateLock.read {
        return if (this.run.currentTask?.status == TaskRunStatus.RUNNING) {
            val currentTaskRun = this.currentTask(context)
                ?: throw IllegalStateException("SynchronizedRunManager is in status ${this.status} but has no active TaskRun. This is a serious error!")
            max(
                0L,
                currentTaskRun.duration * 1000L - (System.currentTimeMillis() - currentTaskRun.started!!) + InteractiveRunManager.COUNTDOWN_DURATION
            )
        } else {
            -1L
        }
    }

    /**
     * Returns the time in milliseconds that has elapsed since the start of the current [InteractiveSynchronousCompetition.Task].
     * Only works if the [RunManager] is in state [RunManagerStatus.RUNNING_TASK]. If no task is running, this method returns -1L.
     *
     * @return Time remaining until the task will end or -1, if no task is running.
     */
    override fun timeElapsed(context: RunActionContext): Long = this.stateLock.read {
        return if (this.run.currentTask?.status == TaskRunStatus.RUNNING) {
            val currentTaskRun = this.currentTask(context)
                ?: throw IllegalStateException("SynchronizedRunManager is in status ${this.status} but has no active TaskRun. This is a serious error!")
            System.currentTimeMillis() - (currentTaskRun.started!! + InteractiveRunManager.COUNTDOWN_DURATION)
        } else {
            -1L
        }
    }

    /**
     * Lists  all WebsSocket session IDs for viewer instances currently registered to this [InteractiveSynchronousRunManager].
     *
     * @return Map of session ID to ready state.
     */
    override fun viewers(): HashMap<WebSocketConnection, Boolean> = this.readyLatch.state()

    /**
     * Can be used to manually override the READY state of a viewer. Can be used in case a viewer hangs in the PREPARING_TASK phase.
     *
     * @param viewerId The ID of the viewer's WebSocket session.
     */
    override fun overrideReadyState(context: RunActionContext, viewerId: String): Boolean = this.stateLock.read {
        //checkStatus(RunManagerStatus.PREPARING_TASK)
        checkStatus(RunManagerStatus.ACTIVE)
        assureTaskPreparingOrRunning()
        checkContext(context)

        return try {
            val viewer = this.readyLatch.state().keys.find { it.sessionId == viewerId }
            if (viewer != null) {
                this.readyLatch.setReady(viewer)
                true
            } else {
                false
            }
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    /**
     * Processes WebSocket [ClientMessage] received by the [RunExecutor].
     *
     * @param connection The [WebSocketConnection] through which the message was received.
     * @param message The [ClientMessage] received.
     */
    override fun wsMessageReceived(connection: WebSocketConnection, message: ClientMessage): Boolean =
        this.stateLock.read {
            when (message.type) {
                ClientMessageType.ACK -> {
                    if (this.run.currentTask?.status == TaskRunStatus.PREPARING) {
                        this.readyLatch.setReady(connection)
                    }
                }
                ClientMessageType.REGISTER -> this.readyLatch.register(connection)
                ClientMessageType.UNREGISTER -> this.readyLatch.unregister(connection)
                ClientMessageType.PING -> {
                } //handled in [RunExecutor]
            }
            return true
        }

    /**
     * Processes incoming [Submission]s. If a [InteractiveSynchronousCompetition.Task] is running then that [Submission] will usually
     * be associated with that [InteractiveSynchronousCompetition.Task].
     *
     * This method will not throw an exception and instead return false if a [Submission] was
     * ignored for whatever reason (usually a state mismatch). It is up to the caller to re-invoke
     * this method again.
     *
     * @param context The [RunActionContext] used for the invocation
     * @param sub [Submission] that should be registered.
     */
    override fun postSubmission(context: RunActionContext, sub: Submission): SubmissionStatus = this.stateLock.read {
        assureTaskRunning()

        /* Register submission. */
        val task = this.currentTask(context)
            ?: throw IllegalStateException("Could not find ongoing task in run manager, despite correct status. This is a programmer's error!")
        task.addSubmission(sub)

        /** Checks for the presence of the [SimpleOption.PROLONG_ON_SUBMISSION] and applies it. */
        val option = task.description.taskType.options.find { it.option == SimpleOption.PROLONG_ON_SUBMISSION }
        if (option != null) {
            this.prolongOnSubmit(context, option, sub)
        }

        /* Mark DAO for update. */
        this.daoUpdatable.dirty = true

        /* Enqueue submission for post-processing. */
        this.scoresUpdatable.enqueue(Pair(task, sub))

        /* Enqueue WS message for sending */
        this.messageQueueUpdatable.enqueue(ServerMessage(this.id.string, ServerMessageType.TASK_UPDATED))

        return sub.status
    }

    /**
     * Processes incoming [Submission]s. If a [InteractiveSynchronousCompetition.Task] is running then that [Submission] will usually
     * be associated with that [InteractiveSynchronousCompetition.Task].
     *
     * This method will not throw an exception and instead return false if a [Submission] was
     * ignored for whatever reason (usually a state mismatch). It is up to the caller to re-invoke
     * this method again.
     *
     * @param context The [RunActionContext] used for the invocation
     * @param submissionId The [UID] of the [Submission] to update.
     * @param submissionStatus The new [SubmissionStatus]
     * @return True on success, false otherwise.
     */
    override fun updateSubmission(
        context: RunActionContext,
        submissionId: UID,
        submissionStatus: SubmissionStatus
    ): Boolean = this.stateLock.read {
        /* Sanity check. */
        val found = this.allSubmissions.find { it.uid == submissionId } ?: return false

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
     * Internal method that orchestrates the internal progression of the [InteractiveSynchronousCompetition].
     */
    override fun run() {
        /** Sort list of by [Phase] in ascending order. */
        this.updatables.sortBy { it.phase }

        var errorCounter = 0

        /** Start [InteractiveSynchronousRunManager] . */
        while (this.status != RunManagerStatus.TERMINATED) {
            try {
                /* Obtain lock on current state. */
                this.stateLock.read {
                    /* 2) Invoke all relevant [Updatable]s. */
                    this.invokeUpdatables()

                    /* 3) Process internal state updates (if necessary). */
                    this.internalStateUpdate()
                }

                /* 3) Yield to other threads. */
                Thread.sleep(10)

                /* Reset error counter. */
                errorCounter = 0
            } catch (ie: InterruptedException) {
                LOGGER.info("Interrupted SynchronousRunManager, exiting")
                return
            } catch (e: Throwable) {
                LOGGER.error(
                    "Uncaught exception in run loop for competition run ${this.id}. Loop will continue to work but this error should be handled!",
                    e
                )
                LOGGER.error("This is the ${++errorCounter}. in a row, will terminate loop after $maxErrorCount errors")

                // oh shit, something went horribly, horribly wrong
                if (errorCounter >= maxErrorCount) {
                    LOGGER.error("Reached maximum consecutive error count, terminating loop")
                    RunExecutor.dump(this.run)
                    break //terminate loop
                }
            }
        }

        /** Invoke [Updatable]s one last time. */
        this.stateLock.read {
            this.invokeUpdatables()
        }

        LOGGER.info("SynchronousRunManager ${this.id} reached end of run logic.")
    }

    /**
     * Invokes all [Updatable]s registered with this [InteractiveSynchronousRunManager].
     */
    private fun invokeUpdatables() {
        this.updatables.forEach {
            if (it.shouldBeUpdated(this.status)) {
                try {
                    it.update(this.status)
                } catch (e: Throwable) {
                    LOGGER.error(
                        "Uncaught exception while updating ${it.javaClass.simpleName} for competition run ${this.id}. Loop will continue to work but this error should be handled!",
                        e
                    )
                }
            }
        }
    }

    /**
     * This is an internal method that facilitates internal state updates to this [InteractiveSynchronousRunManager],
     * i.e., status updates that are not triggered by an outside interaction.
     */
    private fun internalStateUpdate() {
        /** Case 1: Facilitates internal transition from RunManagerStatus.PREPARING_TASK to RunManagerStatus.RUNNING_TASK. */
        if (this.run.currentTask?.status == TaskRunStatus.PREPARING && this.readyLatch.allReadyOrTimedOut()) {
            this.stateLock.write {
                this.run.currentTask!!.start()
                //this.status = RunManagerStatus.RUNNING_TASK
                AuditLogger.taskStart(this.id, this.run.currentTask!!.uid, this.run.currentTaskDescription, AuditLogSource.INTERNAL, null)
            }

            /* Mark DAO for update. */
            this.daoUpdatable.dirty = true

            /* Enqueue WS message for sending */
            this.messageQueueUpdatable.enqueue(ServerMessage(this.id.string, ServerMessageType.TASK_START))
        }

        /** Case 2: Facilitates internal transition from RunManagerStatus.RUNNING_TASK to RunManagerStatus.TASK_ENDED due to timeout. */
        if (this.run.currentTask?.status == TaskRunStatus.RUNNING) {
            val task = this.run.currentTask!!
            val timeLeft = max(
                0L,
                task.duration * 1000L - (System.currentTimeMillis() - task.started!!) + InteractiveRunManager.COUNTDOWN_DURATION
            )
            if (timeLeft <= 0) {
                this.stateLock.write {
                    task.end()
                    //this.status = RunManagerStatus.TASK_ENDED
                    AuditLogger.taskEnd(this.id, this.run.currentTask!!.uid, AuditLogSource.INTERNAL, null)
                    EventStreamProcessor.event(TaskEndEvent(this.id, task.uid))
                }

                /* Mark DAO for update. */
                this.daoUpdatable.dirty = true

                /* Enqueue WS message for sending */
                this.messageQueueUpdatable.enqueue(ServerMessage(this.id.string, ServerMessageType.TASK_END))
            }
        }
    }

    /**
     * Applies the [SimpleOption.PROLONG_ON_SUBMISSION] [Option].
     *
     * @param context [RunActionContext] used for invocation.
     * @param option The [ConfiguredOption<SimpleOption>] option.
     * @param sub The [Submission] to apply the [Option] for.
     */
    private fun prolongOnSubmit(context: RunActionContext, option: ConfiguredOption<SimpleOption>, sub: Submission) {
        require(option.option == SimpleOption.PROLONG_ON_SUBMISSION) { "Cannot process ${option.option} in prolongOnSubmit()." }
        val limit = option.getAsInt(SimpleOptionParameters.PROLONG_ON_SUBMISSION_LIMIT_PARAM)
            ?: SimpleOptionParameters.PROLONG_ON_SUBMISSION_LIMIT_DEFAULT
        val prolongBy = option.getAsInt(SimpleOptionParameters.PROLONG_ON_SUBMISSION_BY_PARAM)
            ?: SimpleOptionParameters.PROLONG_ON_SUBMISSION_BY_DEFAULT
        val correctOnly = option.getAsBool(SimpleOptionParameters.PROLONG_ON_SUBMISSION_CORRECT_PARAM)
            ?: SimpleOptionParameters.PROLONG_ON_SUBMISSION_CORRECT_DEFAULT
        if (correctOnly && sub.status != SubmissionStatus.CORRECT) {
            return
        }
        val timeLeft = Math.floorDiv(this.timeLeft(context), 1000)
        if (timeLeft in 0 until limit) {
            this.adjustDuration(context, prolongBy)
        }
    }

    /**
     * Checks if the [InteractiveSynchronousRunManager] is in one of the given [RunManagerStatus] and throws an exception, if not.
     *
     * @param status List of expected [RunManagerStatus].
     */
    private fun checkStatus(vararg status: RunManagerStatus) {
        if (this.status !in status) throw IllegalRunStateException(this.status)
    }

    private fun assureTaskRunning() {
        if (this.run.currentTask?.status != TaskRunStatus.RUNNING) throw IllegalStateException("Task not running")
    }

    private fun assureTaskPreparingOrRunning() {
        val status = this.run.currentTask?.status
        if (status != TaskRunStatus.RUNNING && status != TaskRunStatus.PREPARING) throw IllegalStateException("Task not preparing or running")
    }

    private fun assureNoRunningTask() {
        if (this.run.tasks.any { it.status == TaskRunStatus.RUNNING }) throw IllegalStateException("Task running!")
    }
}