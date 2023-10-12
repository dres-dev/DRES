package dev.dres.api.rest.handler.template

import dev.dres.api.rest.handler.PostRestHandler
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.api.rest.types.template.ApiEvaluationTemplate
import dev.dres.mgmt.TemplateManager
import io.javalin.http.Context
import io.javalin.openapi.*

class CloneEvaluationTemplateHandler :
    AbstractEvaluationTemplateHandler(), PostRestHandler<ApiEvaluationTemplate> {

    override val route: String = "template/{templateId}/clone"

    @OpenApi(
        summary = "Clones an existing evaluation template.",
        path = "/api/v2/template/{templateId}/clone",
        operationId = OpenApiOperation.AUTO_GENERATE,
        pathParams = [OpenApiParam(
            "templateId",
            String::class,
            "The evaluation template ID to clone.",
            required = true,
            allowEmptyValue = false
        )],
        methods = [HttpMethod.POST],
        tags = ["Template"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(ApiEvaluationTemplate::class)]),
            OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("409", [OpenApiContent(ErrorStatus::class)])
        ]
    )
    override fun doPost(ctx: Context): ApiEvaluationTemplate {

        val id = this.templateIdFromContext(ctx)

        return try {
            /* Clone */
            TemplateManager.copyTemplate(id)
        } catch (e: IllegalArgumentException) {
            throw ErrorStatusException(404, e.message ?: "", ctx)
        }
    }
}
