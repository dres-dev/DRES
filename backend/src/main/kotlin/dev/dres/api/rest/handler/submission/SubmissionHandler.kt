package dev.dres.api.rest.handler.submission

import dev.dres.api.rest.AccessManager
import dev.dres.api.rest.RestApi
import dev.dres.api.rest.handler.AccessManagedRestHandler
import dev.dres.api.rest.handler.PostRestHandler
import dev.dres.api.rest.types.evaluation.submission.ApiClientSubmission
import dev.dres.api.rest.types.evaluation.submission.ApiSubmission
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.api.rest.types.users.ApiRole
import dev.dres.data.model.run.RunActionContext.Companion.runActionContext
import dev.dres.run.audit.AuditLogSource
import dev.dres.run.audit.AuditLogger
import dev.dres.run.exceptions.IllegalRunStateException
import dev.dres.run.exceptions.IllegalTeamIdException
import dev.dres.run.filter.SubmissionRejectedException
import dev.dres.utilities.extensions.sessionToken
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.http.bodyAsClass
import io.javalin.openapi.*
import org.slf4j.LoggerFactory

class SubmissionHandler : PostRestHandler<ApiSubmission>,
    AccessManagedRestHandler {

    override val permittedRoles = setOf(ApiRole.PARTICIPANT)

    override val apiVersion = RestApi.LATEST_API_VERSION

    override val route = "submit/{evaluationId}"

    private val logger = LoggerFactory.getLogger(this.javaClass)

    @OpenApi(
        summary = "Endpoint to accept submissions.",
        path = "/api/v2/submit/{evaluationId}",
        methods = [HttpMethod.POST],
        operationId = OpenApiOperation.AUTO_GENERATE,
        requestBody = OpenApiRequestBody([OpenApiContent(ApiClientSubmission::class)], required = true,
            description =
               "Some notes regarding the submission format. " +
               "At least one answerSet is required, taskId, taskName are inferred if not provided," +
               "  at least one answer is required, mediaItemCollectionName is inferred if not provided," +
               "  start and end should be provided in milliseconds." +
               "For most evaluation setups, an answer is built in one of the three following ways:" +
               " A) only text is required: just provide the text property with a meaningful entry" +
               " B) only a mediaItemName is required: just provide the mediaItemName, optionally with the collection name." +
               " C) a specific portion of a mediaItem is required: provide mediaItemName, start and end, optionally with collection name"
            ),
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
                [OpenApiContent(ApiSubmission::class)],
                description = "The submission that was received."
            ),
            OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse(
                "412",
                [OpenApiContent(ErrorStatus::class)],
                description = "The submission was rejected by the server"
            )
        ],
        tags = ["Submission"]
    )
    override fun doPost(ctx: Context): ApiSubmission {
        /* Obtain run action context and parse submission. */
        val rac = ctx.runActionContext()
        val apiClientSubmission = try {
            ctx.bodyAsClass<ApiClientSubmission>()
        } catch (e: BadRequestResponse) {
            throw ErrorStatusException(400, "Invalid submission, cannot parse: ${e.message}", ctx)
        }

        /* Obtain run action context and run manager. */
        val runManager = AccessManager.getRunManagerForUser(rac.userId).find { it.id == rac.evaluationId }
            ?: throw ErrorStatusException(404, "Evaluation with ID '${rac.evaluationId}' could not be found.", ctx)

        /* Post submission. */
        val apiSubmission = try {
            runManager.postSubmission(rac, apiClientSubmission)
        } catch (e: SubmissionRejectedException) {
            logger.info("Submission was rejected by submission filter.")
            throw ErrorStatusException(412, e.message ?: "Submission rejected by submission filter.", ctx)
        } catch (e: IllegalRunStateException) {
            logger.info("Submission was received while run manager was not accepting submissions.")
            throw ErrorStatusException(400, "Run manager is in wrong state and cannot accept any more submission.", ctx)
        } catch (e: IllegalTeamIdException) {
            logger.info("Submission with unknown team id '${apiClientSubmission.teamId}' was received.")
            throw ErrorStatusException(
                400,
                "Run manager does not know the given teamId ${apiClientSubmission.teamId}.",
                ctx
            )
        } finally {
            AuditLogger.submission(
                apiClientSubmission,
                rac.evaluationId!!,
                AuditLogSource.REST,
                ctx.sessionToken(),
                ctx.ip()
            )
        }

        return apiSubmission

    }
}
