package dev.dres.api.rest.handler.submission

import dev.dres.api.rest.AccessManager
import dev.dres.api.rest.RestApi
import dev.dres.api.rest.handler.AccessManagedRestHandler
import dev.dres.api.rest.handler.PostRestHandler
import dev.dres.api.rest.types.evaluation.submission.ApiClientSubmission
import dev.dres.api.rest.types.evaluation.submission.ApiVerdictStatus
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.api.rest.types.status.SuccessStatus
import dev.dres.api.rest.types.status.SuccessfulSubmissionsStatus
import dev.dres.api.rest.types.users.ApiRole
import dev.dres.data.model.run.RunActionContext.Companion.runActionContext
import dev.dres.data.model.submissions.DbSubmission
import dev.dres.data.model.submissions.DbVerdictStatus
import dev.dres.data.model.submissions.VerdictStatus
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
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.filter
import kotlinx.dnq.query.first
import kotlinx.dnq.query.firstOrNull
import kotlinx.dnq.transactional
import org.slf4j.LoggerFactory

class SubmissionHandler(private val store: TransientEntityStore) : PostRestHandler<SuccessfulSubmissionsStatus>,
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
                [OpenApiContent(SuccessfulSubmissionsStatus::class)],
                description = "The submission was accepted by the server and there was a verdict"
            ),
            OpenApiResponse(
                "202",
                [OpenApiContent(SuccessfulSubmissionsStatus::class)],
                description = "The submission was accepted by the server and there has not yet been a verdict available"
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
    override fun doPost(ctx: Context): SuccessfulSubmissionsStatus {
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


        /* Return status. */

        var correct = 0
        var wrong = 0
        var undcidable = 0
        var indeterminate = 0
        apiSubmission.answers.map { it.status() }.forEach {
            when (it) {
                VerdictStatus.CORRECT -> correct++
                VerdictStatus.WRONG -> wrong++
                VerdictStatus.INDETERMINATE -> indeterminate++
                VerdictStatus.UNDECIDABLE -> undcidable++
            }
        }
        val max = listOf(correct, wrong, undcidable, indeterminate).max()
        return when (max) {
            0 -> throw ErrorStatusException(
                500,
                "No verdict information available ${apiClientSubmission.submissionId}. This is a serious bug and should be reported!",
                ctx
            )

            correct -> SuccessfulSubmissionsStatus(ApiVerdictStatus.CORRECT, "Submission correct, well done!")
            wrong -> SuccessfulSubmissionsStatus(ApiVerdictStatus.WRONG, "Submission wrong, try again!")
            undcidable -> SuccessfulSubmissionsStatus(
                ApiVerdictStatus.UNDECIDABLE,
                "Submission undecidable, try again!"
            )

            indeterminate -> {
                ctx.status(202)
                SuccessfulSubmissionsStatus(ApiVerdictStatus.INDETERMINATE, "Submission received, awaiting verdict!")
            }

            else -> throw ErrorStatusException(
                500,
                "Error while calculating submission verdict for submission ${apiClientSubmission.submissionId}. This is a serious bug and should be reported!",
                ctx
            )
        }

    }
}
