package dev.dres.api.rest.handler.download

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.data.model.run.DbEvaluation
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.eq
import kotlinx.dnq.query.firstOrNull
import kotlinx.dnq.query.query

/**
 * A [GetRestHandler] that allows for downloading the entire [DbEvaluation] structure as JSON file.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class EvaluationDownloadHandler(private val store: TransientEntityStore) : AbstractDownloadHandler(), GetRestHandler<String> {

    /** The route of this [EvaluationDownloadHandler]. */
    override val route = "download/evaluation/{evaluationId}"

    @OpenApi(
        summary = "Provides a JSON download of the entire evaluation  structure.",
        path = "/api/v2/download/evaluation/{evaluationId}",
        operationId = OpenApiOperation.AUTO_GENERATE,
        tags = ["Download"],
        pathParams = [
            OpenApiParam("evaluationId", String::class, "The evaluation ID.", required = true)
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
        val evaluationId = ctx.pathParamMap().getOrElse("evaluationId") { throw ErrorStatusException(400, "Parameter 'evaluationId' is missing!'", ctx) }
        val evaluation = this.store.transactional(true) {
            DbEvaluation.query(DbEvaluation::id eq evaluationId).firstOrNull()?.toApi()
                ?: throw ErrorStatusException(404, "Run $evaluationId not found", ctx)
        }

        /* Set header for download. */
        ctx.header("Content-Disposition", "attachment; filename=\"run-${evaluationId}.json\"")

        /* Return value. */
        val mapper = jacksonObjectMapper()
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(evaluation)
    }
}
