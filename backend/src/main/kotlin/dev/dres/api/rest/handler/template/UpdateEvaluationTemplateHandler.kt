package dev.dres.api.rest.handler.template

import dev.dres.api.rest.handler.PatchRestHandler
import dev.dres.api.rest.types.template.ApiEvaluationTemplate
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.api.rest.types.status.SuccessStatus
import dev.dres.data.model.template.DbEvaluationTemplate
import dev.dres.mgmt.TemplateManager
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.openapi.*

/**
 * A [AbstractEvaluationTemplateHandler] that can be used to create a new [DbEvaluationTemplate].
 *
 * @author Ralph Gasser
 * @author Luca Rossetto
 * @author Loris Sauter
 * @version 2.1.0
 */
class UpdateEvaluationTemplateHandler :
    AbstractEvaluationTemplateHandler(), PatchRestHandler<SuccessStatus> {

    override val route: String = "template/{templateId}"

    @OpenApi(
        summary = "Updates an existing evaluation template.",
        path = "/api/v2/template/{templateId}",
        operationId = OpenApiOperation.AUTO_GENERATE,
        pathParams = [OpenApiParam(
            "templateId",
            String::class,
            "The evaluation template ID.",
            required = true,
            allowEmptyValue = false
        )],
        methods = [HttpMethod.PATCH],
        requestBody = OpenApiRequestBody([OpenApiContent(ApiEvaluationTemplate::class)]),
        tags = ["Template"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
            OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("409", [OpenApiContent(ErrorStatus::class)])
        ]
    )
    override fun doPatch(ctx: Context): SuccessStatus {
        val apiValue = try {
            ctx.bodyAsClass(ApiEvaluationTemplate::class.java)
        } catch (e: BadRequestResponse) {
            throw ErrorStatusException(400, "Invalid parameters. This is a programmers error!", ctx)
        }

        val existing = TemplateManager.getTemplate(apiValue.id) ?: throw ErrorStatusException(404, "Template with id '' not found", ctx)
        if (existing.modified != apiValue.modified) {
            throw ErrorStatusException(409, "Evaluation template ${apiValue.id} has been modified in the meantime. Reload and try again!", ctx)
        }

        try {
            TemplateManager.updateTemplate(apiValue)
        } catch (e: IllegalArgumentException) {
            throw ErrorStatusException(404, e.message ?: "", ctx)
        }

        return SuccessStatus("Evaluation template with ID ${apiValue.id} was updated successfully.")
    }
}


