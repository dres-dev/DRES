package dev.dres.run

import dev.dres.api.rest.types.ViewerInfo
import dev.dres.api.rest.types.evaluation.submission.ApiClientSubmission
import dev.dres.data.model.run.*
import dev.dres.data.model.template.DbEvaluationTemplate
import dev.dres.data.model.run.interfaces.TaskId
import dev.dres.run.score.scoreboard.Scoreboard
import dev.dres.run.validation.interfaces.JudgementValidator
import jetbrains.exodus.database.TransientEntityStore
import org.slf4j.LoggerFactory
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read

class NonInteractiveRunManager(
    override val evaluation: NonInteractiveEvaluation,
    override val store: TransientEntityStore
) : RunManager {

    private val LOGGER = LoggerFactory.getLogger(this.javaClass)

    /** Generates and returns [RunProperties] for this [InteractiveAsynchronousRunManager]. */
    override val runProperties: RunProperties
        get() = RunProperties(
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

    /** The [DbEvaluationTemplate] executed by this [InteractiveSynchronousRunManager]. */
    override val template: DbEvaluationTemplate
        get() = this.evaluation.description

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
        taskId: dev.dres.data.model.run.TaskId,
        rac: RunActionContext,
        viewerInfo: ViewerInfo
    ) {
        /* nop */
    }

    override fun viewerReady(taskId: dev.dres.data.model.run.TaskId, rac: RunActionContext, viewerInfo: ViewerInfo) {
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
    override fun tasks(context: RunActionContext): List<AbstractNonInteractiveTask> = this.evaluation.tasks

    private val taskMap = this.evaluation.tasks.associateBy { it.taskId }

    /**
     *
     */
    override fun postSubmission(context: RunActionContext, submission: ApiClientSubmission) {


        TODO("Not yet implemented")

        /*val submissionByTask =
            submission.answers.groupBy { it.taskId }.mapValues { submission.copy(answers = it.value) }

        if (submissionByTask.keys.any { !taskMap.containsKey(it) }) {
            throw IllegalStateException("Unknown task")
        }

        this.stateLock.write {

            val errorBuffer = StringBuilder()

            submissionByTask.forEach { (taskId, submission) ->

                val task = taskMap[taskId] ?: throw IllegalStateException("Unknown task $taskId")

                try {

                    /* Check if ApiSubmission meets formal requirements. */
                    task.filter.acceptOrThrow(submission)

                    /* Apply transformations to submissions */
                    val transformedSubmission = task.transformer.transform(submission)

                    /* Check if there are answers left after transformation */
                    if (transformedSubmission.answers.isEmpty()) {
                        return@forEach
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

                } catch (e: SubmissionRejectedException) {
                    errorBuffer.append(e.message)
                    errorBuffer.append('\n')
                }
            }

            if (errorBuffer.isNotBlank()) {
                throw SubmissionRejectedException(submission, errorBuffer.toString())
            }

        } */
    }

    override fun reScore(taskId: TaskId) {
        TODO("Not yet implemented")
    }
}