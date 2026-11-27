package dev.dres.api.rest.handler.evaluation.admin

import dev.dres.api.rest.AccessManager
import dev.dres.api.rest.handler.DeleteRestHandler
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.api.rest.types.status.SuccessStatus
import dev.dres.mgmt.EvaluationManager
import dev.dres.utilities.extensions.evaluationId
import dev.dres.utilities.extensions.sessionToken
import io.javalin.http.Context
import io.javalin.openapi.HttpMethod
import io.javalin.openapi.OpenApi
import io.javalin.openapi.OpenApiContent
import io.javalin.openapi.OpenApiOperation
import io.javalin.openapi.OpenApiParam
import io.javalin.openapi.OpenApiResponse
import org.slf4j.LoggerFactory

/** Permanently deletes a stopped evaluation and its result data. */
class DeleteEvaluationHandler : AbstractEvaluationAdminHandler(), DeleteRestHandler<SuccessStatus> {

    private val logger = LoggerFactory.getLogger(this.javaClass)
    override val route = "evaluation/admin/{evaluationId}"

    @OpenApi(
        summary = "Permanently deletes a stopped evaluation including its submissions.",
        path = "/api/v2/evaluation/admin/{evaluationId}",
        operationId = OpenApiOperation.AUTO_GENERATE,
        methods = [HttpMethod.DELETE],
        pathParams = [OpenApiParam("evaluationId", String::class, "Evaluation ID", required = true, allowEmptyValue = false)],
        tags = ["Evaluation Administrator"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("409", [OpenApiContent(ErrorStatus::class)])
        ]
    )
    override fun doDelete(ctx: Context): SuccessStatus {
        val id = ctx.evaluationId()
        val deleted = try {
            EvaluationManager.deleteEvaluation(id)
        } catch (e: IllegalStateException) {
            throw ErrorStatusException(409, e.message ?: "Evaluation $id is still running.", ctx)
        } ?: throw ErrorStatusException(404, "Evaluation $id not found.", ctx)
        logger.info(
            "Evaluation '${deleted.name}' ($id) with ${deleted.submissions} submissions was deleted by user ${AccessManager.userIdForSession(ctx.sessionToken())}."
        )
        return SuccessStatus("Evaluation '${deleted.name}' and its ${deleted.submissions} submissions were deleted.")
    }
}
