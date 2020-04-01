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

class DistributedRunManager(competition: Competition, name: String, private val executor: RunExecutor, private val dao: DAO<CompetitionRun>) : RunManager {

    /** The [CompetitionRun] capturing the state of this [DistributedRunManager]. */
    private val run = CompetitionRun(-1, name, competition)

    /** Run ID of this [DistributedRunManager]. */
    override val runId: Long
        get() = this.run.id

    /** Name of this [DistributedRunManager]. */
    override val name: String
        get() = this.run.name

    /** The [Competition] executed by this [DistributedRunManager]. */
    override val competition: Competition
        get() = this.run.competition

    /** The status of this [RunManager]. */
    @Volatile
    override var status: RunManagerStatus = RunManagerStatus.CREATED
        get() = this.stateLock.read {
            return field
        }
        private set

    /** Currently active task. */
    override var currentTask: Task = this.competition.tasks.first()
        private set

    /** A lock for state changes to this [DistributedRunManager]. */
    private val stateLock = ReentrantReadWriteLock()

    /** */
    @Volatile
    private var clientCounter = 0

    /** */
    @Volatile
    private var ackCounter = -1


    init {
        this.run.id = this.dao.append(this.run)
    }

    override val scoreboards: List<Scoreboard>
        get() = TODO("Not yet implemented")

    /**
     *
     */
    override fun start(): Boolean = this.stateLock.write {
        if (this.status != RunManagerStatus.CREATED) throw IllegalStateException("DistributedRunManager is in status ${this.status} and cannot be started.")
        this.run.start()
        this.status = RunManagerStatus.ACTIVE
        this.executor.broadcastWsMessage(ServerMessage(this.runId, ServerMessageType.COMPETITION_START))
        return true
    }

    /**
     *
     */
    override fun terminate() = this.stateLock.write {
        if (this.status != RunManagerStatus.ACTIVE) throw IllegalStateException("DistributedRunManager is in status ${this.status} and cannot be terminated.")
        this.run.end()
        this.status = RunManagerStatus.TERMINATED
        this.executor.broadcastWsMessage(ServerMessage(this.runId, ServerMessageType.COMPETITION_END))
    }

    override fun previousTask() = this.stateLock.write {
        val newIndex = this.competition.tasks.indexOf(this.currentTask) - 1
        this.goToTask(newIndex)
    }

    override fun nextTask() = this.stateLock.write {
        val newIndex = this.competition.tasks.indexOf(this.currentTask) + 1
        this.goToTask(newIndex)
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

        /* Creates a new TaskRun; the run is not started until the PREPARE phase has completed. */
        this.run.TaskRun(this.competition.tasks.indexOf(this.currentTask))

        /* Update status. */
        this.status = RunManagerStatus.PREPARING_TASK
        this.ackCounter = 0
        this.executor.broadcastWsMessage(this.runId, ServerMessage(this.runId, ServerMessageType.TASK_PREPARE))
    }

    override fun abortTask() = this.stateLock.write {
        if (this.status != RunManagerStatus.PREPARING_TASK && this.status != RunManagerStatus.RUNNING_TASK) throw IllegalStateException("DistributedRunManager is in status ${this.status}. Tasks can therefore not be aborted.")

        /**  End TaskRun. */
        this.run.currentTask?.end()

        this.status = RunManagerStatus.ACTIVE
        this.ackCounter = -1
        this.executor.broadcastWsMessage(this.runId, ServerMessage(this.runId, ServerMessageType.TASK_END))
    }

    override fun timeElapsed(): Long = this.stateLock.write {
        TODO("Not yet implemented")
    }



    /**
     * Processes WebSocket message received by the [RunExecutor].
     */
    override fun wsMessageReceived(message: ClientMessage) = this.stateLock.read {
        when (message.type) {
            ClientMessageType.ACK -> {
                if (this.status == RunManagerStatus.PREPARING_TASK) {
                    this.ackCounter += 1
                }
            }
            ClientMessageType.REGISTER -> this.clientCounter += 1
            ClientMessageType.UNREGISTER -> this.clientCounter -= 1
        }
    }

    /**
     * Processes [Submission]s received by ...
     */
    override fun postSubmission(sub: Submission) = this.stateLock.read {
        if (this.status == RunManagerStatus.RUNNING_TASK) {

            /* Inform clients about update. */
            this.executor.broadcastWsMessage(ServerMessage(this.runId, ServerMessageType.TASK_UPDATED))
        }
    }

    override fun run() {
        /** */
        loop@while (true) {
            if (this.status != RunManagerStatus.CREATED) {
                break@loop
            }
            Thread.onSpinWait()
        }

        /** Handles the activity part of the DistributedRunManager.*/
        while (this.status == RunManagerStatus.ACTIVE) {
            /** Handles the preparation period of the DistributedRunManager. */
            while (this.status == RunManagerStatus.PREPARING_TASK) {
                if (this.ackCounter >= this.clientCounter) {
                    this.stateLock.write {
                        this.status = RunManagerStatus.RUNNING_TASK
                        this.run.currentTask?.start()
                    }
                    this.executor.broadcastWsMessage(ServerMessage(this.runId, ServerMessageType.TASK_START))
                    break
                }
                Thread.onSpinWait()
            }

            /** Handles the task execution period of the DistributedRunManager. */
            while (this.status == RunManagerStatus.RUNNING_TASK) {
                TODO()
            }

            Thread.onSpinWait()
        }
    }
}