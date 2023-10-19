package dev.dres.api.rest.handler.judgement

import dev.dres.api.rest.handler.PostRestHandler
import dev.dres.utilities.extensions.eligibleManagerForId
import dev.dres.api.rest.types.judgement.ApiJudgementRequest
import dev.dres.api.rest.types.judgement.ApiVote
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.api.rest.types.status.SuccessStatus
import dev.dres.data.model.submissions.VerdictStatus
import dev.dres.run.RunManager
import dev.dres.run.validation.interfaces.VoteValidator
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore

/**
 * A [PostRestHandler] to post an [ApiVote] on a previously dequeued [ApiJudgementRequest].
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class PostVoteHandler : AbstractJudgementHandler(), PostRestHandler<SuccessStatus> {
    override val route = "evaluation/{evaluationId}/judge/vote"
    override val apiVersion = "v2"


    @OpenApi(
        summary = "Returns a Vote.",
        path = "/api/v2/evaluation/{evaluationId}/judge/vote", methods = [HttpMethod.POST],
        operationId = OpenApiOperation.AUTO_GENERATE,
        pathParams = [OpenApiParam("evaluationId", String::class, "The evaluation ID.", required = true)],
        requestBody = OpenApiRequestBody([OpenApiContent(ApiVote::class)]),
        tags = ["Judgement"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
            OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
        ]
    )
    override fun doPost(ctx: Context): SuccessStatus {
        val vote = try {
            ctx.bodyAsClass(ApiVote::class.java)
        } catch (e: BadRequestResponse) {
            throw ErrorStatusException(400, "Invalid parameters. This is a programmers error!", ctx)
        }


        val evaluationManager = ctx.eligibleManagerForId<RunManager>()
        val validator =
            evaluationManager.judgementValidators.find { it is VoteValidator && it.isActive } as? VoteValidator // Get first active vote validator
                ?: throw ErrorStatusException(
                    404,
                    "There is currently no voting going on in evaluation ${evaluationManager.id}.",
                    ctx
                )
        validator.vote(vote.verdict)

        return SuccessStatus("Vote received.")
    }
}
