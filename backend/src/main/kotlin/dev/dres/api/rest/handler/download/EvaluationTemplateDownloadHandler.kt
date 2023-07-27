package dev.dres.api.rest.handler.download

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.data.model.template.DbEvaluationTemplate
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.eq
import kotlinx.dnq.query.firstOrNull
import kotlinx.dnq.query.query

/**
 * A [GetRestHandler] that allow for downloading the entire [DbEvaluationTemplate] structure as JSON file.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class EvaluationTemplateDownloadHandler(store: TransientEntityStore) : AbstractDownloadHandler(store), GetRestHandler<String> {

    /** The route of this [EvaluationTemplateDownloadHandler]. */
    override val route = "download/template/{templateId}"

    @OpenApi(
        summary = "Provides a JSON download of the entire evaluation template structure.",
        path = "/api/v2/download/template/{templateId}",
        operationId = OpenApiOperation.AUTO_GENERATE,
        tags = ["Download"],
        pathParams = [
            OpenApiParam("templateId", String::class, "The evaluation template ID", required = true)
        ],
        responses = [
            OpenApiResponse("200", [OpenApiContent(String::class, type = "application/json")]),
            OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
        ],
        methods = [HttpMethod.GET]
    )
    override fun doGet(ctx: Context): String {
        /* Obtain run id and run. */
        val templateId = ctx.pathParamMap()["templateId"] ?: throw ErrorStatusException(400, "Parameter 'templateId' is missing!'", ctx)
        val template = this.store.transactional(true) {
           DbEvaluationTemplate.query(DbEvaluationTemplate::id eq templateId).firstOrNull()?.toApi()
               ?: throw ErrorStatusException(404, "Evaluation template $templateId not found", ctx)
        }

        /* Set header for download. */
        ctx.header("Content-Disposition", "attachment; filename=\"evaluation-template-${templateId}.json")

        /* Return value. */
        val mapper = jacksonObjectMapper()
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(template)
    }
}
