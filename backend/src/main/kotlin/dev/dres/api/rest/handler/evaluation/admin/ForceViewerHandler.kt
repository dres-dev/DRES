package dev.dres.api.rest.handler.evaluation.admin

import dev.dres.api.rest.handler.PostRestHandler
import dev.dres.utilities.extensions.evaluationId
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.api.rest.types.status.SuccessStatus
import dev.dres.data.model.run.RunActionContext
import dev.dres.data.model.run.RunActionContext.Companion.runActionContext
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore

/**
 * A [PostRestHandler] to enforce the state of a viewer for a particular evaluation.
 *
 * @author Ralph Gasser
 * @author Luca Rossetto
 * @author Loris Sauter
 * @version 2.0.0
 */
class ForceViewerHandler() : AbstractEvaluationAdminHandler(), PostRestHandler<SuccessStatus> {
    override val route: String = "evaluation/admin/{evaluationId}/viewer/list/{viewerId}/force"

    @OpenApi(
        summary = "Forces a viewer with the given viewer ID into the READY state. This is a method for admins.",
        path = "/api/v2/evaluation/admin/{evaluationId}/viewer/list/{viewerId}/force",
        operationId = OpenApiOperation.AUTO_GENERATE,
        methods = [HttpMethod.POST],
        pathParams = [
            OpenApiParam("evaluationId", String::class, "The evaluation ID.", required = true, allowEmptyValue = false),
            OpenApiParam("viewerId", String::class, "The viewer ID.", required = true, allowEmptyValue = false)
        ],
        tags = ["Evaluation Administrator"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
            OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
        ]
    )
    override fun doPost(ctx: Context): SuccessStatus {
        val evaluationId = ctx.evaluationId()
        val viewerId =
            ctx.pathParamMap()["viewerId"] ?: throw ErrorStatusException(404, "Parameter 'viewerId' is missing!'", ctx)
        val evaluationManager =
            getManager(evaluationId) ?: throw ErrorStatusException(404, "Run $evaluationId not found", ctx)
        val rac = ctx.runActionContext()

        try {
            if (evaluationManager.overrideReadyState(rac, viewerId)) {
                return SuccessStatus("State for viewer $viewerId (evaluation '$evaluationId') forced successfully.")
            } else {
                throw ErrorStatusException(404, "Viewer $viewerId does not exist!'", ctx)
            }
        } catch (e: IllegalStateException) {
            throw ErrorStatusException(
                400,
                "State for viewer $viewerId (evaluation '$evaluationId') could not be enforced because evaluation is in the wrong state (state = ${evaluationManager.status}).",
                ctx
            )
        }

    }
}
