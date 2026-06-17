package dev.dres.run

import dev.dres.api.rest.types.ViewerInfo
import dev.dres.api.rest.types.evaluation.submission.ApiClientSubmission
import dev.dres.api.rest.types.evaluation.submission.ApiSubmission
import dev.dres.data.model.run.*
import dev.dres.data.model.run.interfaces.TaskId
import dev.dres.data.model.submissions.DbAnswerSet
import dev.dres.data.model.template.team.TeamId
import dev.dres.run.score.scoreboard.Scoreboard
import dev.dres.run.validation.interfaces.JudgementValidator
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.asSequence
import kotlinx.dnq.query.isEmpty
import org.slf4j.LoggerFactory
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read

class NonInteractiveRunManager(
    override val evaluation: NonInteractiveEvaluation,
    override val store: TransientEntityStore
) : RunManager {

    private val LOGGER = LoggerFactory.getLogger(this.javaClass)

    /** Generates and returns [ApiRunProperties] for this [InteractiveAsynchronousRunManager]. */
    override val runProperties: ApiRunProperties
        get() = ApiRunProperties(
            this.evaluation.participantCanView,
            false,
            this.evaluation.allowRepeatedTasks,
            this.evaluation.limitSubmissionPreviews
        )

    /** A lock for state changes to this [InteractiveSynchronousRunManager]. */
    private val stateLock = ReentrantReadWriteLock()

    /** Run ID of this [InteractiveSynchronousRunManager]. */
    override val id: TaskId
        get() = this.evaluation.id

    /** Name of this [InteractiveSynchronousRunManager]. */
    override val name: String
        get() = this.evaluation.name

    /** The [ApiEvaluationTemplate] executed by this [InteractiveSynchronousRunManager]. */
    override val template = this.evaluation.template

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
        get() = this.evaluation.taskRuns.map { it.validator }.filterIsInstance(JudgementValidator::class.java)

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

    override fun updateProperties(properties: ApiRunProperties) {
        store.transactional {
            this.evaluation.participantCanView = properties.participantCanView
            this.evaluation.allowRepeatedTasks = properties.allowRepeatedTasks
            this.evaluation.limitSubmissionPreviews = properties.limitSubmissionPreviews
        }
    }

    override fun taskCount(context: RunActionContext): Int = this.evaluation.taskRuns.size

    private val viewerMap: MutableMap<ViewerInfo, Boolean> = mutableMapOf()

    override fun viewers(): Map<ViewerInfo, Boolean> = viewerMap

//    override fun wsMessageReceived(connection: WebSocketConnection, message: ClientMessage): Boolean {
//        when (message.type) {
//            ClientMessageType.REGISTER -> this.viewerMap[connection] = true
//            ClientMessageType.UNREGISTER -> this.viewerMap.remove(connection)
//            ClientMessageType.ACK, ClientMessageType.PING -> {} //nop
//        }
//        return true
//    }

    override fun viewerPreparing(
        taskTemplateId: dev.dres.data.model.run.TaskId,
        rac: RunActionContext,
        viewerInfo: ViewerInfo
    ) {
        /* nop */
    }

    override fun viewerReady(taskTemplateId: dev.dres.data.model.run.TaskId, rac: RunActionContext, viewerInfo: ViewerInfo) {
        /* nop */
    }

    override fun run() {

        while (this.status != RunManagerStatus.TERMINATED) {

            try {
                this.stateLock.read {

                }
            } catch (ie: InterruptedException) {
                LOGGER.info("Interrupted NonInteractiveRunManager, exiting")
                return
            }

            Thread.sleep(1000)
        }

        LOGGER.info("NonInteractiveRunManager ${this.id} reached end of run logic.")

    }


    /**
     *
     */
    override fun tasks(context: RunActionContext): List<AbstractNonInteractiveTask> = this.evaluation.taskRuns

    private val taskMap = this.evaluation.taskRuns.associateBy { it.taskId }

    /**
     * Posts a [ApiClientSubmission] to this [NonInteractiveRunManager].
     *
     * Each answer set in the submission must reference a valid task ID for this evaluation.
     * Answer sets are grouped per task, transformed and filtered by that task's rules,
     * then persisted as a single [DbSubmission] and validated.
     */
    override fun postSubmission(context: RunActionContext, submission: ApiClientSubmission): ApiSubmission {

        /* Phase 1: Validate, transform and filter (read-only). */
        val transformedSubmission = this.stateLock.read {
            this.store.transactional(true) {

                /* Attach context to submission. */
                submission.userId = context.userId
                submission.teamId = resolveTeamId(context)

                /* Reject submission if any answer set references an unknown task. */
                val unknownTaskIds = submission.answerSets.mapNotNull { it.taskId }.filter { !taskMap.containsKey(it) }
                require(unknownTaskIds.isEmpty()) { "Submission references unknown task ID(s): $unknownTaskIds" }

                /* Per task: transform then filter the answer sets that belong to it. */
                val processedAnswerSets = submission.answerSets
                    .groupBy { it.taskId }
                    .flatMap { (taskId, answerSets) ->
                        val task = taskMap[taskId] ?: throw IllegalArgumentException("Unknown task $taskId")
                        val taskSubmission = submission.copy(answerSets = answerSets)
                        val transformed = task.transformer.transform(taskSubmission)
                        task.filter.acceptOrThrow(transformed)
                        transformed.answerSets
                    }

                submission.copy(answerSets = processedAnswerSets)
            }
        }

        /* Phase 2: Persist and validate (write). */
        return this.store.transactional {
            val db = transformedSubmission.toNewDb()

            check(!db.answerSets.isEmpty) { "Submission contains no valid answer sets after transformation." }

            db.answerSets.asSequence().forEach { answerSet: DbAnswerSet ->
                val task = taskMap[answerSet.task.taskId]
                    ?: throw IllegalArgumentException("Unknown task ${answerSet.task.taskId}")
                task.validator.validate(answerSet)
            }

            db.toApi()
        }
    }

    override fun reScore(taskId: TaskId) {
        taskMap[taskId]?.scorer?.invalidate()
    }

    /** Resolves the [TeamId] for the user in the given [RunActionContext]. */
    private fun resolveTeamId(context: RunActionContext): TeamId =
        this.template.teams.firstOrNull { team -> team.users.any { it.id == context.userId } }?.teamId
            ?: throw IllegalArgumentException("Could not find a matching team for user ${context.userId}.")
}