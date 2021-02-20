package dev.dres.api.rest.handler

import dev.dres.api.rest.RestApiRole
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.api.rest.types.status.SuccessStatus
import dev.dres.data.dbo.DAO
import dev.dres.data.model.basics.media.MediaCollection
import dev.dres.data.model.run.SubmissionStatus
import dev.dres.data.model.run.TemporalSubmissionAspect
import dev.dres.run.RunExecutor
import dev.dres.run.audit.AuditLogger
import dev.dres.run.audit.LogEventSource
import dev.dres.run.validation.interfaces.VoteValidator
import dev.dres.utilities.extensions.UID
import dev.dres.utilities.extensions.sessionId
import io.javalin.core.security.Role
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.plugin.openapi.annotations.*

abstract class AbstractJudgementHandler : RestHandler, AccessManagedRestHandler {
    override val permittedRoles: Set<Role> = setOf(RestApiRole.JUDGE)

    protected fun runId(ctx: Context) = ctx.pathParamMap().getOrElse("runId") {
        throw ErrorStatusException(400, "Parameter 'runId' is missing!'", ctx)
    }.UID()
}

data class Judgement(val token: String, val validator: String, val verdict: SubmissionStatus)

data class JudgementVote(val verdict: SubmissionStatus)

data class JudgementRequest(val token: String, val validator: String, val collection: String, val item: String, val taskDescription: String, val startTime: String?, val endTime: String?)

class NextOpenJudgementHandler(val collections: DAO<MediaCollection>) : AbstractJudgementHandler(), GetRestHandler<JudgementRequest> {
    override val route = "run/:runId/judge/next"

    @OpenApi(
            summary = "Gets the next open Submission to be judged.",
            path = "/api/run/:runId/judge/next",
            pathParams = [OpenApiParam("runId", dev.dres.data.model.UID::class, "Run ID")],
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
        val run = RunExecutor.managerForId(runId) ?: throw ErrorStatusException(404, "Run $runId not found", ctx)

        val validator = run.judgementValidators.find { it.hasOpen } ?: throw ErrorStatusException(202, "There is currently no submission awaiting judgement", ctx, true)
        val next = validator.next(ctx.sessionId()) ?: throw ErrorStatusException(202, "There is currently no submission awaiting judgement", ctx)

        val collection = this.collections[next.second.item.collection] ?: throw ErrorStatusException(404, "Could not find collection with id ${next.second.item.collection}", ctx)

        val taskDescription = next.second.task()?.taskDescription?.textualDescription() ?: next.second.task()?.taskDescription?.name ?: "no task description available"

        return if (next.second is TemporalSubmissionAspect){
            val tsa = next.second as TemporalSubmissionAspect
            JudgementRequest(next.first, validator.id, collection.id.string, tsa.item.id.string, taskDescription, tsa.start.toString(), tsa.end.toString())
        } else {
            JudgementRequest(next.first, validator.id, collection.id.string, next.second.item.id.string, taskDescription, null, null)
        }
    }
}

class PostJudgementHandler : AbstractJudgementHandler(), PostRestHandler<SuccessStatus> {
    override val route = "run/:runId/judge"

    @OpenApi(
            summary = "Returns a Judgement.",
            path = "/api/run/:runId/judge", method = HttpMethod.POST,
            pathParams = [OpenApiParam("runId", dev.dres.data.model.UID::class, "Run ID")],
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
        val run = RunExecutor.managerForId(runId) ?: throw ErrorStatusException(404, "Run $runId not found", ctx)
        val judgement = try {
            ctx.bodyAsClass(Judgement::class.java)
        } catch (e: BadRequestResponse) {
            throw ErrorStatusException(400, "Invalid parameters. This is a programmers error!", ctx)
        }

        val validator = run.judgementValidators.find { it.id == judgement.validator } ?: throw ErrorStatusException(404, "no matching task found with validator ${judgement.validator}", ctx)

        validator.judge(judgement.token, judgement.verdict)

        AuditLogger.judgement(run.id, judgement.validator, judgement.token, judgement.verdict, LogEventSource.REST, ctx.sessionId())

        return SuccessStatus("Verdict received and accepted. Thanks!")
    }
}

class JudgementStatusHandler : GetRestHandler<List<JudgementValidatorStatus>>, AccessManagedRestHandler {
    override val permittedRoles = setOf(RestApiRole.VIEWER)
    override val route = "run/:runId/judge/status"


    @OpenApi(
            summary = "Gets the status of all judgement validators.",
            path = "/api/run/:runId/judge/status",
            pathParams = [OpenApiParam("runId", dev.dres.data.model.UID::class, "Run ID")],
            tags = ["Judgement"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(Array<JudgementValidatorStatus>::class)]),
                OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doGet(ctx: Context): List<JudgementValidatorStatus> {

        val runId = ctx.pathParamMap().getOrElse("runId") {
            throw ErrorStatusException(400, "Parameter 'runId' is missing!'", ctx)
        }.UID()

        val run = RunExecutor.managerForId(runId) ?: throw ErrorStatusException(404, "Run $runId not found", ctx)

        return run.judgementValidators.map { JudgementValidatorStatus(it.id, it.pending, it.open) }
    }

}

data class JudgementValidatorStatus(val validator: String, val pending: Int, val open: Int)

class JudgementVoteHandler : PostRestHandler<SuccessStatus> {
    override val route = "run/:runId/judge/vote"


    @OpenApi(
        summary = "Returns a Vote.",
        path = "/api/run/:runId/judge/vote", method = HttpMethod.POST,
        pathParams = [OpenApiParam("runId", dev.dres.data.model.UID::class, "Run ID")],
        requestBody = OpenApiRequestBody([OpenApiContent(JudgementVote::class)]),
        tags = ["Judgement"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
            OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
        ]
    )
    override fun doPost(ctx: Context): SuccessStatus {

        val runId = try{
            ctx.pathParamMap().getOrElse("runId") {
                throw ErrorStatusException(400, "Parameter 'runId' is missing!'", ctx)
            }.UID()
        } catch (e: IllegalArgumentException) {
            throw ErrorStatusException(400, "Parameter 'runId' is invalid!'", ctx)
        }
        val run = RunExecutor.managerForId(runId) ?: throw ErrorStatusException(404, "Run $runId not found", ctx)
        val vote = try {
            ctx.bodyAsClass(JudgementVote::class.java)
        } catch (e: BadRequestResponse) {
            throw ErrorStatusException(400, "Invalid parameters. This is a programmers error!", ctx)
        }

        //gets the first active VoteValidator
        val validator = run.judgementValidators.find { it is VoteValidator && it.isActive } ?: throw ErrorStatusException(404, "There is currently no voting going on in run $runId", ctx)
        validator as VoteValidator
        validator.vote(vote.verdict)

        return SuccessStatus("vote received")
    }

}

class NextOpenVoteJudgementHandler(val collections: DAO<MediaCollection>) : AbstractJudgementHandler(), GetRestHandler<JudgementRequest> {
    override val route = "run/:runId/vote/next"

    @OpenApi(
        summary = "Gets the next open Submission to voted on.",
        path = "/api/run/:runId/vote/next",
        pathParams = [OpenApiParam("runId", dev.dres.data.model.UID::class, "Run ID")],
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
        val run = RunExecutor.managerForId(runId) ?: throw ErrorStatusException(404, "Run $runId not found", ctx)

        val validator = run.judgementValidators.find { it is VoteValidator && it.isActive } ?: throw ErrorStatusException(202, "There is currently no voting going on in run", ctx, true)

        validator as VoteValidator

        val next = validator.nextSubmissionToVoteOn() ?: throw ErrorStatusException(202, "There is currently no voting going on in run", ctx)

        val collection = this.collections[next.item.collection] ?: throw ErrorStatusException(404, "Could not find collection with id ${next.item.collection}", ctx)

        val taskDescription = next.task?.taskDescription?.textualDescription() ?: next.task?.taskDescription?.name ?: "no task description available"

        return if (next is TemporalSubmissionAspect){
            val tsa = next as TemporalSubmissionAspect
            JudgementRequest("vote", validator.id, collection.id.string, tsa.item.id.string, taskDescription, tsa.start.toString(), tsa.end.toString())
        } else {
            JudgementRequest("vote", validator.id, collection.id.string, next.item.id.string, taskDescription, null, null)
        }
    }
}