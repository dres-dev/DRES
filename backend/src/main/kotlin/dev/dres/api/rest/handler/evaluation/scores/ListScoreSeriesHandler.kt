package dev.dres.api.rest.handler.evaluation.scores

import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.api.rest.handler.eligibleManagerForId
import dev.dres.api.rest.handler.evaluationId
import dev.dres.api.rest.types.evaluation.scores.ApiScoreSeries
import dev.dres.api.rest.types.evaluation.scores.ApiScoreSeriesPoint
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.run.InteractiveRunManager
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore

/**
 * A [GetRestHandler] that returns a time series of all data points for a given run and scoreboard.
 */
class ListScoreSeriesHandler(store: TransientEntityStore) : AbstractScoreHandler(store), GetRestHandler<List<ApiScoreSeries>> {
    override val route = "score/evaluation/{evaluationId}/series/{scoreboard}"

    @OpenApi(
        summary = "Returns a time series for a given run and scoreboard.",
        path = "/api/v1/score/evaluation/{evaluationId}/series/{scoreboard}",
        tags = ["Evaluation Scores"],
        pathParams = [
            OpenApiParam("evaluationId", String::class, "The evaluation ID.", required = true),
            OpenApiParam("scoreboard", String::class, "Name of the scoreboard to return the time series for.", required = true)
        ],
        responses = [
            OpenApiResponse("200", [OpenApiContent(Array<ApiScoreSeries>::class)]),
            OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
        ],
        methods = [HttpMethod.GET]
    )
    override fun doGet(ctx: Context): List<ApiScoreSeries> {
        val scoreboard = ctx.pathParamMap().getOrElse("scoreboard") { throw ErrorStatusException(400, "Parameter 'scoreboard' is missing!'", ctx) }
        val manager = ctx.eligibleManagerForId() as? InteractiveRunManager ?: throw ErrorStatusException(400, "Specified evaluation ${ctx.evaluationId()} does not have a score history.'", ctx)
        return this.store.transactional(true) {
            manager.scoreHistory
                .filter { it.name == scoreboard }
                .groupBy { it.team }
                .mapValues { it.value.map { p -> ApiScoreSeriesPoint(p.score, p.timestamp) } }
                .map { ApiScoreSeries(it.key, scoreboard, it.value) }
        }
    }
}
