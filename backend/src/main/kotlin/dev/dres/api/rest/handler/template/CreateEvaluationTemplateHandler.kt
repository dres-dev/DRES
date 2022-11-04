package dev.dres.api.rest.handler.template

import dev.dres.api.rest.handler.PostRestHandler
import dev.dres.api.rest.types.competition.ApiCreateCompetition
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.api.rest.types.status.SuccessStatus
import dev.dres.data.model.template.EvaluationTemplate
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.http.bodyAsClass
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore
import java.util.*

/**
 * A [AbstractCompetitionDescriptionHandler] that can be used to create a new [EvaluationTemplate].
 *
 * @author Ralph Gasser
 * @author Luca Rossetto
 * @author Loris Sauter
 * @version 2.0.0
 */
class CreateEvaluationTemplateHandler(store: TransientEntityStore) : AbstractCompetitionDescriptionHandler(store), PostRestHandler<SuccessStatus> {

    override val route: String = "template"

    @OpenApi(
        summary = "Creates a new evaluation template.",
        path = "/api/v1/template",
        methods = [HttpMethod.POST],
        requestBody = OpenApiRequestBody([OpenApiContent(ApiCreateCompetition::class)]),
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
            ctx.bodyAsClass<ApiCreateCompetition>()
        } catch (e: BadRequestResponse) {
            throw ErrorStatusException(400, "Invalid parameters. This is a programmers error!", ctx)
        }

        val newId = UUID.randomUUID().toString()
        this.store.transactional {
            EvaluationTemplate.new {
                id = newId
                name = createRequest.name
                description = createRequest.description
            }
        }
        return SuccessStatus("Competition description with ID $newId was created successfully.")
    }
}