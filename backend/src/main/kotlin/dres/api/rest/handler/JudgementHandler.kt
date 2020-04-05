package dres.api.rest.handler

import dres.api.rest.RestApiRole
import dres.api.rest.types.status.ErrorStatus
import dres.api.rest.types.status.ErrorStatusException
import dres.api.rest.types.status.SuccessStatus
import dres.data.model.run.SubmissionStatus
import dres.data.model.run.VBSSubmission
import dres.run.RunExecutor
import io.javalin.core.security.Role
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.plugin.openapi.annotations.*

abstract class AbstractJudgementHandler : RestHandler, AccessManagedRestHandler {

    override val permittedRoles: Set<Role> = setOf(RestApiRole.JUDGE)

}

data class Judgement(val id: Int, val judgement: SubmissionStatus)

data class JudgementRequest(val id: Int, val collection: String, val item: String, val startTime: String?, val endTime: String?)

class NextOpenJudgementHandler : AbstractJudgementHandler(), GetRestHandler<JudgementRequest> {
    override val route = "judgement/next"

    @OpenApi(
            summary = "Gets the next open Submission to be Judged.",
            path = "/api/judgement/next",
            tags = ["Judgement"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(JudgementRequest::class)]),
                OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doGet(ctx: Context): JudgementRequest {

        val next = RunExecutor.judgementQueue.next() ?: throw ErrorStatusException(404, "No submissions to be judged")

        return if(next.submission is VBSSubmission) {
            JudgementRequest(next.id, next.submission.collection, next.submission.item, next.submission.start.toString(), next.submission.end.toString())
        } else {
            JudgementRequest(next.id, next.submission.collection, next.submission.item, null, null)
        }
    }

}

class PostJudgementHandler : AbstractJudgementHandler(), PostRestHandler<SuccessStatus> {
    override val route = "judgement"

    @OpenApi(
            summary = "Returns a Judgement.",
            path = "/api/judgement", method = HttpMethod.POST,
            requestBody = OpenApiRequestBody([OpenApiContent(Judgement::class)]),
            tags = ["Judgement"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
                OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doPost(ctx: Context): SuccessStatus {

        val judgement = try {
            ctx.bodyAsClass(Judgement::class.java)
        } catch (e: BadRequestResponse) {
            throw ErrorStatusException(400, "Invalid parameters. This is a programmers error!")
        }

        TODO("Not yet implemented")
    }


}