package dev.dres.run

import dev.dres.api.rest.types.WebSocketConnection
import dev.dres.api.rest.types.evaluation.ApiSubmission
import dev.dres.api.rest.types.evaluation.websocket.ClientMessage
import dev.dres.api.rest.types.evaluation.websocket.ClientMessageType
import dev.dres.data.model.run.*
import dev.dres.data.model.template.DbEvaluationTemplate
import dev.dres.data.model.run.interfaces.TaskId
import dev.dres.data.model.submissions.*
import dev.dres.data.model.template.team.TeamId
import dev.dres.run.score.scoreboard.Scoreboard
import dev.dres.run.updatables.ScoreboardsUpdatable
import dev.dres.run.validation.interfaces.JudgementValidator
import jetbrains.exodus.database.TransientEntityStore
import org.slf4j.LoggerFactory
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read

class NonInteractiveRunManager(override val evaluation: NonInteractiveEvaluation, override val store: TransientEntityStore) : RunManager {

    private val SCOREBOARD_UPDATE_INTERVAL_MS = 10_000L // TODO make configurable

    private val LOGGER = LoggerFactory.getLogger(this.javaClass)

    /** Generates and returns [RunProperties] for this [InteractiveAsynchronousRunManager]. */
    override val runProperties: RunProperties
        get() = RunProperties(this.evaluation.participantCanView, false, this.evaluation.allowRepeatedTasks, this.evaluation.limitSubmissionPreviews)

    /** A lock for state changes to this [InteractiveSynchronousRunManager]. */
    private val stateLock = ReentrantReadWriteLock()

    /** Run ID of this [InteractiveSynchronousRunManager]. */
    override val id: TaskId
        get() = this.evaluation.id

    /** Name of this [InteractiveSynchronousRunManager]. */
    override val name: String
        get() = this.evaluation.name

    /** The [DbEvaluationTemplate] executed by this [InteractiveSynchronousRunManager]. */
    override val template: DbEvaluationTemplate
        get() = this.evaluation.description

    /** The internal [ScoreboardsUpdatable] instance for this [InteractiveSynchronousRunManager]. */
    private val scoreboardsUpdatable = ScoreboardsUpdatable(this, SCOREBOARD_UPDATE_INTERVAL_MS) //TODO requires some changes

    /** The [List] of [Scoreboard]s maintained by this [NonInteractiveRunManager]. */
    override val scoreboards: List<Scoreboard>
        get() = this.evaluation.scoreboards

    @Volatile
    override var status: RunManagerStatus = if (this.evaluation.hasStarted) {
        RunManagerStatus.ACTIVE
    } else {
        RunManagerStatus.CREATED
    }
    private set

    /** */
    override val judgementValidators: List<JudgementValidator>
        get() = this.evaluation.tasks.map { it.validator }.filterIsInstance(JudgementValidator::class.java)

    override fun start(context: RunActionContext) {
        check(this.status == RunManagerStatus.CREATED) { "NonInteractiveRunManager is in status ${this.status} and cannot be started." }
        if (!context.isAdmin)
            throw IllegalAccessError("functionality of NonInteractiveRunManager only available to administrators")

        /* Start the run. */
        this.evaluation.start()

        /* Update status. */
        this.status = RunManagerStatus.ACTIVE

        LOGGER.info("NonInteractiveRunManager ${this.id} started")
    }

    override fun end(context: RunActionContext) {
        check(this.status != RunManagerStatus.TERMINATED) { "NonInteractiveRunManager is in status ${this.status} and cannot be terminated." }
        if (!context.isAdmin)
            throw IllegalAccessError("functionality of NonInteractiveRunManager only available to administrators")

        /* End the run. */
        this.evaluation.end()

        /* Update status. */
        this.status = RunManagerStatus.TERMINATED

        LOGGER.info("SynchronousRunManager ${this.id} terminated")
    }

    override fun updateProperties(properties: RunProperties) {
        TODO("Not yet implemented")
    }

    override fun taskCount(context: RunActionContext): Int = this.evaluation.tasks.size

    private val viewerMap: MutableMap<WebSocketConnection, Boolean> = mutableMapOf()

    override fun viewers(): Map<WebSocketConnection, Boolean> = viewerMap

    override fun wsMessageReceived(connection: WebSocketConnection, message: ClientMessage): Boolean {
        when (message.type) {
            ClientMessageType.REGISTER -> this.viewerMap[connection] = true
            ClientMessageType.UNREGISTER -> this.viewerMap.remove(connection)
            ClientMessageType.ACK, ClientMessageType.PING -> {} //nop
        }
        return true
    }


    override fun run() {

        while (this.status != RunManagerStatus.TERMINATED) {

            try {
                this.stateLock.read {
                    while (updatedTasks.isNotEmpty()) {
                        val idNamePair = updatedTasks.poll(1, TimeUnit.SECONDS)

                        if (idNamePair == null) {
                            LOGGER.error("Unable to retrieve task id from queue despite it being indicated not to be empty")
                            break
                        }

                        val task = this.evaluation.tasks.find { it.taskId == idNamePair.first }

                        if (task == null) {
                            LOGGER.error("Unable to retrieve task with changed id ${idNamePair.first}")
                            break
                        }


                        /* TODO: Redo. */
                        /*val batches = idNamePair.second.mapNotNull { task.submissions[it] }

                        val validator = task.validator
                        val scorer = task.scorer

                        batches.forEach {
                            validator.validate(it)
                            scorer.computeScores(it)
                        }*/
                        scoreboardsUpdatable.update(this.status)
                    }
                }
            } catch (ie: InterruptedException) {
                LOGGER.info("Interrupted NonInteractiveRunManager, exiting")
                return
            }


            Thread.sleep(100)

        }

        LOGGER.info("NonInteractiveRunManager ${this.id} reached end of run logic.")

    }

    private val updatedTasks = LinkedBlockingQueue<Pair<TaskId, List<Pair<TeamId, String>>>>()

    /**
     *
     */
    override fun tasks(context: RunActionContext): List<AbstractNonInteractiveTask> = this.evaluation.tasks

    override fun postSubmission(context: RunActionContext, submission: ApiSubmission): VerdictStatus {
        TODO("Not yet implemented")
    }
}