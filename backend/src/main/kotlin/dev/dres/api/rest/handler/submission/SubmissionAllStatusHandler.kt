package dev.dres.api.rest.handler.submission

import dev.dres.api.rest.AccessManager
import dev.dres.api.rest.RestApi
import dev.dres.api.rest.handler.AccessManagedRestHandler
import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.api.rest.types.evaluation.submission.ApiSubmission
import dev.dres.api.rest.types.evaluation.submission.ApiSubmissionList
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.api.rest.types.users.ApiRole
import dev.dres.data.model.run.RunActionContext.Companion.runActionContext
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore

class SubmissionAllStatusHandler(private val store: TransientEntityStore) : GetRestHandler<ApiSubmissionList>,
    AccessManagedRestHandler {

    override val permittedRoles = setOf(ApiRole.PARTICIPANT)

    override val apiVersion = RestApi.LATEST_API_VERSION

    override val route = "submission/{evaluationId}/all"


    @OpenApi(
        summary = "Endpoint provide the information about all submissions of a team.",
        path = "/api/v2/submission/{evaluationId}/all",
        methods = [HttpMethod.GET],
        operationId = OpenApiOperation.AUTO_GENERATE,
        pathParams = [
            OpenApiParam(
                "evaluationId",
                String::class,
                "The ID of the evaluation the submission belongs to.",
                required = true
            ),
        ],
        queryParams = [
            OpenApiParam("session", String::class, "Session Token")
        ],
        responses = [
            OpenApiResponse(
                "200",
                [OpenApiContent(ApiSubmissionList::class)],
                description = "The submissions for this evaluation."
            ),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)]),
        ],
        tags = ["Submission"]
    )
    override fun doGet(ctx: Context): ApiSubmissionList {

        val rac = ctx.runActionContext()

        val runManager = AccessManager.getRunManagerForUser(rac.userId).find { it.id == rac.evaluationId }
            ?: throw ErrorStatusException(404, "Evaluation with ID '${rac.evaluationId}' could not be found.", ctx)

        val teamId = runManager.template.teams.singleOrNull { it.users.any { u -> u.id == rac.userId } }?.id

        if (teamId == null) {
            throw ErrorStatusException(404, "No valid team found in evaluation with ID '${rac.evaluationId}'.", ctx)
        }

        val submissions = this.store.transactional(true) {
            runManager.allSubmissions().asSequence().filter { it.teamId == teamId }.map { it.toApi() }.toList()
        }

        return ApiSubmissionList(submissions)
    }
}