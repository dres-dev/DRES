package dev.dres.run

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import dev.dres.api.rest.types.WebSocketConnection
import dev.dres.api.rest.types.run.websocket.ClientMessage
import dev.dres.api.rest.types.run.websocket.ClientMessageType
import dev.dres.api.rest.types.run.websocket.ServerMessage
import dev.dres.api.rest.types.run.websocket.ServerMessageType
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.data.model.UID
import dev.dres.data.model.competition.CompetitionDescription
import dev.dres.data.model.competition.TaskDescription
import dev.dres.data.model.run.CompetitionRun
import dev.dres.data.model.run.Submission
import dev.dres.data.model.run.SubmissionStatus
import dev.dres.run.audit.AuditLogger
import dev.dres.run.audit.LogEventSource
import dev.dres.run.eventstream.EventStreamProcessor
import dev.dres.run.eventstream.TaskEndEvent
import dev.dres.run.filter.SubmissionFilter
import dev.dres.run.score.ScoreTimePoint
import dev.dres.run.score.interfaces.TaskRunScorer
import dev.dres.run.updatables.*
import dev.dres.run.validation.interfaces.JudgementValidator
import dev.dres.run.validation.interfaces.SubmissionValidator
import dev.dres.utilities.ReadyLatch
import dev.dres.utilities.extensions.UID
import org.slf4j.LoggerFactory
import java.io.File
import java.util.*
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.math.max

/**
 * An implementation of [RunManager] aimed at distributed execution having a single DRES Server instance and multiple
 * viewers connected via WebSocket. Before starting a [CompetitionRun.TaskRun], all viewer instances are synchronized.
 *
 * @version 2.1.0
 * @author Ralph Gasser
 */
class SynchronousRunManager(val run: CompetitionRun) : RunManager {

    private val VIEWER_TIME_OUT = 30L //TODO make configurable

    private val LOGGER = LoggerFactory.getLogger(this.javaClass)

    /**
     * Number of consecutive errors which have to occur within the main execution loop before it tries to gracefully terminate
     */
    private val maxErrorCount = 5

    /**
     * Alternative constructor from existing [CompetitionRun].
     */
    constructor(description: CompetitionDescription, name: String) : this(CompetitionRun(UID.EMPTY, name, description).apply { RunExecutor.runs.append(this) })

    /** Run ID of this [SynchronousRunManager]. */
    override val id: UID
        get() = this.run.id

    /** Name of this [SynchronousRunManager]. */
    override val name: String
        get() = this.run.name

    /** The [CompetitionDescription] executed by this [SynchronousRunManager]. */
    override val competitionDescription: CompetitionDescription
        get() = this.run.competitionDescription

    /** Reference to the currently active [TaskDescription]. This is part of the task navigation. */
    override var currentTask: TaskDescription = this.competitionDescription.tasks[0]
        private set

    /** Reference to the currently active [CompetitionRun.TaskRun].*/
    override val currentTaskRun: CompetitionRun.TaskRun?
        get() = this.stateLock.read {
            return when (this.status) {
                RunManagerStatus.PREPARING_TASK,
                RunManagerStatus.RUNNING_TASK,
                RunManagerStatus.TASK_ENDED -> this.run.lastTask
                else -> null
            }
        }

    /** The list of [Submission]s for the current [CompetitionRun.TaskRun]. */
    override val submissions: List<Submission>
        get() = this.stateLock.read {
            this.currentTaskRun?.submissions ?: emptyList()
        }

    /** The list of all [Submission]s tracked ever received by this [SynchronousRunManager]. */
    override val allSubmissions: List<Submission>
        get() = this.stateLock.read {
            this.run.runs.flatMap { it.submissions }
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

    /** Returns list [JudgementValidator]s associated with this [SynchronousRunManager]. May be empty*/
    override val judgementValidators: List<JudgementValidator>
        get() = this.run.runs.mapNotNull { if (it.hasStarted && it.validator is JudgementValidator) it.validator else null }

    /** Internal data structure that tracks all [WebSocketConnection]s and their ready state (for [RunManagerStatus.PREPARING_TASK]) */
    private val readyLatch = ReadyLatch<WebSocketConnection>()

    /** History of scores over time*/
    private val scoreTimeSeries = ArrayList<ScoreTimePoint>()

    override val scoreHistory: List<ScoreTimePoint>
        get() = scoreTimeSeries

    /** The internal [ScoreboardsUpdatable] instance for this [SynchronousRunManager]. */
    override val scoreboards = ScoreboardsUpdatable(this.competitionDescription.generateDefaultScoreboards(), this.run, scoreTimeSeries)

    /** The internal [MessageQueueUpdatable] instance used by this [SynchronousRunManager]. */
    private val messageQueueUpdatable = MessageQueueUpdatable(RunExecutor)

    /** The internal [ScoresUpdatable] instance for this [SynchronousRunManager]. */
    private val scoresUpdatable = ScoresUpdatable(this.id, this.scoreboards, this.messageQueueUpdatable)

    /** The internal [DAOUpdatable] instance used by this [SynchronousRunManager]. */
    private val daoUpdatable = DAOUpdatable(RunExecutor.runs, this.run)

    /** The internal [DAOUpdatable] used to end a task once no more submissions are possible */
    private val endTaskUpdatable = EndTaskUpdatable(this)

    /** List of [Updatable] held by this [SynchronousRunManager]. */
    private val updatables = mutableListOf<Updatable>()

    /** The pipeline for [Submission] processing. All [Submission]s undergo three steps: filter, validation and score update. */
    private val submissionPipeline: List<Triple<SubmissionFilter,SubmissionValidator, TaskRunScorer>> = LinkedList()

    /** A lock for state changes to this [SynchronousRunManager]. */
    private val stateLock = ReentrantReadWriteLock()

    init {
        /* Register relevant Updatables. */
        this.updatables.add(this.scoresUpdatable)
        this.updatables.add(this.scoreboards)
        this.updatables.add(this.messageQueueUpdatable)
        this.updatables.add(this.daoUpdatable)
        this.updatables.add(this.endTaskUpdatable)


        /** End ongoing runs upon initialization (in case server crashed during task execution). */
        if (this.run.lastTask?.isRunning == true) {
            this.run.lastTask?.end()
        }

        /** Re-enqueue pending submissions (if any). */
        this.run.runs.forEach { run ->
            run.submissions.filter { it.status == SubmissionStatus.INDETERMINATE }.forEach {
                run.validator.validate(it)
            }
        }

        /** Re-calculate all the relevant scores. */
        this.run.runs.forEach { run ->
            run.submissions.forEach { sub ->
                this.scoresUpdatable.enqueue(Pair(run, sub))
            }
        }
        this.scoresUpdatable.update(this.status)
    }

    override fun start() = this.stateLock.write {
        check(this.status == RunManagerStatus.CREATED) { "SynchronizedRunManager is in status ${this.status} and cannot be started." }

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

    override fun end() = this.stateLock.write {
        check(this.status != RunManagerStatus.TERMINATED) { "SynchronizedRunManager is in status ${this.status} and cannot be terminated." }

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

    override fun previousTask(): Boolean = this.stateLock.write {
        val newIndex = this.competitionDescription.tasks.indexOf(this.currentTask) - 1
        return try {
            this.goToTask(newIndex)
            true
        } catch (e: IndexOutOfBoundsException) {
            false
        }
    }

    override fun nextTask(): Boolean = this.stateLock.write {
        val newIndex = this.competitionDescription.tasks.indexOf(this.currentTask) + 1
        return try {
            this.goToTask(newIndex)
            true
        } catch (e: IndexOutOfBoundsException) {
            false
        }
    }

    override fun goToTask(index: Int) = this.stateLock.write {
        check(this.status == RunManagerStatus.ACTIVE || this.status == RunManagerStatus.TASK_ENDED) { "SynchronizedRunManager is in status ${this.status}. Tasks can therefore not be changed." }
        if (index >= 0 && index < this.competitionDescription.tasks.size) {

            /* Update active task. */
            this.currentTask = this.competitionDescription.tasks[index]

            /* Update RunManager status. */
            this.status = RunManagerStatus.ACTIVE

            /* Mark scoreboards for update. */
            this.scoreboards.dirty = true

            /* Enqueue WS message for sending */
            this.messageQueueUpdatable.enqueue(ServerMessage(this.id.string, ServerMessageType.COMPETITION_UPDATE))

            LOGGER.info("SynchronousRunManager ${this.id} set to task $index")
        } else {
            throw IndexOutOfBoundsException("Index $index is out of bounds for the number of available tasks.")
        }
    }

    override fun startTask() = this.stateLock.write {
        check(this.status == RunManagerStatus.ACTIVE || this.status == RunManagerStatus.TASK_ENDED) { "SynchronizedRunManager is in status ${this.status}. Tasks can therefore not be started." }

        /* Create and prepare pipeline for submission. */
        val ret = this.run.newTaskRun(this.currentTask.id)
        val pipeline = Triple(ret.task.newFilter(), ret.task.newValidator(), ret.task.newScorer())
        (this.submissionPipeline as MutableList).add(pipeline)

        /* Update status. */
        this.status = RunManagerStatus.PREPARING_TASK

        /* Mark scoreboards and dao for update. */
        this.scoreboards.dirty = true
        this.daoUpdatable.dirty = true

        /* Reset the ReadyLatch. */
        this.readyLatch.reset(VIEWER_TIME_OUT)

        /* Enqueue WS message for sending */
        this.messageQueueUpdatable.enqueue(ServerMessage(this.id.string, ServerMessageType.TASK_PREPARE))

        LOGGER.info("SynchronousRunManager ${this.id} started task task ${this.currentTask}")
    }

    override fun abortTask() = this.stateLock.write {
        if (!(this.status == RunManagerStatus.PREPARING_TASK || this.status == RunManagerStatus.RUNNING_TASK)) {
            throw IllegalStateException("SynchronizedRunManager is in status ${this.status}. Tasks can therefore not be aborted.")
        }

        /*  End TaskRun and persist. */
        this.currentTaskRun?.end()

        /* Update state. */
        this.status = RunManagerStatus.TASK_ENDED

        /* Mark scoreboards and dao for update. */
        this.scoreboards.dirty = true
        this.daoUpdatable.dirty = true

        /* Enqueue WS message for sending */
        this.messageQueueUpdatable.enqueue(ServerMessage(this.id.string, ServerMessageType.TASK_END))

        LOGGER.info("SynchronousRunManager ${this.id} aborted task task ${this.currentTask}")
    }

    /**
     * Returns [CompetitionRun.TaskRun]s for a specific task [UID]. May be empty.
     *
     * @param taskRunId The [UID] of the [CompetitionRun.TaskRun].
     */
    override fun taskRunForId(taskRunId: UID): CompetitionRun.TaskRun? = this.run.runs.find { it.uid == taskRunId }

    /**
     * Returns the number of [CompetitionRun.TaskRun]s held by this [RunManager].
     *
     * @return The number of [CompetitionRun.TaskRun]s held by this [RunManager]
     */
    override fun taskRuns(): Int = this.run.runs.size

    /**
     * Adjusts the duration of the current [CompetitionRun.TaskRun] by the specified amount. Amount can be either positive or negative.
     *
     * @param s The number of seconds to adjust the duration by.
     * @return Time remaining until the task will end in milliseconds
     *
     * @throws IllegalArgumentException If the specified correction cannot be applied.
     * @throws IllegalStateException If [RunManager] was not in status [RunManagerStatus.RUNNING_TASK].
     */
    override fun adjustDuration(s: Int): Long = this.stateLock.read {
        check(this.status == RunManagerStatus.RUNNING_TASK) { "SynchronizedRunManager is in status ${this.status}. Duration of task can therefore not be adjusted." }

        val currentTaskRun = this.currentTaskRun ?: throw IllegalStateException("SynchronizedRunManager is in status ${this.status} but has no active TaskRun. This is a serious error!")
        val newDuration = currentTaskRun.duration + s
        check((newDuration * 1000L - (System.currentTimeMillis() - currentTaskRun.started!!)) > 0) { "New duration $s can not be applied because too much time has already elapsed." }
        currentTaskRun.duration = newDuration
        return (currentTaskRun.duration * 1000L - (System.currentTimeMillis() - currentTaskRun.started!!))
    }

    /**
     * Returns the time in milliseconds that is left until the end of the current [CompetitionRun.TaskRun].
     * Only works if the [RunManager] is in state [RunManagerStatus.RUNNING_TASK]. If no task is running,
     * this method returns -1L.
     *
     * @return Time remaining until the task will end or -1, if no task is running.
     */
    override fun timeLeft(): Long = this.stateLock.read {
        if (this.status == RunManagerStatus.RUNNING_TASK) {
            val currentTaskRun = this.currentTaskRun ?: throw IllegalStateException("SynchronizedRunManager is in status ${this.status} but has no active TaskRun. This is a serious error!")
            return max(0L, currentTaskRun.duration * 1000L - (System.currentTimeMillis() - currentTaskRun.started!!))
        } else {
            -1L
        }
    }

    /**
     * Lists  all WebsSocket session IDs for viewer instances currently registered to this [SynchronousRunManager].
     *
     * @return Map of session ID to ready state.
     */
    override fun viewers(): HashMap<WebSocketConnection, Boolean> = this.readyLatch.state()

    /**
     * Can be used to manually override the READY state of a viewer. Can be used in case a viewer hangs in the PREPARING_TASK phase.
     *
     * @param viewerId The ID of the viewer's WebSocket session.
     */
    override fun overrideReadyState(viewerId: String): Boolean = this.stateLock.read {
        check(this.status == RunManagerStatus.PREPARING_TASK) { }
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
    override fun wsMessageReceived(connection: WebSocketConnection, message: ClientMessage): Boolean = this.stateLock.read {
        when (message.type) {
            ClientMessageType.ACK -> {
                if (this.status == RunManagerStatus.PREPARING_TASK) {
                    this.readyLatch.setReady(connection)
                }
            }
            ClientMessageType.REGISTER -> this.readyLatch.register(connection)
            ClientMessageType.UNREGISTER -> this.readyLatch.unregister(connection)
            ClientMessageType.PING -> {} //handled in [RunExecutor]
        }
        return true
    }

    /**
     * Processes incoming [Submission]s. If a [CompetitionRun.TaskRun] is running then that [Submission] will usually
     * be associated with that [CompetitionRun.TaskRun].
     *
     * This method will not throw an exception and instead return false if a [Submission] was
     * ignored for whatever reason (usually a state mismatch). It is up to the caller to re-invoke
     * this method again.
     *
     * @param sub [Submission] that should be registered.
     */
    override fun postSubmission(sub: Submission): SubmissionStatus = this.stateLock.read {
        check(this.status == RunManagerStatus.RUNNING_TASK) { "SynchronizedRunManager is in status ${this.status} and can currently not accept submissions." }

        /* Register submission. */
        val task = this.currentTaskRun!!
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
     * Processes incoming [Submission]s. If a [CompetitionRun.TaskRun] is running then that [Submission] will usually
     * be associated with that [CompetitionRun.TaskRun].
     *
     * This method will not throw an exception and instead return false if a [Submission] was
     * ignored for whatever reason (usually a state mismatch). It is up to the caller to re-invoke
     * this method again.
     *
     * @param sub [Submission] that should be registered.
     */
    override fun updateSubmission(suid: UID, newStatus: SubmissionStatus): Boolean = this.stateLock.read {
        // We have to ignore anything state related, as we allow updates by admins regardless of state
//        check(this.status == RunManagerStatus.RUNNING_TASK) { "SynchronizedRunManager is in status ${this.status} and can currently not accept submissions." }

        /* Sanity check */
        val found = submissions.find { it.uid == suid}
                ?: return false
        /* Actual update - currently, only status update is allowed */
        found.status = newStatus

        /* Mark dao for update. */
        this.daoUpdatable.dirty = true

        /* Enqueue submission for post-processing. */
        this.scoresUpdatable.enqueue(Pair(found.taskRun!!, found))

        /* Enqueue WS message for sending */
        this.messageQueueUpdatable.enqueue(ServerMessage(this.id.string, ServerMessageType.TASK_UPDATED))

        return true
    }

    /**
     * Internal method that orchestrates the internal progression of the [CompetitionRun].
     */
    override fun run() {
        /** Sort list of by [Phase] in ascending order. */
        this.updatables.sortBy { it.phase }

        var errorCounter = 0

        /** Start [SynchronousRunManager] . */
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
            } catch (ie: InterruptedException) {
                LOGGER.info("Interrupted SynchronousRunManager, exiting")
                return
            } catch (e: Throwable) {
                LOGGER.error("Uncaught exception in run loop for competition run ${this.id}. Loop will continue to work but this error should be handled!", e)
                LOGGER.error("This is the ${++errorCounter}. in a row, will terminate loop after $maxErrorCount errors")

                // oh shit, something went horribly horribly wrong
                if (errorCounter >= maxErrorCount){
                    LOGGER.error("Reached maximum consecutive error count, terminating loop")
                    this.persistCurrentRunInformation()
                    break //terminate loop
                }

            } finally {
                errorCounter = 0
            }
        }

        /** Invoke [Updatable]s one last time. */
        this.stateLock.read {
            this.invokeUpdatables()
        }

        LOGGER.info("SynchronousRunManager ${this.id} reached end of run logic.")
    }

    /**
     * Tries to persist the information of a current run in case something goes horribly wrong
     */
    private fun persistCurrentRunInformation() {
        try{
            val file = File("run_dump_${this.run.id.string}.json")
            jacksonObjectMapper().writeValue(file, this.run)
            LOGGER.info("Wrote current run state to ${file.absolutePath}")
        } catch (e: Exception){
            LOGGER.error("Could not write run to disk: ", e)
        }
    }

    /**
     * Invokes all [Updatable]s registered with this [SynchronousRunManager].
     */
    private fun invokeUpdatables() {
        this.updatables.forEach {
            if (it.shouldBeUpdated(this.status)) {
                try{
                    it.update(this.status)
                } catch (e: Throwable) {
                    LOGGER.error("Uncaught exception while updating ${it.javaClass.simpleName} for competition run ${this.id}. Loop will continue to work but this error should be handled!", e)
                }
            }
        }
    }

    /**
     * This is an internal method that facilitates internal state updates to this [SynchronousRunManager],
     * i.e., status updates that are not triggered by an outside interaction.
     */
    private fun internalStateUpdate() {
        /** Case 1: Facilitates internal transition from RunManagerStatus.PREPARING_TASK to RunManagerStatus.RUNNING_TASK. */
        if (this.status == RunManagerStatus.PREPARING_TASK && this.readyLatch.allReadyOrTimedOut()) {
            this.stateLock.write {
                this.currentTaskRun?.start()
                this.status = RunManagerStatus.RUNNING_TASK
                AuditLogger.taskStart(this.id, this.currentTask.name, LogEventSource.INTERNAL, null)
            }

            /* Mark DAO for update. */
            this.daoUpdatable.dirty = true

            /* Enqueue WS message for sending */
            this.messageQueueUpdatable.enqueue(ServerMessage(this.id.string, ServerMessageType.TASK_START))
        }

        /** Case 2: Facilitates internal transition from RunManagerStatus.RUNNING_TASK to RunManagerStatus.TASK_ENDED due to timeout. */
        if (this.status == RunManagerStatus.RUNNING_TASK) {
            val timeLeft = this.timeLeft()
            if (timeLeft <= 0) {
                this.stateLock.write {
                    val task = this.currentTaskRun!!
                    task.end()
                    this.status = RunManagerStatus.TASK_ENDED
                    AuditLogger.taskEnd(this.id, this.currentTask.name, LogEventSource.INTERNAL, null);
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