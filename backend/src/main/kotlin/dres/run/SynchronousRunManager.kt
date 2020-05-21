package dres.run

import dres.api.rest.types.run.websocket.ClientMessage
import dres.api.rest.types.run.websocket.ClientMessageType
import dres.api.rest.types.run.websocket.ServerMessage
import dres.api.rest.types.run.websocket.ServerMessageType
import dres.data.dbo.DAO
import dres.data.model.competition.CompetitionDescription
import dres.data.model.competition.interfaces.TaskDescription
import dres.data.model.run.CompetitionRun
import dres.data.model.run.Submission
import dres.data.model.run.SubmissionStatus
import dres.data.model.run.TaskRunData
import dres.run.filter.SubmissionFilter
import dres.run.score.interfaces.TaskRunScorer
import dres.run.score.scoreboard.Scoreboard
import dres.run.validation.interfaces.JudgementValidator
import dres.run.validation.interfaces.SubmissionValidator
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
 * @version 1.0
 * @author Ralph Gasser
 */
class SynchronousRunManager(competitionDescription: CompetitionDescription, name: String, override val scoreboards: List<Scoreboard>, private val executor: RunExecutor, private val dao: DAO<CompetitionRun>) : RunManager {

    private val logger = LoggerFactory.getLogger(this.javaClass)

    /** The [CompetitionRun] capturing the state of this [SynchronousRunManager]. */
    private val run = CompetitionRun(-1, name, competitionDescription)

    /** Run ID of this [SynchronousRunManager]. */
    override val runId: Long
        get() = this.run.id

    /** Name of this [SynchronousRunManager]. */
    override val name: String
        get() = this.run.name

    /** The [CompetitionDescription] executed by this [SynchronousRunManager]. */
    override val competitionDescription: CompetitionDescription
        get() = this.run.competitionDescription

    /** Currently active [TaskDescription]. */
    override var currentTask: TaskDescription? = null
        private set

    override val currentTaskRun: TaskRunData?
        get() = run.currentTask?.data

    /** Currently active [TaskRunScorer]. */
    override val currentTaskScore: TaskRunScorer?
        get() = this.run.currentTask?.scorer

    /** The status of this [RunManager]. */
    @Volatile
    override var status: RunManagerStatus = RunManagerStatus.CREATED
        get() = this.stateLock.read {
            return field
        }
        private set

    override val judgementValidators: List<JudgementValidator>
        get() = this.run.runs.mapNotNull { if (it.hasStarted && it.validator is JudgementValidator) it.validator else null }

    /** The list of [Submission]s for the current [Task]. */
    override val submissions: List<Submission>?
        get() = this.stateLock.read {
            this.run.currentTask?.data?.submissions ?: emptyList()
        }

    /** The pipeline for [Submission] processing. All [Submission]s undergo three steps: filter, validation and score update. */
    private val submissionPipeline: List<Triple<SubmissionFilter,SubmissionValidator, TaskRunScorer>> = LinkedList()

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

    /** A flag indicating whether the [Scoreboard]s need an update. */
    @Volatile
    private var scoreboardUpdateRequired = false

    init {
        this.run.id = this.dao.append(this.run)
        //lambda to send submission updates
        this.run.updateSubmissionStatus = {this.executor.broadcastWsMessage(ServerMessage(this.runId, ServerMessageType.TASK_UPDATED))}
    }

    override fun start() = this.stateLock.write {
        if (this.status != RunManagerStatus.CREATED) throw IllegalStateException("SynchronizedRunManager is in status ${this.status} and cannot be started.")
        this.run.start()
        this.status = RunManagerStatus.ACTIVE
        this.goToTask(0)
        this.executor.broadcastWsMessage(ServerMessage(this.runId, ServerMessageType.COMPETITION_START))
        logger.info("SynchronousRunManager ${this.runId} started")
    }

    override fun terminate() = this.stateLock.write {
        if (this.status != RunManagerStatus.ACTIVE) throw IllegalStateException("SynchronizedRunManager is in status ${this.status} and cannot be terminated.")
        this.run.end()
        this.status = RunManagerStatus.TERMINATED
        this.executor.broadcastWsMessage(ServerMessage(this.runId, ServerMessageType.COMPETITION_END))
        logger.info("SynchronousRunManager ${this.runId} terminated")
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
        check(this.status == RunManagerStatus.ACTIVE) { "SynchronizedRunManager is in status ${this.status}. Tasks can therefore not be changed." }
        val newIndex = this.competitionDescription.tasks.indexOf(this.currentTask) + 1
        return try {
            this.goToTask(newIndex)
            true
        } catch (e: IndexOutOfBoundsException) {
            false
        }
    }

    override fun goToTask(index: Int) = this.stateLock.write {
        check(this.status == RunManagerStatus.ACTIVE) { "SynchronizedRunManager is in status ${this.status}. Tasks can therefore not be changed." }
        if (index >= 0 && index < this.competitionDescription.tasks.size) {
            this.currentTask = this.competitionDescription.tasks[index]
            this.executor.broadcastWsMessage(ServerMessage(this.runId, ServerMessageType.COMPETITION_UPDATE))
            logger.info("SynchronousRunManager ${this.runId} set to task $index")
        } else {
            throw IndexOutOfBoundsException("Index $index is out of bounds for the number of available tasks.")
        }
    }

    override fun startTask() = this.stateLock.write {
        check(this.status == RunManagerStatus.ACTIVE) { "SynchronizedRunManager is in status ${this.status}. Tasks can therefore not be started." }

        /* Create and prepare pipeline for submission. */
        val ret = this.run.newTaskRun(this.competitionDescription.tasks.indexOf(this.currentTask))
        val pipeline = Triple(ret.task.newFilter(), ret.task.newValidator(), ret.task.newScorer())
        (this.submissionPipeline as MutableList).add(pipeline)

        /* Update competition run. */
        this.dao.update(this.run)

        /* Update status. */
        this.status = RunManagerStatus.PREPARING_TASK
        this.ackCounter = 0
        this.executor.broadcastWsMessage(this.runId, ServerMessage(this.runId, ServerMessageType.TASK_PREPARE))
        logger.info("SynchronousRunManager ${this.runId} started task task ${this.currentTask}")
    }

    override fun abortTask() = this.stateLock.write {
        if (!(this.status == RunManagerStatus.PREPARING_TASK || this.status == RunManagerStatus.RUNNING_TASK)) {
            throw IllegalStateException("SynchronizedRunManager is in status ${this.status}. Tasks can therefore not be aborted.")
        }

        /**  End TaskRun and persist. */
        if (this.status == RunManagerStatus.RUNNING_TASK) {
            this.run.currentTask?.end()
            this.dao.update(this.run)
        }

        /** Update state. */
        this.status = RunManagerStatus.ACTIVE
        this.ackCounter = -1
        this.executor.broadcastWsMessage(this.runId, ServerMessage(this.runId, ServerMessageType.TASK_END))
        logger.info("SynchronousRunManager ${this.runId} aborted task task ${this.currentTask}")
    }

    override fun timeLeft(): Long = this.stateLock.read {
        if (this.status == RunManagerStatus.RUNNING_TASK) {
            return (this.run.currentTask!!.task.duration * 1000L - (System.currentTimeMillis() - this.run.currentTask!!.started!!))
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
        this.run.currentTask!!.addSubmission(sub)

        /* Inform clients about update. */
        this.executor.broadcastWsMessage(ServerMessage(this.runId, ServerMessageType.TASK_UPDATED))
        return sub.status
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

            /** Yield to other threads. */
            Thread.onSpinWait()
        }

        /** Handles the activity part of the SynchronizedRunManager (status = ACTIVE).*/
        while (this.status == RunManagerStatus.ACTIVE || this.status == RunManagerStatus.PREPARING_TASK || this.status == RunManagerStatus.RUNNING_TASK) {

            /** Handles the preparation period of the SynchronizedRunManager (status = PREPARING_TASK). */
            try {
                while (this.status == RunManagerStatus.PREPARING_TASK) {
                    if (this.ackCounter >= this.clientCounter) {
                        this.stateLock.write {
                            this.run.currentTask?.start()
                            this.status = RunManagerStatus.RUNNING_TASK
                        }
                        this.executor.broadcastWsMessage(ServerMessage(this.runId, ServerMessageType.TASK_START))
                        break
                    }

                    /** Update scoreboards on each iteration. */
                    this.scoreboards.forEach { it.update(this.run.runs) }
                    logger.debug("SynchronousRunManager ${this.runId} updated scoreboards while preparing for a task")

                    /** Sleep for 100ms. */
                    Thread.sleep(100)

                    Thread.onSpinWait()
                }
            } catch (e: Exception) {
                logger.error("Uncaught exception during task preparation", e)
            }

            /** Handles the task execution period of the SynchronizedRunManager (status = RUNNING_TASK). */
            try {
                while (this.status == RunManagerStatus.RUNNING_TASK) {
                    val timeLeft = this.timeLeft()
                    if (timeLeft <= 0) {
                        this.stateLock.write {
                            this.run.currentTask?.end()
                            this.status = RunManagerStatus.ACTIVE
                        }
                        this.dao.update(this.run)
                        this.executor.broadcastWsMessage(ServerMessage(this.runId, ServerMessageType.TASK_END))
                        break
                    }

                    /** Update scoreboards on each iteration. */
                    this.scoreboards.forEach { it.update(this.run.runs) }
                    logger.debug("SynchronousRunManager ${this.runId} updated scoreboards for running task")

                    /** Sleep for 100ms. */
                    Thread.sleep(100)
                }
            } catch (e: Exception) {
                logger.error("Uncaught exception during task run", e)
            }

            /** Update scoreboards on each iteration. */

            try {
                this.scoreboards.forEach { it.update(this.run.runs) }
                logger.debug("SynchronousRunManager ${this.runId} updated scoreboards while no task was running")
            }catch (e: Exception) {
                logger.error("Uncaught exception during scoreboard update between tasks", e)
            }

            /** Sleep for 100ms. */
            Thread.sleep(100)

            /** Yield to other threads. */
            Thread.onSpinWait()
        }
        logger.info("SynchronousRunManager ${this.runId} reached end of run logic")
    }
}