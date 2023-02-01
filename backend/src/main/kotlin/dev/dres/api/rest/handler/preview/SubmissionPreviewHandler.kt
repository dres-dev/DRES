package dev.dres.api.rest.handler.preview

import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.data.model.Config
import dev.dres.data.model.media.MediaItem
import dev.dres.data.model.run.RunActionContext
import dev.dres.run.InteractiveRunManager
import dev.dres.run.RunExecutor
import dev.dres.utilities.extensions.errorResponse
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.firstOrNull

/**
 * An [AbstractPreviewHandler] used to access previews of [MediaItem]s based on a specific [Submission].
 *
 * @author Luca Rossetto
 * @author Loris Sauter
 * @version 2.0.0
 */
class SubmissionPreviewHandler(store: TransientEntityStore, config: Config) : AbstractPreviewHandler(store, config) {
    override val route: String = "preview/submission/{evaluationId}/{submissionId}"

    @OpenApi(
        summary = "Returns a preview image for a specific submission.",
        path = "/api/v2/preview/submission/{evaluationId}/{submissionId}",
        pathParams = [
            OpenApiParam("evaluationId", String::class, "The evaluation ID."),
            OpenApiParam("submissionId", String::class, "The submission ID")
        ],
        tags = ["Media"],
        responses = [OpenApiResponse(
            "200",
            [OpenApiContent(type = "image/png")]
        ), OpenApiResponse("401"), OpenApiResponse("400")],
        ignore = true,
        methods = [HttpMethod.GET]
    )
    override fun get(ctx: Context) {
        try {
            val params = ctx.pathParamMap()
            val runId = params["evaluationId"] ?: throw ErrorStatusException(404, "Parameter 'evaluationId' is invalid", ctx)
            val submissionId = params["submissionId"] ?: throw ErrorStatusException(404, "Parameter 'submissionId' is missing", ctx)
            val run = RunExecutor.managerForId(runId) ?: throw ErrorStatusException(404, "Competition run $runId not found", ctx)
            if (run !is InteractiveRunManager) throw ErrorStatusException(404, "Competition Run $runId is not interactive", ctx)

            /* TODO: Make this work for batched submissions ? */
            this.store.transactional (true) {
                val rac = RunActionContext.runActionContext(ctx, run)
                val submission = run.allSubmissions(rac).find { it.id == submissionId }
                    ?: throw ErrorStatusException(404, "Submission '$submissionId' not found", ctx)
                val verdict = submission.verdicts.firstOrNull()
                    ?: throw ErrorStatusException(404, "Submission '$submissionId' not found", ctx)

                when {
                    verdict.item != null -> {
                        handlePreviewRequest(verdict.item!!, if (verdict.start != null) verdict.start else null, ctx)
                    }
                    verdict.text != null -> {
                        ctx.header("Cache-Control", "max-age=31622400")
                        ctx.contentType("image/png")
                        ctx.result(this.javaClass.getResourceAsStream("/img/text.png")!!)
                    }
                    else -> {
                        ctx.header("Cache-Control", "max-age=31622400")
                        ctx.contentType("image/png")
                        ctx.result(this.javaClass.getResourceAsStream("/img/missing.png")!!)
                    }
                }
            }
        } catch (e: ErrorStatusException) {
            ctx.errorResponse(e)
        }
    }

    override fun doGet(ctx: Context): Any {
        throw UnsupportedOperationException("SubmissionPreviewHandler::doGet() is not supported and should not be executed!")
    }
}