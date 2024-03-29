package dev.dres.api.rest.handler.evaluation.viewer

import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.utilities.extensions.eligibleManagerForId
import dev.dres.utilities.extensions.isParticipant
import dev.dres.api.rest.types.evaluation.submission.ApiSubmission
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.api.rest.types.template.tasks.options.ApiTaskOption
import dev.dres.data.model.run.RunActionContext.Companion.runActionContext
import dev.dres.data.model.submissions.DbSubmission
import dev.dres.data.model.template.task.options.DbTaskOption
import dev.dres.run.InteractiveRunManager
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.contains

/**
 *
 */
class GetSubmissionInfoHandler(private val store: TransientEntityStore): AbstractEvaluationViewerHandler(), GetRestHandler<List<ApiSubmission>> {
    override val route = "evaluation/{evaluationId}/submission/list"

    @OpenApi(
        summary = "Returns all submissions for the current task run, if one is either running or has just ended.",
        path = "/api/v2/evaluation/{evaluationId}/submission/list",
        operationId = OpenApiOperation.AUTO_GENERATE,
        tags = ["Evaluation"],
        pathParams = [
            OpenApiParam("evaluationId", String::class, "The evaluation ID.", required = true, allowEmptyValue = false)
        ],
        responses = [
            OpenApiResponse("200", [OpenApiContent(Array<ApiSubmission>::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
        ],
        methods = [HttpMethod.GET]
    )
    override fun doGet(ctx: Context): List<ApiSubmission>  {
        val manager = ctx.eligibleManagerForId<InteractiveRunManager>()
        val rac = ctx.runActionContext()

        if (!manager.runProperties.participantCanView && ctx.isParticipant()) {
            throw ErrorStatusException(403, "Access denied.", ctx)
        }

        val limit = manager.runProperties.limitSubmissionPreviews
        val currentTask = manager.currentTask(rac) ?: return emptyList()

        val blind = currentTask.isRunning && manager.template.taskTypes.find { it.name == currentTask.template.taskType }?.taskOptions?.contains(
            ApiTaskOption.HIDDEN_RESULTS
        ) == true

        /* Obtain current task run and check status. */
        return this.store.transactional (true) {
            if (limit > 0) {
                limitSubmissions(manager.currentSubmissions(rac), limit, blind)
            } else {
                manager.currentSubmissions(rac).map { it.toApi(blind) }
            }
        }
    }

    /**
     * Implements a manual limit on the provided list of [DbSubmission]s.
     *
     * TODO: Delegate to database?
     *
     * @param submissions The [List] of [DbSubmission]s to limit.
     * @param limit The number of items to limit to.
     * @param blind If [DbSubmission] should be anonymised.
     * @return Limited [List] of [DbSubmission]
     */
    private fun limitSubmissions(submissions: List<DbSubmission>, limit: Int, blind: Boolean = false): List<ApiSubmission>
        = submissions.groupBy { it.team.id }.values.map {
            it.sortedBy { s -> s.timestamp }.take(limit)
        }.flatMap {
            it.map { s -> s.toApi(blind) }
        }
}
