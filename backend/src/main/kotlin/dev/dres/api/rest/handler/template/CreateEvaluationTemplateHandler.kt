package dev.dres.api.rest.handler.template

import dev.dres.api.rest.handler.PostRestHandler
import dev.dres.api.rest.types.template.ApiCreateEvaluation
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.api.rest.types.status.SuccessStatus
import dev.dres.data.model.template.DbEvaluationTemplate
import dev.dres.mgmt.TemplateManager
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.http.bodyAsClass
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore
import org.joda.time.DateTime
import java.util.*

/**
 * A [AbstractEvaluationTemplateHandler] that can be used to create a new [DbEvaluationTemplate].
 *
 * @author Ralph Gasser
 * @author Luca Rossetto
 * @author Loris Sauter
 * @version 2.0.0
 */
class CreateEvaluationTemplateHandler : AbstractEvaluationTemplateHandler(), PostRestHandler<SuccessStatus> {

    override val route: String = "template"

    @OpenApi(
        summary = "Creates a new evaluation template.",
        path = "/api/v2/template",
        operationId = OpenApiOperation.AUTO_GENERATE,
        methods = [HttpMethod.POST],
        requestBody = OpenApiRequestBody([OpenApiContent(ApiCreateEvaluation::class)]),
        tags = ["Template"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
            OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
        ]
    )
    override fun doPost(ctx: Context): SuccessStatus {
        val createRequest = try {
            ctx.bodyAsClass<ApiCreateEvaluation>()
        } catch (e: BadRequestResponse) {
            throw ErrorStatusException(400, "Invalid parameters. This is a programmers error!", ctx)
        }

        val template = TemplateManager.createEvaluationTemplate(createRequest.name, createRequest.description)

        return SuccessStatus("Evaluation template with ID ${template.id} was created successfully.")
    }
}
