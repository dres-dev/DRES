package dev.dres.run

import dev.dres.api.rest.types.ViewerInfo
import dev.dres.api.rest.types.evaluation.submission.ApiClientSubmission
import dev.dres.api.rest.types.evaluation.submission.ApiSubmission
import dev.dres.data.model.run.*
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
        TODO("Not yet implemented")
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
     *
     */
    override fun postSubmission(context: RunActionContext, submission: ApiClientSubmission) : ApiSubmission {


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