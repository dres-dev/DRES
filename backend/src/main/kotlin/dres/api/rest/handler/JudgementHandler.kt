package dres.api.rest.handler

import dres.api.rest.RestApiRole
import dres.api.rest.types.status.ErrorStatus
import dres.api.rest.types.status.ErrorStatusException
import dres.api.rest.types.status.SuccessStatus
import dres.data.model.run.SubmissionStatus
import dres.run.RunExecutor
import io.javalin.core.security.Role
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.plugin.openapi.annotations.*

abstract class AbstractJudgementHandler : RestHandler, AccessManagedRestHandler {
    override val permittedRoles: Set<Role> = setOf(RestApiRole.JUDGE)

    protected fun runId(ctx: Context) = ctx.pathParamMap().getOrElse("runId") {
        throw ErrorStatusException(400, "Parameter 'runId' is missing!'")
    }.toLong()
}

data class Judgement(val token: String, val verdict: SubmissionStatus)

data class JudgementRequest(val token: String, val collection: String, val item: String, val startTime: String?, val endTime: String?)

class NextOpenJudgementHandler : AbstractJudgementHandler(), GetRestHandler<JudgementRequest> {
    override val route = "run/:runId/judge/next"

    @OpenApi(
            summary = "Gets the next open Submission to be judged.",
            path = "/api/judge/run/:runId/next",
            tags = ["Judgement"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(JudgementRequest::class)]),
                OpenApiResponse("202", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doGet(ctx: Context): JudgementRequest {
        val runId = this.runId(ctx)
        val run = RunExecutor.managerForId(runId) ?: throw ErrorStatusException(404, "Run $runId not found")
        val next = run.judgementValidator.next(ctx.req.session.id)

        if (next != null) {
            return JudgementRequest(next.first, next.second.collection, next.second.item, next.second.start?.toString(), next.second.end?.toString())
        } else {
           throw ErrorStatusException(202, "No element left.")
        }
    }
}

class PostJudgementHandler : AbstractJudgementHandler(), PostRestHandler<SuccessStatus> {
    override val route = "run/:runId/judge"

    @OpenApi(
            summary = "Returns a Judgement.",
            path = "/api/judge/run/:runId", method = HttpMethod.POST,
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
        val runId = this.runId(ctx)
        val judgement = try {
            ctx.bodyAsClass(Judgement::class.java)
        } catch (e: BadRequestResponse) {
            throw ErrorStatusException(400, "Invalid parameters. This is a programmers error!")
        }

        val run = RunExecutor.managerForId(runId) ?: throw ErrorStatusException(404, "Run $runId not found")
        try {
            run.judgementValidator.judge(judgement.token, judgement.verdict)
            return SuccessStatus("Verdict received and accepted. Thanks!")
        } catch (e: IllegalArgumentException) {
            throw ErrorStatusException(404, e.message!!)
        }
    }
}