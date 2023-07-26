package dev.dres.api.rest.handler.judgement

import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.utilities.extensions.eligibleManagerForId
import dev.dres.api.rest.types.collection.ApiMediaType
import dev.dres.api.rest.types.judgement.ApiJudgementRequest
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.data.model.submissions.AnswerType
import dev.dres.data.model.submissions.DbAnswerSet
import dev.dres.data.model.submissions.DbAnswerType
import dev.dres.run.RunManager
import dev.dres.run.validation.interfaces.VoteValidator
import dev.dres.utilities.extensions.sessionToken
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.firstOrNull

/**
 * A [GetRestHandler] to dequeue the next [ApiJudgementRequest] that is ready for public voting.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class DequeueVoteHandler(store: TransientEntityStore): AbstractJudgementHandler(store), GetRestHandler<ApiJudgementRequest> {
    override val route = "evaluation/{evaluationId}/vote/next"

    @OpenApi(
        summary = "Gets the next open submission that is waiting to be voted on.",
        path = "/api/v2/evaluation/{evaluationId}/vote/next",
        operationId = OpenApiOperation.AUTO_GENERATE,
        pathParams = [OpenApiParam("evaluationId", String::class, "The evaluation ID.", required = true)],
        tags = ["Judgement"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(ApiJudgementRequest::class)]),
            OpenApiResponse("202", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
        ],
        methods = [HttpMethod.GET]
    )
    override fun doGet(ctx: Context): ApiJudgementRequest {
        val request = this.store.transactional(false) {//TODO needs adjustment to deal with answerSets
            val evaluationManager = ctx.eligibleManagerForId<RunManager>()

            val validator = evaluationManager.judgementValidators.filterIsInstance<VoteValidator>().find {  it.isActive } ?: return@transactional null
            val next = validator.next()
                ?: /* No submission awaiting judgement */
                return@transactional null
            val taskDescription = next.second.task.template.textualDescription()
            return@transactional ApiJudgementRequest(
                token = next.first,
                validator = validator.id,
                taskDescription = taskDescription,
                answerSet = next.second.toApi(false)
            )
        }
        return request ?: throw ErrorStatusException(
            202,
            "There is currently no submission awaiting judgement.",
            ctx
        )
    }
}
