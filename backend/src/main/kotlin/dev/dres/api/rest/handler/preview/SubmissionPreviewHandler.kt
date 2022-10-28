package dev.dres.api.rest.handler.preview

import dev.dres.api.rest.handler.AbstractPreviewHandler
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.data.model.Config
import dev.dres.data.model.UID
import dev.dres.data.model.basics.media.MediaCollection
import dev.dres.data.model.basics.media.MediaItem
import dev.dres.data.model.submissions.aspects.ItemAspect
import dev.dres.data.model.submissions.aspects.TemporalSubmissionAspect
import dev.dres.data.model.submissions.aspects.TextAspect
import dev.dres.run.InteractiveRunManager
import dev.dres.run.RunExecutor
import dev.dres.utilities.extensions.UID
import dev.dres.utilities.extensions.errorResponse
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore

/**
 * An [AbstractPreviewHandler] used to access previews of [MediaItem]s based on a specific [Submission].
 *
 * @author Luca Rossetto
 * @author Loris Sauter
 * @version 2.0.0
 */
class SubmissionPreviewHandler(store: TransientEntityStore, config: Config) : AbstractPreviewHandler(store, config) {
    override val route: String = "preview/submission/{runId}/{submissionId}"

    @OpenApi(
        summary = "Returns a preview image for a specific submission.",
        path = "/api/v1/preview/submission/{runId}/{submissionId}",
        pathParams = [
            OpenApiParam("runId", String::class, "Competition ID"),
            OpenApiParam("submissionId", String::class, "Subission ID")
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
            val runId = params["runId"] ?: throw ErrorStatusException(404, "Parameter 'runId' is invalid", ctx)
            val submissionId = params["submissionId"]?.UID() ?: throw ErrorStatusException(404, "Parameter 'submissionId' is missing", ctx)
            val run = RunExecutor.managerForId(runId) ?: throw ErrorStatusException(404, "Competition Run $runId not found", ctx)
            if (run !is InteractiveRunManager) throw ErrorStatusException(404, "Competition Run $runId is not interactive", ctx)

            val submission = run.allSubmissions.find { it.uid == submissionId }
                ?: throw ErrorStatusException(404, "Submission '$submissionId' not found", ctx)

            when (submission) {
                is ItemAspect -> {
                    handlePreviewRequest(submission.item, if (submission is TemporalSubmissionAspect) submission.start else null, ctx)
                }
                is TextAspect -> {
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
        } catch (e: ErrorStatusException) {
            ctx.errorResponse(e)
        }

    }


    override fun doGet(ctx: Context): Any {
        throw UnsupportedOperationException("SubmissionPreviewHandler::doGet() is not supported and should not be executed!")
    }
}