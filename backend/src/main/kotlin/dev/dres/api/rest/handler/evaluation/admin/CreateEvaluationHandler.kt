package dev.dres.api.rest.handler.evaluation.admin

import dev.dres.api.rest.handler.PostRestHandler
import dev.dres.api.rest.types.competition.ApiEvaluationStartMessage
import dev.dres.api.rest.types.evaluation.ApiEvaluationType
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.api.rest.types.status.SuccessStatus
import dev.dres.data.model.Config
import dev.dres.data.model.run.DbEvaluation
import dev.dres.data.model.run.InteractiveAsynchronousEvaluation
import dev.dres.data.model.template.DbEvaluationTemplate
import dev.dres.data.model.run.InteractiveSynchronousEvaluation
import dev.dres.run.InteractiveSynchronousRunManager
import dev.dres.run.RunExecutor
import dev.dres.run.RunManagerStatus
import dev.dres.utilities.FFmpegUtil
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.http.bodyAsClass
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.eq
import kotlinx.dnq.query.firstOrNull
import kotlinx.dnq.query.query
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * [PostRestHandler] to create an [DbEvaluation].
 *
 * @author Ralph Gasser
 * @author Luca Rossetto
 * @author Loris Sauter
 * @version 2.0.0
 */
class CreateEvaluationHandler(store: TransientEntityStore, config: Config) : AbstractEvaluationAdminHandler(store), PostRestHandler<SuccessStatus> {

    /** The [Path] to preview cache location. */
    private val cacheLocation: Path = Paths.get(config.cachePath, "tasks")

    private val logger = LoggerFactory.getLogger(this.javaClass)

    override val route = "evaluation/admin/create"

    @OpenApi(
        summary = "Creates a new evaluation run from an existing evaluation template. This is a method for administrators.",
        path = "/api/v2/evaluation/admin/create",
        methods = [HttpMethod.POST],
        operationId = OpenApiOperation.AUTO_GENERATE,
        requestBody = OpenApiRequestBody([OpenApiContent(ApiEvaluationStartMessage::class)]),
        tags = ["Evaluation Administrator"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
            OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)])
        ]
    )
    override fun doPost(ctx: Context): SuccessStatus {
        val message = try {
            ctx.bodyAsClass<ApiEvaluationStartMessage>()
        } catch (e: BadRequestResponse) {
            throw ErrorStatusException(400, "Invalid parameters. This is a programmers error!", ctx)
        }

        /* Prepare run manager. */
        val evaluationId = this.store.transactional { tx ->
            val template = DbEvaluationTemplate.query(DbEvaluationTemplate::id eq message.templateId).firstOrNull()
                ?: throw ErrorStatusException(404, "Competition with ID ${message.templateId} not found.'", ctx)
            /* ensure that only one synchronous run of a competition is happening at any given time */

            if (message.type == ApiEvaluationType.SYNCHRONOUS && RunExecutor.managers().any {
                    it is InteractiveSynchronousRunManager && it.template == template && it.status != RunManagerStatus.TERMINATED
                }
            ) {
                throw ErrorStatusException(400, "Synchronous run of evaluation template ${template.name} already exists.", ctx)
            }

            /* Check and prepare videos */
            val segmentTasks = template.getAllVideos()
            segmentTasks.forEach {
                val item = it.first
                val path = item.pathToOriginal()
                if (!Files.exists(path)) {
                    logger.error("Required media file $path not found for item ${item.name}.")
                    return@forEach
                }

                val cacheName = item.cachedItemName(it.second.start.toMilliseconds(), it.second.end.toMilliseconds())
                val cachePath = this.cacheLocation.resolve(cacheName)
                if (!Files.exists(cachePath)) {
                    logger.warn("Query video file for item ${item.name} not found; rendering to $cachePath")
                    FFmpegUtil.extractSegment(item, it.second, cachePath)
                }
            }

            /* Prepare evaluation. */
            val evaluation = DbEvaluation.new {
                this.name = message.name
                this.template = template /* TODO: Create copy. */
                this.type = message.type.toDb()
                this.allowRepeatedTasks = message.properties.allowRepeatedTasks
                this.participantCanView = message.properties.participantCanView
                this.shuffleTasks = message.properties.shuffleTasks
                this.limitSubmissionPreviews = message.properties.limitSubmissionPreviews
            }

            /* Try to flush change prior to scheduling it. */
            if (!tx.flush()) {
                throw ErrorStatusException(500, "Failed to store new evaluation.", ctx)
            }
            RunExecutor.schedule(when (message.type) {
                ApiEvaluationType.ASYNCHRONOUS -> InteractiveAsynchronousEvaluation(evaluation, emptyMap()) /* TODO: Team map */
                ApiEvaluationType.SYNCHRONOUS -> InteractiveSynchronousEvaluation(evaluation)
                ApiEvaluationType.NON_INTERACTIVE -> TODO()
            }, this.store)
            evaluation.id
        }

        /* Schedule newly created run manager. */
        return SuccessStatus("Evaluation '${message.name}' was started and is running with ID $evaluationId.")
    }
}
