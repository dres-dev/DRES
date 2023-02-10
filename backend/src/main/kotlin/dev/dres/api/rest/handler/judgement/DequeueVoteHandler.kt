package dev.dres.api.rest.handler.judgement

import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.utilities.extensions.eligibleManagerForId
import dev.dres.api.rest.types.collection.ApiMediaType
import dev.dres.api.rest.types.judgement.ApiJudgementRequest
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.data.model.submissions.AnswerType
import dev.dres.data.model.submissions.DbAnswerType
import dev.dres.run.validation.interfaces.VoteValidator
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
        pathParams = [OpenApiParam("evaluationId", String::class, "The evaluation ID.")],
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
        /* Obtain manager and check if any submissions are waiting for judgement. */
        val evaluationManager = ctx.eligibleManagerForId()

        /* Start transaction. */
        this.store.transactional {//TODO needs adjustment to deal with answerSets
            do {
                val validator = evaluationManager.judgementValidators.filterIsInstance<VoteValidator>().find {  it.isActive } ?: break
                val next = validator.nextSubmissionToVoteOn() ?: break
                val taskDescription = next.task.template.textualDescription()
                when (next.answers().firstOrNull()?.type()) {
                    AnswerType.TEXT -> {
                        val text = next.answers().firstOrNull()?.text ?: continue
                        return@transactional ApiJudgementRequest(null, ApiMediaType.TEXT, validator.id, "text", text, taskDescription, null, null)
                    }
                    AnswerType.ITEM -> {
                        val item = next.answers().firstOrNull()?.item ?: continue
                        return@transactional ApiJudgementRequest(null, item.type().toApi(), validator.id, item.collection.id, item.id!!, taskDescription, null, null)
                    }
                    AnswerType.TEMPORAL -> {
                        val answer = next.answers().firstOrNull() ?: continue
                        val item = answer.item ?: continue
                        val start = answer.start ?: continue
                        val end = answer.end ?: continue
                        return@transactional ApiJudgementRequest(null, item.type().toApi(), validator.id, item.collection.id, item.id!!, taskDescription, start, end)
                    }
                    else -> continue
                }
            } while (true)
        }
        throw ErrorStatusException(202, "There is currently no voting going on in evaluation ${evaluationManager.id}.", ctx, true)
    }
}
