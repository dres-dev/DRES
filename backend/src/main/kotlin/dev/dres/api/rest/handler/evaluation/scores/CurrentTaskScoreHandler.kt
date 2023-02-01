package dev.dres.api.rest.handler.evaluation.scores

import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.utilities.extensions.eligibleManagerForId
import dev.dres.utilities.extensions.evaluationId
import dev.dres.utilities.extensions.isParticipant
import dev.dres.api.rest.types.evaluation.scores.ApiScore
import dev.dres.api.rest.types.evaluation.scores.ApiScoreOverview
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.data.model.run.RunActionContext
import dev.dres.data.model.run.Task
import dev.dres.run.InteractiveRunManager
import dev.dres.run.score.interfaces.TeamTaskScorer
import dev.dres.run.score.scoreboard.ScoreOverview
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.asSequence

/**
 * Generates and lists the [ScoreOverview] for the currently active [Task].
 *
 * Only valid for [InteractiveRunManager]s.
 *
 * @author Ralph Gasser
 * @version 2.0.0
 */
class CurrentTaskScoreHandler(store: TransientEntityStore) : AbstractScoreHandler(store), GetRestHandler<ApiScoreOverview> {

    override val route = "score/evaluation/{evaluationId}/current"

    @OpenApi(
        summary = "Returns the overviews of all score boards for the current task, if it is either running or has just ended.",
        path = "/api/v2/score/evaluation/{evaluationId}/current",
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
        val manager = ctx.eligibleManagerForId() as? InteractiveRunManager ?: throw ErrorStatusException(400, "Specified evaluation ${ctx.evaluationId()} does not scores for a current task.", ctx)
        if (!manager.runProperties.participantCanView && ctx.isParticipant()) {
            throw ErrorStatusException(403, "Access denied.", ctx)
        }

        return this.store.transactional(true) {
            val rac = RunActionContext.runActionContext(ctx, manager)
            val scorer = manager.currentTask(rac)?.scorer ?: throw ErrorStatusException(404, "No active task run in evaluation ${ctx.evaluationId()}.", ctx)
            val scores =  (scorer as? TeamTaskScorer)?.teamScoreMap() ?: throw ErrorStatusException(400, "Scorer has more than one score per team for evaluation ${ctx.evaluationId()}.", ctx)
            ApiScoreOverview("task",
                manager.currentTaskTemplate(rac).taskGroup.name,
                manager.template.teams.asSequence().map { team -> ApiScore(team.id, scores[team.id] ?: 0.0) }.toList()
            )
        }
    }
}