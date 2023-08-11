package dev.dres.api.rest.handler.evaluation.scores

import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.api.rest.types.evaluation.scores.ApiScoreSeries
import dev.dres.api.rest.types.evaluation.scores.ApiScoreSeriesPoint
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.data.model.run.interfaces.EvaluationId
import dev.dres.run.eventstream.*
import dev.dres.run.score.ScoreTimePoint
import io.javalin.http.Context
import io.javalin.openapi.*
import java.util.concurrent.ConcurrentHashMap

/**
 * A [GetRestHandler] that returns a time series of all data points for a given run and scoreboard.
 */
class ListScoreSeriesHandler : AbstractScoreHandler(), GetRestHandler<List<ApiScoreSeries>>, StreamEventHandler {
    override val route = "score/evaluation/{evaluationId}/series/{scoreboard}"

    init {
        /* Subscribe to EventStream */
        EventStreamProcessor.register(this)
    }

    private val scoreHistoryMap = ConcurrentHashMap<EvaluationId,ConcurrentHashMap<ScoreboardName, ArrayList<ScoreTimePoint>>>()

    @OpenApi(
        summary = "Returns a time series for a given run and scoreboard.",
        path = "/api/v2/score/evaluation/{evaluationId}/series/{scoreboard}",
        operationId = OpenApiOperation.AUTO_GENERATE,
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
        val evaluationId = ctx.pathParamMap().getOrElse("evaluationId") { throw ErrorStatusException(400, "Parameter 'evaluationId' is missing!'", ctx) }
        val list = this.scoreHistoryMap[evaluationId]?.get(scoreboard) ?: emptyList()
        return list.groupBy { it.team }.map { ApiScoreSeries(it.key, scoreboard, it.value.map { p -> ApiScoreSeriesPoint(p.score, p.timestamp) }) }
    }

    override fun handleStreamEvent(event: StreamEvent) {
        //add scores
        if (event is ScoreUpdateEvent) {
            if (!this.scoreHistoryMap.containsKey(event.runId)) {
                this.scoreHistoryMap[event.runId] = ConcurrentHashMap()
            }

            val runMap = this.scoreHistoryMap[event.runId]!!

            if (!runMap.containsKey(event.scoreboardName)) {
                runMap[event.scoreboardName] = ArrayList()
            }

            runMap[event.scoreboardName]!!.addAll(
                event.scores.map {
                    ScoreTimePoint(event.scoreboardName, it, event.timeStamp)
                }
            )
        }
        if (event is RunEndEvent) {
            this.scoreHistoryMap.remove(event.runId)
        }
    }
}

typealias ScoreboardName = String
