package dres.run

import dres.api.rest.types.WebSocketConnection
import dres.api.rest.types.run.websocket.ClientMessage
import dres.api.rest.types.run.websocket.ClientMessageType
import dres.api.rest.types.run.websocket.ServerMessage
import dres.api.rest.types.run.websocket.ServerMessageType
import dres.data.model.competition.CompetitionDescription
import dres.data.model.competition.interfaces.TaskDescription
import dres.data.model.run.CompetitionRun
import dres.data.model.run.Submission
import dres.data.model.run.SubmissionStatus
import dres.data.model.run.TaskRunData
import dres.run.filter.SubmissionFilter
import dres.run.score.interfaces.TaskRunScorer
import dres.run.updatables.*
import dres.run.validation.interfaces.JudgementValidator
import dres.run.validation.interfaces.SubmissionValidator
import dres.utilities.ReadyLatch
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * An implementation of [RunManager] aimed at distributed execution having a single DRES Server
 * instance and multiple viewers connected via WebSocket. Before starting a [Task], all viewer
 * instances are synchronized.
 *
 * @version 2.0.1
 * @author Ralph Gasser
 */
class SynchronousRunManager(val run: CompetitionRun) : RunManager {

    private val LOGGER = LoggerFactory.getLogger(this.javaClass)

    /**
     * Alternative constructor from existing [CompetitionRun].
     */
    constructor(description: CompetitionDescription, name: String) : this(CompetitionRun(-1L, name, description).apply { RunExecutor.runs.append(this) })

    /** Run ID of this [SynchronousRunManager]. */
    override val runId: Long
        get() = this.run.id

    /** Name of this [SynchronousRunManager]. */
    override val name: String
        get() = this.run.name

    override val uid: String
        get() = this.run.uid

    /** The [CompetitionDescription] executed by this [SynchronousRunManager]. */
    override val competitionDescription: CompetitionDescription
        get() = this.run.competitionDescription

    /** Reference to the currently active [TaskDescription]. This is part of the task navigation. */
    override var currentTask: TaskDescription = this.competitionDescription.tasks[0]
        private set

    /** Reference to the currently active [TaskRunData].*/
    override val currentTaskRun: TaskRunData?
        get() = this.run.currentTask?.data

    /** Currently active [TaskRunScorer]. */
    override val currentTaskScore: TaskRunScorer?
        get() = this.run.currentTask?.scorer

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

    override val judgementValidators: List<JudgementValidator>
        get() = this.run.runs.mapNotNull { if (it.hasStarted && it.validator is JudgementValidator) it.validator else null }

    override fun taskRunData(taskId: Int): TaskRunData? = this.run.runs.find { it.taskId == taskId }?.data

    /** The list of [Submission]s for the current [Task]. */
    override val submissions: List<Submission>
        get() = this.stateLock.read {
            this.run.currentTask?.data?.submissions ?: emptyList()
        }

    /** The list of all [Submission]s tracked ever received by this [SynchronousRunManager]. */
    override val allSubmissions: List<Submission>
        get() = this.stateLock.read {
            this.run.runs.flatMap { it.data.submissions }
        }

    /** Internal data structure that tracks all [WebSocketConnection]s and their ready state (for [RunManagerStatus.PREPARING_TASK]) */
    private val readyLatch = ReadyLatch<WebSocketConnection>()

    /** The internal [ScoreboardsUpdatable] instance for this [SynchronousRunManager]. */
    override val scoreboards = ScoreboardsUpdatable(this.competitionDescription.generateDefaultScoreboards(), this.run)

    /** The internal [MessageQueueUpdatable] instance used by this [SynchronousRunManager]. */
    private val messageQueueUpdatable = MessageQueueUpdatable(RunExecutor)

    /** The internal [ScoresUpdatable] instance for this [SynchronousRunManager]. */
    private val scoresUpdatable = ScoresUpdatable(this.runId, this.scoreboards, this.messageQueueUpdatable)

    /** The internal [DAOUpdatable] instance used by this [SynchronousRunManager]. */
    private val daoUpdatable = DAOUpdatable(RunExecutor.runs, this.run)

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

        /** End ongoing runs upon initialization (in case server crashed during task execution). */
        if (this.run.currentTask?.isRunning == true) {
            this.run.currentTask?.end()
        }

        /** Re-enqueue pending submissions (if any). */
        this.run.runs.forEach { run ->
            run.data.submissions.filter { it.status == SubmissionStatus.INDETERMINATE }.forEach {
                run.validator.validate(it)
            }
        }
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
        this.messageQueueUpdatable.enqueue(ServerMessage(this.runId, ServerMessageType.COMPETITION_START))

        LOGGER.info("SynchronousRunManager ${this.runId} started")
    }

    override fun terminate() = this.stateLock.write {
        check(this.status != RunManagerStatus.TERMINATED) { "SynchronizedRunManager is in status ${this.status} and cannot be terminated." }

        /* End the run. */
        this.run.end()

        /* Update status. */
        this.status = RunManagerStatus.TERMINATED

        /* Mark DAO for update. */
        this.daoUpdatable.dirty = true

        /* Enqueue WS message for sending */
        this.messageQueueUpdatable.enqueue(ServerMessage(this.runId, ServerMessageType.COMPETITION_END))

        LOGGER.info("SynchronousRunManager ${this.runId} terminated")
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
            this.messageQueueUpdatable.enqueue(ServerMessage(this.runId, ServerMessageType.COMPETITION_UPDATE))

            LOGGER.info("SynchronousRunManager ${this.runId} set to task $index")
        } else {
            throw IndexOutOfBoundsException("Index $index is out of bounds for the number of available tasks.")
        }
    }

    override fun startTask() = this.stateLock.write {
        check(this.status == RunManagerStatus.ACTIVE || this.status == RunManagerStatus.TASK_ENDED) { "SynchronizedRunManager is in status ${this.status}. Tasks can therefore not be started." }

        /* Create and prepare pipeline for submission. */
        val ret = this.run.newTaskRun(this.competitionDescription.tasks.indexOf(this.currentTask))
        val pipeline = Triple(ret.task.newFilter(), ret.task.newValidator(), ret.task.newScorer())
        (this.submissionPipeline as MutableList).add(pipeline)

        /* Update status. */
        this.status = RunManagerStatus.PREPARING_TASK

        /* Mark scoreboards and dao for update. */
        this.scoreboards.dirty = true
        this.daoUpdatable.dirty = true

        /* Reset the ReadyLatch. */
        this.readyLatch.reset()

        /* Enqueue WS message for sending */
        this.messageQueueUpdatable.enqueue(ServerMessage(this.runId, ServerMessageType.TASK_PREPARE))

        LOGGER.info("SynchronousRunManager ${this.runId} started task task ${this.currentTask}")
    }

    override fun abortTask() = this.stateLock.write {
        if (!(this.status == RunManagerStatus.PREPARING_TASK || this.status == RunManagerStatus.RUNNING_TASK)) {
            throw IllegalStateException("SynchronizedRunManager is in status ${this.status}. Tasks can therefore not be aborted.")
        }

        /*  End TaskRun and persist. */
        this.run.currentTask?.end()

        /* Update state. */
        this.status = RunManagerStatus.TASK_ENDED

        /* Mark scoreboards and dao for update. */
        this.scoreboards.dirty = true
        this.daoUpdatable.dirty = true

        /* Enqueue WS message for sending */
        this.messageQueueUpdatable.enqueue(ServerMessage(this.runId, ServerMessageType.TASK_END))

        LOGGER.info("SynchronousRunManager ${this.runId} aborted task task ${this.currentTask}")
    }

    override fun adjustDuration(s: Int): Long = this.stateLock.read {
        check(this.status == RunManagerStatus.RUNNING_TASK) { "SynchronizedRunManager is in status ${this.status}. Duration of task can therefore not be adjusted." }

        val newDuration = this.run.currentTask!!.duration + s
        check((newDuration * 1000L - (System.currentTimeMillis() - this.run.currentTask!!.started!!)) > 0) { "New duration $s can not be applied because too much time has already elapsed." }
        this.run.currentTask!!.duration = newDuration
        return (this.run.currentTask!!.duration * 1000L - (System.currentTimeMillis() - this.run.currentTask!!.started!!))
    }

    override fun timeLeft(): Long = this.stateLock.read {
        if (this.status == RunManagerStatus.RUNNING_TASK) {
            return (this.run.currentTask!!.duration * 1000L - (System.currentTimeMillis() - this.run.currentTask!!.started!!))
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
     * Processes incoming [Submission]s. If a [Task] is running then that [Submission] will usually
     * be associated with the current [TaskRun].
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
        val task = this.run.currentTask!!
        task.addSubmission(sub)

        /* Mark dao for update. */
        this.daoUpdatable.dirty = true

        /* Enqueue submission for post-processing. */
        this.scoresUpdatable.enqueue(Pair(task, sub))

        /* Enqueue WS message for sending */
        this.messageQueueUpdatable.enqueue(ServerMessage(this.runId, ServerMessageType.TASK_UPDATED))

        return sub.status
    }

    /**
     * Internal method that orchestrates the internal progression of the [CompetitionRun].
     */
    override fun run() {
        /** Sort list of by [Phase] in ascending order. */
        this.updatables.sortBy { it.phase }

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
            } catch (e: Throwable) {
                LOGGER.error("Uncaught exception in run loop for competition run ${this.runId}. Loop will continue to work but this error should be handled!", e)
            }
        }

        /** Invoke [Updatables] one last time. */
        this.stateLock.read {
            this.invokeUpdatables()
        }

        LOGGER.info("SynchronousRunManager ${this.runId} reached end of run logic.")
    }

    /**
     * Invokes all [Updatable]s registered with this [SynchronousRunManager].
     *
     * @param status The [RunManagerStatus] for which to trigger the [Updatable]s.
     */
    private fun invokeUpdatables() {
        this.updatables.forEach {
            if (it.shouldBeUpdated(this.status)) {
                try{
                    it.update(this.status)
                } catch (e: Throwable) {
                    LOGGER.error("Uncaught exception while updating ${it.javaClass.simpleName} for competition run ${this.runId}. Loop will continue to work but this error should be handled!", e)
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
        if (this.status == RunManagerStatus.PREPARING_TASK && this.readyLatch.allReady()) {
            this.stateLock.write {
                this.run.currentTask?.start()
                this.status = RunManagerStatus.RUNNING_TASK
            }

            /* Mark DAO for update. */
            this.daoUpdatable.dirty = true

            /* Enqueue WS message for sending */
            this.messageQueueUpdatable.enqueue(ServerMessage(this.runId, ServerMessageType.TASK_START))
        }

        /** Case 2: Facilitates internal transition from RunManagerStatus.RUNNING_TASK to RunManagerStatus.TASK_ENDED due to timeout. */
        if (this.status == RunManagerStatus.RUNNING_TASK) {
            val timeLeft = this.timeLeft()
            if (timeLeft <= 0) {
                this.stateLock.write {
                    this.run.currentTask?.end()
                    this.status = RunManagerStatus.TASK_ENDED
                }

                /* Mark DAO for update. */
                this.daoUpdatable.dirty = true

                /* Enqueue WS message for sending */
                this.messageQueueUpdatable.enqueue(ServerMessage(this.runId, ServerMessageType.TASK_END))
            }
        }
    }
}