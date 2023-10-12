package dev.dres.api.rest.handler.evaluation.admin

import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.api.rest.types.evaluation.ApiEvaluation
import dev.dres.api.rest.types.evaluation.ApiEvaluationOverview
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.data.model.run.DbEvaluation
import dev.dres.utilities.extensions.evaluationId
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.eq
import kotlinx.dnq.query.firstOrNull
import kotlinx.dnq.query.query

class GetEvaluationHandler(private val store: TransientEntityStore): AbstractEvaluationAdminHandler(),
    GetRestHandler<ApiEvaluation> {
    override val route = "evaluation/admin/{evaluationId}"

    @OpenApi(
        summary = "Provides the evaluation.",
        path = "/api/v2/evaluation/admin/{evaluationId}",
        operationId = OpenApiOperation.AUTO_GENERATE,
        methods = [HttpMethod.GET],
        pathParams = [
            OpenApiParam("evaluationId", String::class, "The evaluation ID", required = true, allowEmptyValue = false),
        ],
        tags = ["Evaluation Administrator"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(ApiEvaluation::class)]),
            OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
        ]
    )
    override fun doGet(ctx: Context): ApiEvaluation {
        /* Obtain run id and run. */
        val evaluationId = ctx.pathParamMap().getOrElse("evaluationId") { throw ErrorStatusException(400, "Parameter 'evaluationId' is missing!'", ctx) }
        return this.store.transactional(true) {
            DbEvaluation.query(DbEvaluation::id eq evaluationId).firstOrNull()?.toApi()
                ?: throw ErrorStatusException(404, "Run $evaluationId not found", ctx)
        }

    }
}
