package dev.dres.api.rest.handler.submission

import dev.dres.api.rest.AccessManager
import dev.dres.api.rest.RestApi
import dev.dres.api.rest.handler.AccessManagedRestHandler
import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.api.rest.types.evaluation.submission.ApiSubmission
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.api.rest.types.users.ApiRole
import dev.dres.data.model.run.RunActionContext.Companion.runActionContext
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore

class SubmissionStatusHandler(private val store: TransientEntityStore) : GetRestHandler<ApiSubmission>,
    AccessManagedRestHandler {

    override val permittedRoles = setOf(ApiRole.PARTICIPANT)

    override val apiVersion = RestApi.LATEST_API_VERSION

    override val route = "submission/{evaluationId}/{submissionId}"


    @OpenApi(
        summary = "Endpoint provide the information about a given submission.",
        path = "/api/v2/submission/{evaluationId}/{submissionId}",
        methods = [HttpMethod.GET],
        operationId = OpenApiOperation.AUTO_GENERATE,
        pathParams = [
            OpenApiParam(
                "evaluationId",
                String::class,
                "The ID of the evaluation the submission belongs to.",
                required = true
            ),
            OpenApiParam(
                "submissionId",
                String::class,
                "The ID of the submission.",
                required = true
            ),
        ],
        queryParams = [
            OpenApiParam("session", String::class, "Session Token")
        ],
        responses = [
            OpenApiResponse(
                "200",
                [OpenApiContent(ApiSubmission::class)],
                description = "The submission."
            ),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)]),
        ],
        tags = ["Submission"]
    )
    override fun doGet(ctx: Context): ApiSubmission {

        val rac = ctx.runActionContext()

        val runManager = AccessManager.getRunManagerForUser(rac.userId).find { it.id == rac.evaluationId }
            ?: throw ErrorStatusException(404, "Evaluation with ID '${rac.evaluationId}' could not be found.", ctx)

        val submissionId = ctx.pathParam("submissionId")

        val submission = this.store.transactional(true) {
            runManager.allSubmissions().find { it.id == submissionId }?.toApi()
        }

        if (submission == null) {
            throw ErrorStatusException(404, "Submission with ID '${submissionId}' not found.", ctx)
        }

        val teamId = runManager.template.teams.singleOrNull { it.users.any { u -> u.id == rac.userId } }?.id

        if (submission.teamId != teamId) {
            throw ErrorStatusException(404, "No valid submission found", ctx)
        }

        return submission
    }
}