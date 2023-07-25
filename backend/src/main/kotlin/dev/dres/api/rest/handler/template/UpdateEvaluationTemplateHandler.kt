package dev.dres.api.rest.handler.template

import dev.dres.api.rest.handler.PatchRestHandler
import dev.dres.api.rest.types.template.ApiEvaluationTemplate
import dev.dres.api.rest.types.template.tasks.ApiTargetType
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.api.rest.types.status.SuccessStatus
import dev.dres.data.model.config.Config
import dev.dres.data.model.admin.DbUser
import dev.dres.data.model.media.DbMediaCollection
import dev.dres.data.model.template.DbEvaluationTemplate
import dev.dres.data.model.template.task.*
import dev.dres.data.model.template.task.options.DbConfiguredOption
import dev.dres.data.model.template.team.DbTeam
import dev.dres.data.model.template.team.DbTeamGroup
import dev.dres.data.model.media.DbMediaItem
import dev.dres.utilities.TemplateUtil
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.creator.findOrNew
import kotlinx.dnq.query.*
import kotlinx.dnq.util.getSafe
import org.joda.time.DateTime
import kotlin.time.ExperimentalTime

/**
 * A [AbstractEvaluationTemplateHandler] that can be used to create a new [DbEvaluationTemplate].
 *
 * @author Ralph Gasser
 * @author Luca Rossetto
 * @author Loris Sauter
 * @version 2.1.0
 */
class UpdateEvaluationTemplateHandler(store: TransientEntityStore, val config: Config) :
    AbstractEvaluationTemplateHandler(store), PatchRestHandler<SuccessStatus> {

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

        /* Store change. */
        this.store.transactional {
            val existing = this.evaluationTemplateById(apiValue.id, ctx)
            if (existing.modified?.millis != apiValue.modified) {
                throw ErrorStatusException(409, "Evaluation template ${apiValue.id} has been modified in the meantime. Reload and try again!", ctx)
            }

            try {
                TemplateUtil.updateDbTemplate(existing, apiValue)
            } catch (e: IllegalArgumentException) {
                throw ErrorStatusException(404, e.message ?: "", ctx)
            }

        }
        return SuccessStatus("Competition with ID ${apiValue.id} was updated successfully.")
    }
}


