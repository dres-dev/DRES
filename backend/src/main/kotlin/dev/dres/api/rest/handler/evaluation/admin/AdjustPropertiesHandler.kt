package dev.dres.api.rest.handler.evaluation.admin

import dev.dres.api.rest.handler.PatchRestHandler
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.api.rest.types.status.SuccessStatus
import dev.dres.data.model.run.DbEvaluation
import dev.dres.data.model.run.ApiRunProperties
import dev.dres.utilities.extensions.evaluationId
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.http.bodyAsClass
import io.javalin.openapi.*

/**
 * A [PatchRestHandler] handler to adjust an ongoing [DbEvaluation]'s [ApiRunProperties].
 *
 * @author Ralph Gasser
 * @author Luca Rossetto
 * @author Loris Sauter
 * @version 2.0.0
 */
class AdjustPropertiesHandler : AbstractEvaluationAdminHandler(), PatchRestHandler<SuccessStatus> {
    override val route = "evaluation/admin/{evaluationId}/properties"

    @OpenApi(
        summary = "Changes the properties of an evaluation.",
        path = "/api/v2/evaluation/admin/{evaluationId}/properties",
        methods = [HttpMethod.PATCH],
        operationId = OpenApiOperation.AUTO_GENERATE,
        pathParams = [
            OpenApiParam("evaluationId", String::class, "The evaluation ID", required = true, allowEmptyValue = false),
        ],
        requestBody = OpenApiRequestBody([OpenApiContent(ApiRunProperties::class)]),
        tags = ["Evaluation Administrator"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
            OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
        ]
    )
    override fun doPatch(ctx: Context): SuccessStatus {
        val properties = try {
            ctx.bodyAsClass<ApiRunProperties>()
        } catch (e: BadRequestResponse) {
            throw ErrorStatusException(400, "Invalid parameters. This is a programmers error!", ctx)
        }
        val evaluationId = ctx.evaluationId()
        val evaluationManager =
            getManager(evaluationId) ?: throw ErrorStatusException(404, "Evaluation $evaluationId not found", ctx)

        evaluationManager.updateProperties(properties)
        return SuccessStatus("Properties updated successfully!")

    }
}

