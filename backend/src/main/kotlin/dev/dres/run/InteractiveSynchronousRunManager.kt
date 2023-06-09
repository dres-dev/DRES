package dev.dres.run

import dev.dres.api.rest.AccessManager
import dev.dres.api.rest.types.WebSocketConnection
import dev.dres.api.rest.types.evaluation.ApiSubmission
import dev.dres.api.rest.types.evaluation.websocket.ClientMessage
import dev.dres.api.rest.types.evaluation.websocket.ClientMessageType
import dev.dres.api.rest.types.evaluation.websocket.ServerMessage
import dev.dres.api.rest.types.evaluation.websocket.ServerMessageType
import dev.dres.data.model.audit.DbAuditLogSource
import dev.dres.data.model.run.*
import dev.dres.data.model.run.interfaces.EvaluationId
import dev.dres.data.model.run.interfaces.TaskRun
import dev.dres.data.model.submissions.*
import dev.dres.data.model.template.DbEvaluationTemplate
import dev.dres.data.model.template.task.DbTaskTemplate
import dev.dres.data.model.template.task.options.DbSubmissionOption
import dev.dres.data.model.template.task.options.DbTaskOption
import dev.dres.data.model.template.task.options.Defaults.SCOREBOARD_UPDATE_INTERVAL_DEFAULT
import dev.dres.data.model.template.task.options.Defaults.VIEWER_TIMEOUT_DEFAULT
import dev.dres.run.RunManager.Companion.MAXIMUM_RUN_LOOP_ERROR_COUNT
import dev.dres.run.audit.DbAuditLogger
import dev.dres.run.eventstream.EventStreamProcessor
import dev.dres.run.eventstream.TaskEndEvent
import dev.dres.run.exceptions.IllegalRunStateException
import dev.dres.run.score.ScoreTimePoint
import dev.dres.run.score.scoreboard.Scoreboard
import dev.dres.run.updatables.*
import dev.dres.run.validation.interfaces.JudgementValidator
import dev.dres.utilities.ReadyLatch
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.math.max

/**
 * An implementation of [RunManager] aimed at distributed execution having a single DRES Server instance and multiple
 * viewers connected via WebSocket. Before starting a [DbTask], all viewer instances are synchronized.
 *
 * @version 3.0.0
 * @author Ralph Gasser
 */
class InteractiveSynchronousRunManager(override val evaluation: InteractiveSynchronousEvaluation, override val store: TransientEntityStore) : InteractiveRunManager {


    companion object {
        /** The [Logger] instance used by [InteractiveSynchronousRunManager]. */
        private val LOGGER = LoggerFactory.getLogger(InteractiveSynchronousRunManager::class.java)
    }

    /** Generates and returns [RunProperties] for this [InteractiveAsynchronousRunManager]. */
    override val runProperties: RunProperties
        get() = RunProperties(this.evaluation.participantCanView, false, this.evaluation.allowRepeatedTasks, this.evaluation.limitSubmissionPreviews)

    /** [EvaluationId] of this [InteractiveSynchronousRunManager]. */
    override val id: EvaluationId
        get() = this.evaluation.id

    /** Name of this [InteractiveSynchronousRunManager]. */
    override val name: String
        get() = this.evaluation.name

    /** The [DbEvaluationTemplate] executed by this [InteractiveSynchronousRunManager]. */
    override val template: DbEvaluationTemplate
        get() = this.evaluation.description

    /** The status of this [RunManager]. */
    @Volatile
    override var status: RunManagerStatus = if (this.evaluation.hasStarted) {
        RunManagerStatus.ACTIVE
    } else {
        RunManagerStatus.CREATED
    }
    private set

    /** Returns list [JudgementValidator]s associated with this [InteractiveSynchronousRunManager]. May be empty*/
    override val judgementValidators: List<JudgementValidator>
        get() = this.evaluation.tasks.mapNotNull { if (it.hasStarted && it.validator is JudgementValidator) it.validator else null }

    /** List of [Scoreboard]s for this [InteractiveSynchronousRunManager]. */
    override val scoreboards: List<Scoreboard>
        get() = this.evaluation.scoreboards

    /** List of [ScoreTimePoint]s tracking the states of the different [Scoreboard]s over time. */
    override val scoreHistory: List<ScoreTimePoint>
        get() = this.scoreboardsUpdatable.timeSeries

    /** Internal data structure that tracks all [WebSocketConnection]s and their ready state. */
    private val readyLatch = ReadyLatch<WebSocketConnection>()

    /** The internal [ScoreboardsUpdatable] instance for this [InteractiveSynchronousRunManager]. */
    private val scoreboardsUpdatable = ScoreboardsUpdatable(this, SCOREBOARD_UPDATE_INTERVAL_DEFAULT)

    /** The internal [ScoresUpdatable] instance for this [InteractiveSynchronousRunManager]. */
    private val scoresUpdatable = ScoresUpdatable(this)

    /** List of [Updatable] held by this [InteractiveSynchronousRunManager]. */
    private val updatables = mutableListOf<Updatable>()

    /** A lock for state changes to this [InteractiveSynchronousRunManager]. */
    private val stateLock = ReentrantReadWriteLock()

    private fun checkContext(context: RunActionContext) {
        if (!context.isAdmin)
            throw IllegalAccessError("functionality of SynchronousInteractiveRunManager only available to administrators")
    }

    init {
        /* Register relevant updatable (these are always required). */
        this.updatables.add(this.scoresUpdatable)
        this.updatables.add(this.scoreboardsUpdatable)

        /* Loads optional updatable. */
        this.registerOptionalUpdatables()

        /* End ongoing tasks upon initialization (in case server crashed during task execution). */
        for (task in this.evaluation.tasks) {
            if (task.isRunning || task.status == DbTaskStatus.RUNNING) {
                task.end()
            }
        }

        /** Trigger score updates and re-enqueue pending submissions for judgement (if any). */
        this.evaluation.tasks.forEach { task ->
            task.getSubmissions().forEach { sub ->
                this.scoresUpdatable.enqueue(task)
                sub.answerSets.filter { v -> v.status eq DbVerdictStatus.INDETERMINATE }.asSequence().forEach {
                    task.validator.validate(it)
                }
            }
        }
    }

    override fun start(context: RunActionContext) = this.stateLock.write {
        checkStatus(RunManagerStatus.CREATED)
        checkContext(context)

        /* Start the run. */
        this.evaluation.start()

        /* Enqueue WS message for sending */
        RunExecutor.broadcastWsMessage(ServerMessage(this.id, ServerMessageType.COMPETITION_START))

        /* Log and update status. */
        LOGGER.info("SynchronousRunManager ${this.id} started")
        this.status = RunManagerStatus.ACTIVE
    }

    override fun end(context: RunActionContext) = this.stateLock.write {
        checkStatus(RunManagerStatus.CREATED, RunManagerStatus.ACTIVE /*RunManagerStatus.TASK_ENDED*/)
        checkContext(context)

        /* End the run. */
        this.evaluation.end()

        /* Update status. */
        this.status = RunManagerStatus.TERMINATED

        /* Enqueue WS message for sending */
        RunExecutor.broadcastWsMessage(ServerMessage(this.id, ServerMessageType.COMPETITION_END))

        LOGGER.info("SynchronousRunManager ${this.id} terminated")
    }

    /**
     * Updates the [RunProperties] for the [InteractiveSynchronousEvaluation] backing this [InteractiveSynchronousRunManager].
     *
     * @param properties The set of new [RunProperties]
     */
    override fun updateProperties(properties: RunProperties) {
        this.evaluation.allowRepeatedTasks = properties.allowRepeatedTasks
        this.evaluation.limitSubmissionPreviews = properties.limitSubmissionPreviews
        this.evaluation.participantCanView = properties.participantCanView
    }

    /**
     * Returns the [DbTaskTemplate] this [InteractiveAsynchronousRunManager] is currently pointing to.
     *
     * Requires an active database transaction.
     *
     * @return [DbTaskTemplate]
     */
    override fun currentTaskTemplate(context: RunActionContext): DbTaskTemplate = this.stateLock.write {
        checkStatus(RunManagerStatus.CREATED, RunManagerStatus.ACTIVE)
        this.evaluation.getCurrentTemplate()
    }

    override fun previous(context: RunActionContext): Boolean = this.stateLock.write {
        checkContext(context)
        val newIndex = this.evaluation.templateIndex - 1
        return try {
            this.goTo(context, newIndex)
            true
        } catch (e: IndexOutOfBoundsException) {
            false
        }
    }

    override fun next(context: RunActionContext): Boolean = this.stateLock.write {
        checkContext(context)
        val newIndex = this.evaluation.templateIndex + 1
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
        this.evaluation.tasks.any { it.status == DbTaskStatus.RUNNING }
        if (index >= 0 && index < this.template.tasks.size()) {
            /* Update active task. */
            this.evaluation.goTo(index)

            /* Enqueue WS message for sending */
            RunExecutor.broadcastWsMessage(ServerMessage(this.id, ServerMessageType.COMPETITION_UPDATE, this.evaluation.currentTask?.taskId))
            LOGGER.info("SynchronousRunManager ${this.id} set to task $index")
        } else {
            throw IndexOutOfBoundsException("Index $index is out of bounds for the number of available tasks.")
        }
    }

    override fun startTask(context: RunActionContext) = this.stateLock.write {
        checkStatus(RunManagerStatus.ACTIVE)
        assureNoRunningTask()
        checkContext(context)

        val currentTaskTemplate = this.currentTaskTemplate(context)
        if (!this.evaluation.allowRepeatedTasks && this.evaluation.tasks.any { it.template.id == currentTaskTemplate.id }) {
            throw IllegalStateException("Task '${currentTaskTemplate.name}' has already been used.")
        }

        /* Create and prepare pipeline for submission. */
        this.evaluation.ISTaskRun(currentTaskTemplate)

        /* Update status. */
        this.evaluation.currentTask!!.prepare()

        /* Reset the ReadyLatch. */
        this.readyLatch.reset(VIEWER_TIMEOUT_DEFAULT)

        /* Enqueue WS message for sending */
        RunExecutor.broadcastWsMessage(ServerMessage(this.id, ServerMessageType.TASK_PREPARE, this.evaluation.currentTask?.taskId))

        LOGGER.info("SynchronousRunManager ${this.id} started task ${this.evaluation.getCurrentTemplateId()}.")
    }

    override fun abortTask(context: RunActionContext) = this.stateLock.write {
        checkStatus(RunManagerStatus.ACTIVE)
        assertTaskPreparingOrRunning()
        checkContext(context)

        /* End TaskRun and persist. */
        this.currentTask(context)?.end()

        /* Enqueue WS message for sending */
        RunExecutor.broadcastWsMessage(ServerMessage(this.id, ServerMessageType.TASK_END, this.currentTask(context)?.taskId))
        LOGGER.info("SynchronousRunManager ${this.id} aborted task  ${this.evaluation.getCurrentTemplateId()}.")
    }

    /** List of [DbTask] for this [InteractiveSynchronousRunManager]. */
    override fun tasks(context: RunActionContext): List<AbstractInteractiveTask> = this.evaluation.tasks

    /**
     * Returns the currently active [DbTask]s or null, if no such task is active.
     *
     * @param context The [RunActionContext] used for the invocation.
     * @return [DbTask] or null
     */
    override fun currentTask(context: RunActionContext) = this.stateLock.read {
        when (this.evaluation.currentTask?.status) {
            DbTaskStatus.PREPARING,
            DbTaskStatus.RUNNING,
            DbTaskStatus.ENDED -> this.evaluation.currentTask
            else -> null
        }
    }

    /**
     * Returns [DbTask]s for a specific task [EvaluationId]. May be empty.
     *
     * @param taskId The [EvaluationId] of the [TaskRun].
     */
    override fun taskForId(context: RunActionContext, taskId: EvaluationId) =
        this.evaluation.tasks.find { it.taskId == taskId }

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
     * Returns the [DbSubmission]s for all currently active [DbTask]s or an empty [List], if no such task is active.
     *
     * @param context The [RunActionContext] used for the invocation.
     * @return List of [DbSubmission]s for the currently active [DbTask]
     */
    override fun currentSubmissions(context: RunActionContext): List<DbSubmission> = this.stateLock.read {
        this.currentTask(context)?.getSubmissions()?.toList() ?: emptyList()
    }

    /**
     * Returns the number of [DbTask]s held by this [RunManager].
     *
     * @return The number of [DbTask]s held by this [RunManager]
     */
    override fun taskCount(context: RunActionContext): Int = this.evaluation.tasks.size

    /**
     * Adjusts the duration of the current [DbTask] by the specified amount. Amount can be either positive or negative.
     *
     * @param s The number of seconds to adjust the duration by.
     * @return Time remaining until the task will end in milliseconds
     *
     * @throws IllegalArgumentException If the specified correction cannot be applied.
     * @throws IllegalStateException If [RunManager] was not in wrong [RunManagerStatus].
     */
    override fun adjustDuration(context: RunActionContext, s: Int): Long = this.stateLock.read {
        checkContext(context)

        /* Obtain task and perform sanity check. */
        val task = this.currentTask(context) ?: throw IllegalStateException("SynchronizedRunManager is in status ${this.status} but has no active TaskRun. This is a serious error!")
        check(task.isRunning) { "Task run '${this.name}.${task.position}' is currently not running. This is a programmer's error!" }

        /* Adjust duration. */
        val newDuration = task.duration + s
        check((newDuration * 1000L - (System.currentTimeMillis() - task.started!!)) > 0) { "New duration $s can not be applied because too much time has already elapsed." }
        task.duration = newDuration
        return (task.duration * 1000L - (System.currentTimeMillis() - task.started!!))
    }

    /**
     * Returns the time in milliseconds that is left until the end of the current [DbTask].
     * Only works if the [RunManager] is in wrong [RunManagerStatus]. If no task is running,
     * this method returns -1L.
     *
     * @return Time remaining until the task will end or -1, if no task is running.
     */
    override fun timeLeft(context: RunActionContext): Long = this.stateLock.read {
        return if (this.evaluation.currentTask?.status == DbTaskStatus.RUNNING) {
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
     * Returns the time in milliseconds that has elapsed since the start of the current [DbTask].
     * Only works if the [RunManager] is in wrong [RunManagerStatus]. If no task is running, this method returns -1L.
     *
     * @return Time remaining until the task will end or -1, if no task is running.
     */
    override fun timeElapsed(context: RunActionContext): Long = this.stateLock.read {
        return if (this.evaluation.currentTask?.status == DbTaskStatus.RUNNING) {
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
        checkStatus(RunManagerStatus.ACTIVE)
        assertTaskPreparingOrRunning()
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
                    this.store.transactional(true) {
                        if (this.evaluation.currentTask?.status == DbTaskStatus.PREPARING) {
                            this.readyLatch.setReady(connection)
                        }
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
     * Processes incoming [ApiSubmission]s. If a [DbTask] is running then that [ApiSubmission] will usually
     * be associated with that [DbTask].
     *
     * This method will not throw an exception and instead return false if a [ApiSubmission] was
     * ignored for whatever reason (usually a state mismatch). It is up to the caller to re-invoke
     * this method again.
     *
     * @param context The [RunActionContext] used for the invocation
     * @param submission [ApiSubmission] that should be registered.
     */
    override fun postSubmission(context: RunActionContext, submission: ApiSubmission) = this.stateLock.read {
        /* Register submission. */
        val task = this.currentTask(context) ?: throw IllegalStateException("Could not find ongoing task in run manager, despite correct status. This is a programmer's error!")

        /* Sanity check. */
        check(task.isRunning) { "Task run '${this.name}.${task.position}' is currently not running. This is a programmer's error!" }
        check(this.template.teams.asSequence().filter { it.teamId == submission.teamId }.any()) { "Team ${submission.teamId} does not exists for evaluation run ${this.name}. This is a programmer's error!" }

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
        RunExecutor.broadcastWsMessage(ServerMessage(this.id, ServerMessageType.TASK_UPDATED, task.taskId))
    }

    override fun reScore(taskId: TaskId) {
        val task = evaluation.tasks.find { it.taskId == taskId }
        if (task != null) {
            this.scoresUpdatable.enqueue(task)
        }
    }

    /**
     * Processes incoming [DbSubmission]s. If a [DbTask] is running then that [DbSubmission] will usually
     * be associated with that [DbTask].
     *
     * This method will not throw an exception and instead return false if a [DbSubmission] was
     * ignored for whatever reason (usually a state mismatch). It is up to the caller to re-invoke
     * this method again.
     *
     * @param context The [RunActionContext] used for the invocation
     * @param submissionId The [EvaluationId] of the [DbSubmission] to update.
     * @param submissionStatus The new [DbVerdictStatus]
     * @return True on success, false otherwise.
     */
    override fun updateSubmission(context: RunActionContext, submissionId: EvaluationId, submissionStatus: DbVerdictStatus): Boolean = this.stateLock.read {
        val answerSet = DbAnswerSet.filter { it.submission.submissionId eq submissionId }.singleOrNull() ?: return false
        val task = this.taskForId(context, answerSet.task.id) ?: return false

        /* Actual update - currently, only status update is allowed */
        if (answerSet.status != submissionStatus) {
            answerSet.status = submissionStatus

            /* Enqueue submission for post-processing. */
            this.scoresUpdatable.enqueue(task)

            /* Enqueue WS message for sending */
            RunExecutor.broadcastWsMessage(context.teamId!!, ServerMessage(this.id, ServerMessageType.TASK_UPDATED, task.taskId))
            return true
        }

        return false
    }

    /**
     * Method that orchestrates the internal progression of the [InteractiveSynchronousEvaluation].
     *
     * Implements the main run-loop.
     */
    override fun run() {
        /* Preparation . */
        this.stateLock.read {
            this.store.transactional {
                this.updatables.sortBy { it.phase } /* Sort list of by [Phase] in ascending order. */
                AccessManager.registerRunManager(this) /* Register the run manager with the access manager. */
            }
        }

        /* Initialize error counter. */
        var errorCounter = 0

        /* Start [InteractiveSynchronousRunManager]; main run-loop. */
        while (this.status != RunManagerStatus.TERMINATED) {
            try {
                /* Obtain lock on current state. */
                this.stateLock.read {
                    this.store.transactional {
                        /* 1) Invoke all relevant [Updatable]s. */
                        this.invokeUpdatables()

                        /* 2) Process internal state updates (if necessary). */
                        this.internalStateUpdate()
                    }
                }

                /* 3) Yield to other threads. */
                Thread.sleep(500)

                /* 4) Reset error counter and yield to other threads. */
                errorCounter = 0
            } catch (ie: InterruptedException) {
                LOGGER.info("Interrupted SynchronousRunManager, exiting")
                return
            } catch (e: Throwable) {
                LOGGER.error("Uncaught exception in run loop for competition run ${this.id}. Loop will continue to work but this error should be handled!", e)
                LOGGER.error("This is the ${++errorCounter}. in a row, will terminate loop after $MAXIMUM_RUN_LOOP_ERROR_COUNT errors")

                // oh shit, something went horribly, horribly wrong
                if (errorCounter >= MAXIMUM_RUN_LOOP_ERROR_COUNT) {
                    LOGGER.error("Reached maximum consecutive error count, terminating loop")
                    RunExecutor.dump(this.evaluation)
                    break //terminate loop
                }
            }
        }

        /* Finalization. */
        this.stateLock.read {
            this.store.transactional {
                this.invokeUpdatables() /* Invoke [Updatable]s one last time. */
                AccessManager.deregisterRunManager(this) /* De-register this run manager with the access manager. */
            }
        }

        LOGGER.info("SynchronousRunManager ${this.id} reached end of run logic.")
    }

    /**
     * Invokes all [Updatable]s registered with this [InteractiveSynchronousRunManager].
     */
    private fun invokeUpdatables() {
        val runStatus =  this.status
        val taskStatus = this.evaluation.currentTask?.status?.toApi()
        this.updatables.forEach {
            if (it.shouldBeUpdated(runStatus, taskStatus)) {
                it.update(runStatus, taskStatus)
            }
        }
    }

    /**
     * This is an internal method that facilitates internal state updates to this [InteractiveSynchronousRunManager],
     * i.e., status updates that are not triggered by an outside interaction.
     */
    private fun internalStateUpdate() {
        /** Case 1: Facilitates internal transition from RunManagerStatus.PREPARING_TASK to RunManagerStatus.RUNNING_TASK. */
        if (this.evaluation.currentTask?.status == DbTaskStatus.PREPARING && this.readyLatch.allReadyOrTimedOut()) {
            this.stateLock.write {
                this.evaluation.currentTask!!.start()
                DbAuditLogger.taskStart(this.id, this.evaluation.currentTask!!.taskId, this.evaluation.getCurrentTemplate(), DbAuditLogSource.INTERNAL, null)
            }

            /* Enqueue WS message for sending */
            RunExecutor.broadcastWsMessage(ServerMessage(this.id, ServerMessageType.TASK_START, this.evaluation.currentTask?.taskId))
        }

        /** Case 2: Facilitates internal transition from RunManagerStatus.RUNNING_TASK to RunManagerStatus.TASK_ENDED due to timeout. */
        if (this.evaluation.currentTask?.status == DbTaskStatus.RUNNING) {
            this.stateLock.write {
                val task = this.evaluation.currentTask!!
                val timeLeft = max(0L, task.duration * 1000L - (System.currentTimeMillis() - task.started!!) + InteractiveRunManager.COUNTDOWN_DURATION)
                if (timeLeft <= 0) {
                    task.end()
                    DbAuditLogger.taskEnd(this.id, this.evaluation.currentTask!!.taskId, DbAuditLogSource.INTERNAL, null)
                    EventStreamProcessor.event(TaskEndEvent(this.id, task.taskId))

                    /* Enqueue WS message for sending */
                    RunExecutor.broadcastWsMessage(ServerMessage(this.id, ServerMessageType.TASK_END, this.evaluation.currentTask?.taskId))
                }
            }
        }
    }

    /**
     * Applies the [DbTaskOption.PROLONG_ON_SUBMISSION] and [DbSubmissionOption.LIMIT_CORRECT_PER_TEAM] options.
     */
    private fun registerOptionalUpdatables() {
        /* Determine if any task should be prolonged upon submission. */
        val prolongOnSubmit = this.template.taskGroups.mapDistinct { it.type }.flatMapDistinct { it.options }.filter { it.description eq DbTaskOption.PROLONG_ON_SUBMISSION.description }.any()
        if (prolongOnSubmit) {
            this.updatables.add(ProlongOnSubmitUpdatable(this))
        }

        /* Determine if any task should be ended once submission threshold per team is reached. */
        val endOnSubmit = this.template.taskGroups.mapDistinct { it.type }.flatMapDistinct { it.submission }.filter { it.description eq DbSubmissionOption.LIMIT_CORRECT_PER_TEAM.description }.any()
        if (endOnSubmit) {
            this.updatables.add(EndOnSubmitUpdatable(this))
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

    /**
     * Internal assertion method that makes sure that a task is either preparing or running.
     *
     * @throws IllegalArgumentException If task is neither preparing nor running.
     */
    private fun assertTaskPreparingOrRunning() {
        val status = this.evaluation.currentTask?.status
        if (status != DbTaskStatus.RUNNING && status != DbTaskStatus.PREPARING) throw IllegalStateException("Task is neither preparing nor running.")
    }

    /**
     * Internal assertion method that makes sure that no task is running.
     *
     * @throws IllegalArgumentException If task is neither preparing nor running.
     */
    private fun assureNoRunningTask() {
        if (this.evaluation.tasks.any { it.status == DbTaskStatus.RUNNING }) throw IllegalStateException("Task is already running!")
    }
}