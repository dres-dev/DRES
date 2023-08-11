package dev.dres.api.rest.handler.evaluation.scores

import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.utilities.extensions.eligibleManagerForId
import dev.dres.utilities.extensions.evaluationId
import dev.dres.utilities.extensions.isParticipant
import dev.dres.api.rest.types.evaluation.scores.ApiScore
import dev.dres.api.rest.types.evaluation.scores.ApiScoreOverview
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.data.model.run.DbTask
import dev.dres.data.model.run.RunActionContext.Companion.runActionContext
import dev.dres.run.InteractiveRunManager
import dev.dres.run.score.scoreboard.ScoreOverview
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.asSequence

/**
 * Generates and lists the [ScoreOverview] for the currently active [DbTask].
 *
 * Only valid for [InteractiveRunManager]s.
 *
 * @author Ralph Gasser
 * @version 2.0.0
 */
class CurrentTaskScoreHandler : AbstractScoreHandler(),
    GetRestHandler<ApiScoreOverview> {

    override val route = "score/evaluation/{evaluationId}/current"

    @OpenApi(
        summary = "Returns the overviews of all score boards for the current task, if it is either running or has just ended.",
        path = "/api/v2/score/evaluation/{evaluationId}/current",
        operationId = OpenApiOperation.AUTO_GENERATE,
        tags = ["Evaluation Scores"],
        pathParams = [OpenApiParam("evaluationId", String::class, "The evaluation ID.", required = true)],
        responses = [
            OpenApiResponse("200", [OpenApiContent(ApiScoreOverview::class)]),
            OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
        ],
        methods = [HttpMethod.GET]
    )
    override fun doGet(ctx: Context): ApiScoreOverview {

        val manager = ctx.eligibleManagerForId<InteractiveRunManager>()
        if (!manager.runProperties.participantCanView && ctx.isParticipant()) {
            throw ErrorStatusException(403, "Access denied.", ctx)
        }

        val rac = ctx.runActionContext()
        val scorer = manager.currentTask(rac)?.scorer ?: throw ErrorStatusException(
            404,
            "No active task run in evaluation ${ctx.evaluationId()}.",
            ctx
        )
        val scores = scorer.scoreMap()
        return ApiScoreOverview(
            "task",
            manager.currentTaskTemplate(rac).taskGroup,
            manager.template.teams.asSequence().map { team -> ApiScore(team.id!!, scores[team.id] ?: 0.0) }.toList()
        )
    }

}
