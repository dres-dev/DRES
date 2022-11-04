package dev.dres.api.rest.handler

import dev.dres.api.rest.AccessManager
import dev.dres.api.rest.types.users.ApiRole
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.api.rest.types.status.SuccessStatus
import dev.dres.data.dbo.DAO
import dev.dres.data.model.audit.AuditLogSource
import dev.dres.data.model.media.MediaCollection
import dev.dres.data.model.media.PlayableMediaItem
import dev.dres.data.model.submissions.SubmissionStatus
import dev.dres.data.model.submissions.aspects.ItemAspect
import dev.dres.data.model.submissions.aspects.TemporalSubmissionAspect
import dev.dres.data.model.submissions.aspects.TextAspect
import dev.dres.run.RunExecutor
import dev.dres.run.RunManager
import dev.dres.run.audit.AuditLogger
import dev.dres.run.exceptions.JudgementTimeoutException
import dev.dres.run.validation.interfaces.VoteValidator
import dev.dres.utilities.extensions.UID
import dev.dres.utilities.extensions.sessionId
import io.javalin.security.RouteRole
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.openapi.*

abstract class AbstractJudgementHandler : RestHandler, AccessManagedRestHandler {
    override val permittedRoles: Set<RouteRole> = setOf(ApiRole.JUDGE)
    override val apiVersion = "v1"

    protected fun runId(ctx: Context) = ctx.pathParamMap().getOrElse("runId") {
        throw ErrorStatusException(400, "Parameter 'runId' is missing!'", ctx)
    }.UID()


    companion object {
        fun checkRunManagerAccess(ctx: Context, runManager: RunManager) {
            val userId = AccessManager.userIdForSession(ctx.sessionId()) ?: throw ErrorStatusException(
                403,
                "No valid user.",
                ctx
            )
            if (AccessManager.rolesOfSession(ctx.sessionId()).contains(ApiRole.ADMIN)) {
                return //Admins require no further check
            }
            if (userId !in runManager.template.judges) {
                throw ErrorStatusException(403, "Access denied.", ctx)
            }
        }
    }
}

data class Judgement(val token: String, val validator: String, val verdict: SubmissionStatus)

data class JudgementVote(val verdict: SubmissionStatus)

data class JudgementRequest(val token: String, val mediaType: JudgementRequestMediaType, val validator: String, val collection: String, val item: String, val taskDescription: String, val startTime: String?, val endTime: String?)

enum class JudgementRequestMediaType {
    TEXT,
    VIDEO,
    IMAGE,
}

class NextOpenJudgementHandler(val collections: DAO<MediaCollection>) : AbstractJudgementHandler(), GetRestHandler<JudgementRequest> {
    override val route = "run/{runId}/judge/next"

    @OpenApi(
            summary = "Gets the next open Submission to be judged.",
            path = "/api/v1/run/{runId}/judge/next",
            pathParams = [OpenApiParam("runId", String::class, "Run ID")],
            tags = ["Judgement"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(JudgementRequest::class)]),
                OpenApiResponse("202", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
            ],
        methods = [HttpMethod.GET]
    )
    override fun doGet(ctx: Context): JudgementRequest {
        val runId = this.runId(ctx)
        val run = RunExecutor.managerForId(runId) ?: throw ErrorStatusException(404, "Run $runId not found", ctx)

        checkRunManagerAccess(ctx, run)

        val validator = run.judgementValidators.find { it.hasOpen } ?: throw ErrorStatusException(202, "There is currently no submission awaiting judgement", ctx, true)
        val next = validator.next(ctx.sessionId()) ?: throw ErrorStatusException(202, "There is currently no submission awaiting judgement", ctx)

        val taskDescription = next.second.task?.description?.textualDescription() ?: next.second.task?.description?.name ?: "no task description available"

        if (next.second is TextAspect) {
            return JudgementRequest(next.first, JudgementRequestMediaType.TEXT, validator.id, "text", (next.second as TextAspect).text, taskDescription, null, null)
        }

        if (next.second !is ItemAspect) {
            throw ErrorStatusException(400, "Submission has neither item nor text", ctx)
        }

        val item = (next.second as ItemAspect).item

        val collection = this.collections[item.collection] ?: throw ErrorStatusException(404, "Could not find collection with id ${item.collection}", ctx)

        return if (next.second is TemporalSubmissionAspect){
            val tsa = next.second as TemporalSubmissionAspect
            // Video is assumed, due to Temporal Submission Aspect - might want to change this later
            JudgementRequest(next.first, JudgementRequestMediaType.VIDEO, validator.id, collection.id.string, tsa.item.id.string, taskDescription, tsa.start.toString(), tsa.end.toString())
        } else {
            val type = if(item is PlayableMediaItem){
                JudgementRequestMediaType.VIDEO
            } else {
                JudgementRequestMediaType.IMAGE
            }
            JudgementRequest(next.first, type, validator.id, collection.id.string, item.id.string, taskDescription, null, null)
        }
    }
}

class PostJudgementHandler : AbstractJudgementHandler(), PostRestHandler<SuccessStatus> {
    override val route = "run/{runId}/judge"

    @OpenApi(
            summary = "Returns a Judgement.",
            path = "/api/v1/run/{runId}/judge", methods = [HttpMethod.POST],
            pathParams = [OpenApiParam("runId", String::class, "Run ID")],
            requestBody = OpenApiRequestBody([OpenApiContent(Judgement::class)]),
            tags = ["Judgement"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
                OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("408", [OpenApiContent(ErrorStatus::class)], "On timeout: Judgement took too long"),
                OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doPost(ctx: Context): SuccessStatus {
        val runId = this.runId(ctx)
        val run = RunExecutor.managerForId(runId) ?: throw ErrorStatusException(404, "Run $runId not found", ctx)

        checkRunManagerAccess(ctx, run)

        val judgement = try {
            ctx.bodyAsClass(Judgement::class.java)
        } catch (e: BadRequestResponse) {
            throw ErrorStatusException(400, "Invalid parameters. This is a programmers error!", ctx)
        }

        val validator = run.judgementValidators.find { it.id == judgement.validator } ?: throw ErrorStatusException(404, "no matching task found with validator ${judgement.validator}", ctx)
        try {
            validator.judge(judgement.token, judgement.verdict)
        } catch(ex: JudgementTimeoutException) {
            throw ErrorStatusException(408, ex.message!!, ctx)
        }
        AuditLogger.judgement(run.id, validator, judgement.token, judgement.verdict, AuditLogSource.REST, ctx.sessionId())

        return SuccessStatus("Verdict ${judgement.verdict} received and accepted. Thanks!")
    }
}

class JudgementStatusHandler : GetRestHandler<List<JudgementValidatorStatus>>, AccessManagedRestHandler {
    override val permittedRoles = setOf(ApiRole.VIEWER)
    override val route = "run/{runId}/judge/status"
    override val apiVersion = "v1"


    @OpenApi(
            summary = "Gets the status of all judgement validators.",
            path = "/api/v1/run/{runId}/judge/status",
            pathParams = [OpenApiParam("runId", String::class, "Run ID")],
            tags = ["Judgement"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(Array<JudgementValidatorStatus>::class)]),
                OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
            ],
        methods = [HttpMethod.GET]
    )
    override fun doGet(ctx: Context): List<JudgementValidatorStatus> {

        val runId = ctx.pathParamMap().getOrElse("runId") {
            throw ErrorStatusException(400, "Parameter 'runId' is missing!'", ctx)
        }.UID()

        val run = RunExecutor.managerForId(runId) ?: throw ErrorStatusException(404, "Run $runId not found", ctx)

        AbstractJudgementHandler.checkRunManagerAccess(ctx, run)

        return run.judgementValidators.map { JudgementValidatorStatus(it.id, it.pending, it.open) }
    }

}

data class JudgementValidatorStatus(val validator: String, val pending: Int, val open: Int)

class JudgementVoteHandler : PostRestHandler<SuccessStatus> {
    override val route = "run/{runId}/judge/vote"
    override val apiVersion = "v1"


    @OpenApi(
        summary = "Returns a Vote.",
        path = "/api/v1/run/{runId}/judge/vote", methods = [HttpMethod.POST],
        pathParams = [OpenApiParam("runId", String::class, "Run ID")],
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
    override val route = "run/{runId}/vote/next"

    @OpenApi(
        summary = "Gets the next open Submission to voted on.",
        path = "/api/v1/run/{runId}/vote/next",
        pathParams = [OpenApiParam("runId", String::class, "Run ID")],
        tags = ["Judgement"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(JudgementRequest::class)]),
            OpenApiResponse("202", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
        ],
        methods = [HttpMethod.GET]
    )
    override fun doGet(ctx: Context): JudgementRequest {
        val runId = this.runId(ctx)
        val run = RunExecutor.managerForId(runId) ?: throw ErrorStatusException(404, "Run $runId not found", ctx)

        val validator = run.judgementValidators.find { it is VoteValidator && it.isActive } ?: throw ErrorStatusException(202, "There is currently no voting going on in run", ctx, true)

        validator as VoteValidator

        val next = validator.nextSubmissionToVoteOn() ?: throw ErrorStatusException(202, "There is currently no voting going on in run", ctx)

        val taskDescription = next.task?.description?.textualDescription() ?: next.task?.description?.name ?: "no task description available"

        if (next is TextAspect) {
            return JudgementRequest("vote", JudgementRequestMediaType.TEXT, validator.id, "text", (next as TextAspect).text, taskDescription, null, null)
        }

        if (next !is ItemAspect) {
            throw ErrorStatusException(400, "Submission has neither item nor text", ctx)
        }

        val item = (next as ItemAspect).item

        val collection = this.collections[next.item.collection] ?: throw ErrorStatusException(404, "Could not find collection with id ${next.item.collection}", ctx)

        return if (next is TemporalSubmissionAspect){
            val tsa = next as TemporalSubmissionAspect
            JudgementRequest("vote",JudgementRequestMediaType.VIDEO, validator.id, collection.id.string, tsa.item.id.string, taskDescription, tsa.start.toString(), tsa.end.toString())
        } else {
            val type = if(item is PlayableMediaItem){
                JudgementRequestMediaType.VIDEO
            } else {
                JudgementRequestMediaType.IMAGE
            }
            JudgementRequest("vote",type, validator.id, collection.id.string, next.item.id.string, taskDescription, null, null)
        }
    }
}
