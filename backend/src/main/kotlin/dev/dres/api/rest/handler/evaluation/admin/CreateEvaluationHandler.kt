package dev.dres.api.rest.handler.evaluation.admin

import dev.dres.DRES
import dev.dres.api.rest.handler.PostRestHandler
import dev.dres.api.rest.types.template.ApiEvaluationStartMessage
import dev.dres.api.rest.types.evaluation.ApiEvaluationType
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.api.rest.types.status.SuccessStatus
import dev.dres.data.model.run.DbEvaluation
import dev.dres.data.model.template.DbEvaluationTemplate
import dev.dres.mgmt.cache.CacheManager
import dev.dres.run.InteractiveSynchronousRunManager
import dev.dres.run.RunExecutor
import dev.dres.run.RunManagerStatus
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.http.bodyAsClass
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.and
import kotlinx.dnq.query.eq
import kotlinx.dnq.query.firstOrNull
import kotlinx.dnq.query.query
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.util.concurrent.TimeUnit

/**
 * [PostRestHandler] to create an [DbEvaluation].
 *
 * @author Ralph Gasser
 * @author Luca Rossetto
 * @author Loris Sauter
 * @version 2.0.0
 */
class CreateEvaluationHandler(private val store: TransientEntityStore, private val cache: CacheManager) : AbstractEvaluationAdminHandler(), PostRestHandler<SuccessStatus> {

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
        val manager = this.store.transactional {
            val template = DbEvaluationTemplate.query((DbEvaluationTemplate::id eq message.templateId) and (DbEvaluationTemplate::instance eq false)).firstOrNull()
                ?: throw ErrorStatusException(404, "Evaluation template with ID ${message.templateId} could not be found.'", ctx)

            /* ensure that only one synchronous run of an evaluation is happening at any given time */
            if (message.type == ApiEvaluationType.SYNCHRONOUS && RunExecutor.managers().any {
                    it is InteractiveSynchronousRunManager && it.template.id == template.id && it.status != RunManagerStatus.TERMINATED
                }
            ) {
                throw ErrorStatusException(400, "Synchronous run of evaluation template ${template.name} already exists.", ctx)
            }

            /* Check and prepare videos */
            val segmentTasks = template.getAllVideos()
            val await = segmentTasks.map {source ->

                when(source) {
                    is DbEvaluationTemplate.VideoSource.ItemSource -> {
                        val item = source.item
                        val path = item.pathToOriginal()
                        if (!Files.exists(path)) {
                            logger.error("Required media file $path not found for item ${item.name}.")
                            throw ErrorStatusException(500, "Required media file $path not found for item ${item.name}.", ctx)
                        }

                        this@CreateEvaluationHandler.cache.asyncPreviewVideo(item, source.range.start.toMilliseconds(), source.range.end.toMilliseconds())
                    }
                    is DbEvaluationTemplate.VideoSource.PathSource -> {
                        val path = DRES.EXTERNAL_ROOT.resolve(source.path)
                        if (!Files.exists(path)) {
                            logger.error("ERROR: Media file $path not found for external video.")
                            throw ErrorStatusException(500, "Required media file $path not found.", ctx)
                        }

                        this@CreateEvaluationHandler.cache.asyncPreviewVideo(path, source.range.start.toMilliseconds(), source.range.end.toMilliseconds())
                    }
                }
            }
            await.all {
                try {
                    it.get(60, TimeUnit.SECONDS)
                    true
                 } catch (e: Throwable) {
                    throw ErrorStatusException(500, "Required media file could not be prepared.", ctx)
                 }
            }

            /* Prepare evaluation. */
            val evaluation = DbEvaluation.new {
                this.name = message.name
                this.template = template.toInstance()
                this.type = message.type.toDb()
                this.allowRepeatedTasks = message.properties.allowRepeatedTasks
                this.participantCanView = message.properties.participantCanView
                this.shuffleTasks = message.properties.shuffleTasks
                this.limitSubmissionPreviews = message.properties.limitSubmissionPreviews
                initPermutation()
            }

            /* Create evaluation + run manager and end transaction. */
            evaluation.toRunManager(this.store)
        }

        /* Schedule newly created run manager. IMPORTANT: This MUST take place outside the previous transaction context. */
        RunExecutor.schedule(manager)
        return SuccessStatus("Evaluation '${message.name}' was started and is running with ID ${manager.id}.")
    }
}
