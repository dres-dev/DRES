package dev.dres.api.rest.handler.judgement

import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.api.rest.types.collection.ApiMediaType
import dev.dres.api.rest.types.judgement.ApiJudgementRequest
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.data.model.submissions.DbAnswerType
import dev.dres.utilities.extensions.eligibleManagerForId
import dev.dres.utilities.extensions.sessionToken
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.firstOrNull

/**
 * A [GetRestHandler] to dequeue the next [ApiJudgementRequest] that is ready for judgement.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class DequeueJudgementHandler(store: TransientEntityStore) : AbstractJudgementHandler(store), GetRestHandler<ApiJudgementRequest> {
    override val route = "evaluation/{evaluationId}/judge/next"

    @OpenApi(
        summary = "Gets the next open submission that is waiting to be judged.",
        path = "/api/v2/evaluation/{evaluationId}/judge/next",
        pathParams = [OpenApiParam("evaluationId", String::class, "The evaluation ID.")],
        tags = ["Judgement"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(ApiJudgementRequest::class)]),
            OpenApiResponse("202", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
        ],
        methods = [HttpMethod.GET]
    )
    override fun doGet(ctx: Context): ApiJudgementRequest {
        /* Obtain manager and check if any submissions are waiting for judgement. */
        val evaluationManager = ctx.eligibleManagerForId()

        /* Start transaction. */
        this.store.transactional {
            checkEligibility(ctx, evaluationManager)
            do {
                val validator = evaluationManager.judgementValidators.find { it.hasOpen } ?: break
                val next = validator.next(ctx.sessionToken()!!) ?: break
                val taskDescription = next.second.task.template.textualDescription()
                when (next.second.answers.firstOrNull()?.type) {
                    DbAnswerType.TEXT -> {
                        val text = next.second.answers.firstOrNull()?.text ?: continue
                        return@transactional ApiJudgementRequest(next.first, ApiMediaType.TEXT, validator.id, "text", text, taskDescription, null, null)
                    }
                    DbAnswerType.ITEM -> {
                        val item = next.second.answers.firstOrNull()?.item ?: continue
                        return@transactional ApiJudgementRequest(next.first, item.type.toApi(), validator.id, item.collection.id, item.id, taskDescription, null, null)
                    }
                    DbAnswerType.TEMPORAL -> {
                        val answer = next.second.answers.firstOrNull() ?: continue
                        val item = answer.item ?: continue
                        val start = answer.start ?: continue
                        val end = answer.end ?: continue
                        return@transactional ApiJudgementRequest(next.first, item.type.toApi(), validator.id, item.collection.id, item.id, taskDescription, start, end)
                    }
                    else -> continue
                }
            } while (true)
        }
        throw ErrorStatusException(202, "There is currently no submission awaiting judgement.", ctx)
    }
}