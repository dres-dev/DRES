package dres.run

import dres.api.rest.types.run.ClientMessage
import dres.api.rest.types.run.ClientMessageType
import dres.api.rest.types.run.ServerMessage
import dres.api.rest.types.run.ServerMessageType
import dres.data.dbo.DAO
import dres.data.model.competition.Competition
import dres.data.model.competition.Task
import dres.data.model.run.CompetitionRun
import dres.data.model.run.Submission
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.math.max
import kotlin.math.min

/**
 * An implementation of [RunManager] aimed at distributed execution having a single DRES Server
 * instance and multiple viewers connected via WebSocket. Before starting a [Task], all viewer
 * instances are synchronized.
 *
 * @version 1.0
 * @author Ralph Gasser
 */
class SynchronousRunManager(competition: Competition, name: String, override val scoreboards: List<Scoreboard>, private val executor: RunExecutor, private val dao: DAO<CompetitionRun>) : RunManager {

    /** The [CompetitionRun] capturing the state of this [SynchronousRunManager]. */
    private val run = CompetitionRun(-1, name, competition)

    /** Run ID of this [SynchronousRunManager]. */
    override val runId: Long
        get() = this.run.id

    /** Name of this [SynchronousRunManager]. */
    override val name: String
        get() = this.run.name

    /** The [Competition] executed by this [SynchronousRunManager]. */
    override val competition: Competition
        get() = this.run.competition

    /** Currently active task. */
    override var currentTask: Task? = null
        private set

    /** The status of this [RunManager]. */
    @Volatile
    override var status: RunManagerStatus = RunManagerStatus.CREATED
        get() = this.stateLock.read {
            return field
        }
        private set

    /** The list of [Submission]s fpr the current [Task]. */
    override val submissions: List<Submission>
        get() = this.stateLock.read {
            this.run.currentTask?.submissions ?: emptyList()
        }

    /** A lock for state changes to this [SynchronousRunManager]. */
    private val stateLock = ReentrantReadWriteLock()

    /** Internal counter used to count the number of WebSocket connected clients. */
    @Volatile
    private var clientCounter = 0

    /**
     * Internal counter used to count the number ACKs received since the last issuing of a
     * [ServerMessageType.TASK_PREPARE] message. -1 in cases where no [Task] is running.
     */
    @Volatile
    private var ackCounter = -1

    init {
        this.run.id = this.dao.append(this.run)
    }

    override fun start() = this.stateLock.write {
        if (this.status != RunManagerStatus.CREATED) throw IllegalStateException("DistributedRunManager is in status ${this.status} and cannot be started.")
        this.run.start()
        this.status = RunManagerStatus.ACTIVE
        this.goToTask(0)
        this.executor.broadcastWsMessage(ServerMessage(this.runId, ServerMessageType.COMPETITION_START))
    }

    override fun terminate() = this.stateLock.write {
        if (this.status != RunManagerStatus.ACTIVE) throw IllegalStateException("DistributedRunManager is in status ${this.status} and cannot be terminated.")
        this.run.end()
        this.status = RunManagerStatus.TERMINATED
        this.executor.broadcastWsMessage(ServerMessage(this.runId, ServerMessageType.COMPETITION_END))
    }

    override fun previousTask(): Boolean = this.stateLock.write {
        val newIndex = this.competition.tasks.indexOf(this.currentTask) - 1
        return try {
            this.goToTask(newIndex)
            true
        } catch (e: IndexOutOfBoundsException) {
            false
        }
    }

    override fun nextTask(): Boolean = this.stateLock.write {
        check(this.status == RunManagerStatus.ACTIVE) { "DistributedRunManager is in status ${this.status}. Tasks can therefore not be changed." }
        val newIndex = this.competition.tasks.indexOf(this.currentTask) + 1
        return try {
            this.goToTask(newIndex)
            true
        } catch (e: IndexOutOfBoundsException) {
            false
        }
    }

    override fun goToTask(index: Int) = this.stateLock.write {
        check(this.status == RunManagerStatus.ACTIVE) { "DistributedRunManager is in status ${this.status}. Tasks can therefore not be changed." }
        if (index >= 0 && index < this.competition.tasks.size) {
            this.currentTask = this.competition.tasks[index]
            this.executor.broadcastWsMessage(ServerMessage(this.runId, ServerMessageType.COMPETITION_UPDATE))
        } else {
            throw IndexOutOfBoundsException("Index $index is out of bounds for the number of available tasks.")
        }
    }

    override fun startTask() = this.stateLock.write {
        check(this.status == RunManagerStatus.ACTIVE) { "DistributedRunManager is in status ${this.status}. Tasks can therefore not be started." }

        /* Update status. */
        this.run.newTaskRun(this.competition.tasks.indexOf(this.currentTask))
        this.status = RunManagerStatus.PREPARING_TASK
        this.ackCounter = 0
        this.executor.broadcastWsMessage(this.runId, ServerMessage(this.runId, ServerMessageType.TASK_PREPARE))
    }

    override fun abortTask() = this.stateLock.write {
        if (this.status != RunManagerStatus.PREPARING_TASK && this.status != RunManagerStatus.RUNNING_TASK) throw IllegalStateException("DistributedRunManager is in status ${this.status}. Tasks can therefore not be aborted.")

        /**  End TaskRun. */
        this.run.currentTask?.end()
        this.dao.update(this.run)

        this.status = RunManagerStatus.ACTIVE
        this.ackCounter = -1
        this.executor.broadcastWsMessage(this.runId, ServerMessage(this.runId, ServerMessageType.TASK_END))
    }

    override fun timeLeft(): Long = this.stateLock.read {
        if (this.status == RunManagerStatus.RUNNING_TASK) {
            return (this.run.currentTask!!.task.duration - (System.currentTimeMillis() - this.run.currentTask!!.started!!))
        } else {
            -1L
        }
    }

    /**
     * Processes WebSocket message received by the [RunExecutor].
     */
    override fun wsMessageReceived(message: ClientMessage): Boolean = this.stateLock.read {
        when (message.type) {
            ClientMessageType.ACK -> {
                if (this.status == RunManagerStatus.PREPARING_TASK) {
                    this.ackCounter += 1
                }
            }
            ClientMessageType.REGISTER -> this.clientCounter += 1
            ClientMessageType.UNREGISTER -> this.clientCounter -= 1
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
    override fun postSubmission(sub: Submission): Boolean = this.stateLock.read {
        return if (this.status == RunManagerStatus.RUNNING_TASK) {
            /* Register submission. */
            this.run.currentTask?.addSubmission(sub)

            /* Update scoreboards. */
            this.scoreboards.forEach { it.update() }

            /* Inform clients about update. */
            this.executor.broadcastWsMessage(ServerMessage(this.runId, ServerMessageType.TASK_UPDATED))

            true
        } else {
            false
        }
    }

    /**
     * Internal method that orchestrates the internal progression of the [CompetitionRun].
     */
    override fun run() {
        /** Wait for [SynchronousRunManager] to be started. */
        while (true) {
            if (this.status != RunManagerStatus.CREATED) {
                break
            }
            Thread.onSpinWait()
        }

        /** Handles the activity part of the DistributedRunManager (status = ACTIVE).*/
        while (this.status == RunManagerStatus.ACTIVE || this.status == RunManagerStatus.PREPARING_TASK || this.status == RunManagerStatus.RUNNING_TASK) {

            /** Handles the preparation period of the DistributedRunManager (status = PREPARING_TASK). */
            while (this.status == RunManagerStatus.PREPARING_TASK) {
                if (this.ackCounter >= this.clientCounter) {
                    this.stateLock.write {
                        this.run.currentTask?.start()
                        this.status = RunManagerStatus.RUNNING_TASK
                    }
                    this.executor.broadcastWsMessage(ServerMessage(this.runId, ServerMessageType.TASK_START))
                    break
                }

                /** Sleep for 250ms. */
                Thread.sleep(250)
            }

            /** Handles the task execution period of the DistributedRunManager (status = RUNNING_TASK). */
            while (this.status == RunManagerStatus.RUNNING_TASK) {
                val timeLeft = this.timeLeft()
                if (timeLeft <= 0) {
                    this.stateLock.write {
                        this.run.currentTask!!.end()
                        this.status = RunManagerStatus.ACTIVE
                    }
                    this.dao.update(this.run)
                    this.executor.broadcastWsMessage(ServerMessage(this.runId, ServerMessageType.TASK_END))
                    break
                }

                this.run.updateSubmissionValidations()

                /** Sleep for 250ms. */
                Thread.sleep(max(min(2000, timeLeft), 10000))
            }

            this.run.updateSubmissionValidations()

            Thread.onSpinWait()
        }
    }
}