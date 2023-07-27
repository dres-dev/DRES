package dev.dres.api.rest.handler.submission

import dev.dres.api.rest.AccessManager
import dev.dres.api.rest.handler.AccessManagedRestHandler
import dev.dres.api.rest.handler.PostRestHandler
import dev.dres.api.rest.types.evaluation.submission.ApiClientSubmission
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.api.rest.types.status.SuccessStatus
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

class SubmissionHandler: PostRestHandler<SuccessStatus>, AccessManagedRestHandler {

    override val permittedRoles = setOf(ApiRole.PARTICIPANT)

    override val apiVersion = "v2"

    override val route = "submit/{evaluationId}"

    private val logger = LoggerFactory.getLogger(this.javaClass)

    @OpenApi(
        summary = "Endpoint to accept submissions.",
        path = "/api/v2/submit/{evaluationId}",
        methods = [HttpMethod.POST],
        operationId = OpenApiOperation.AUTO_GENERATE,
        requestBody = OpenApiRequestBody([OpenApiContent(ApiClientSubmission::class)]),
        pathParams = [
            OpenApiParam("evaluationId", String::class, "The ID of the evaluation the submission belongs to.", required = true),
        ],
        responses = [
            OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
            OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("412", [OpenApiContent(ErrorStatus::class)])
        ],
        tags = ["Submission"]
    )
    override fun doPost(ctx: Context): SuccessStatus {
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
        try {
            runManager.postSubmission(rac, apiClientSubmission)
        } catch (e: SubmissionRejectedException) {
            throw ErrorStatusException(412, e.message ?: "Submission rejected by submission filter.", ctx)
        } catch (e: IllegalRunStateException) {
            logger.info("Submission was received while run manager was not accepting submissions.")
            throw ErrorStatusException(400, "Run manager is in wrong state and cannot accept any more submission.", ctx)
        } catch (e: IllegalTeamIdException) {
            logger.info("Submission with unknown team id '${apiClientSubmission.teamId}' was received.")
            throw ErrorStatusException(400, "Run manager does not know the given teamId ${apiClientSubmission.teamId}.", ctx)
        } finally {
            AuditLogger.submission(apiClientSubmission, rac.evaluationId!!, AuditLogSource.REST, ctx.sessionToken(), ctx.ip())
        }

        /* Return status. */
        return SuccessStatus("Submission received.")
    }
}