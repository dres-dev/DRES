package dev.dres.api.rest.handler.submission

import dev.dres.api.rest.AccessManager
import dev.dres.api.rest.handler.AccessManagedRestHandler
import dev.dres.api.rest.handler.PostRestHandler
import dev.dres.api.rest.types.competition.ApiEvaluationStartMessage
import dev.dres.api.rest.types.evaluation.ApiSubmission
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.api.rest.types.status.SuccessfulSubmissionsStatus
import dev.dres.api.rest.types.users.ApiRole
import dev.dres.data.model.Config
import dev.dres.data.model.audit.DbAuditLogSource
import dev.dres.data.model.run.RunActionContext
import dev.dres.data.model.submissions.DbVerdictStatus
import dev.dres.data.model.template.task.options.DbTaskOption
import dev.dres.run.InteractiveRunManager
import dev.dres.run.NonInteractiveRunManager
import dev.dres.run.audit.AuditLogger
import dev.dres.run.exceptions.IllegalRunStateException
import dev.dres.run.exceptions.IllegalTeamIdException
import dev.dres.run.filter.SubmissionRejectedException
import dev.dres.utilities.extensions.sessionToken
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.http.bodyAsClass
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.contains
import kotlinx.dnq.query.first
import org.slf4j.LoggerFactory

class SubmissionHandler(private val store: TransientEntityStore, private val config: Config): PostRestHandler<SuccessfulSubmissionsStatus>, AccessManagedRestHandler {

    override val permittedRoles = setOf(ApiRole.PARTICIPANT)

    /** All [LegacySubmissionHandler]s are part of the v1 API. */
    override val apiVersion = "v2"

    override val route = "submit/{evaluationId}"

    private val logger = LoggerFactory.getLogger(this.javaClass)

    override fun doPost(ctx: Context): SuccessfulSubmissionsStatus {

        return this.store.transactional {

            val userId = AccessManager.userIdForSession(ctx.sessionToken()) ?: throw ErrorStatusException(401, "Authorization required.", ctx)
            val evaluationId = ctx.pathParam("evaluationId")
            val runManager = AccessManager.getRunManagerForUser(userId).find { it.id == evaluationId } ?: throw ErrorStatusException(404, "Evaluation with id '$evaluationId' not found.", ctx)

            val rac = RunActionContext.runActionContext(ctx, runManager)

            val apiSubmission = try {
                ctx.bodyAsClass<ApiSubmission>()
            } catch (e: BadRequestResponse) {
                throw ErrorStatusException(400, "Invalid submission, cannot parse: ${e.message}", ctx)
            }

            val result = try {
                runManager.postSubmission(rac, apiSubmission)
            } catch (e: SubmissionRejectedException) {
                throw ErrorStatusException(412, e.message ?: "Submission rejected by submission filter.", ctx)
            } catch (e: IllegalRunStateException) {
                logger.info("Submission was received while run manager was not accepting submissions.")
                throw ErrorStatusException(400, "Run manager is in wrong state and cannot accept any more submission.", ctx)
            } catch (e: IllegalTeamIdException) {
                logger.info("Submission with unknown team id '${rac.teamId}' was received.")
                throw ErrorStatusException(400, "Run manager does not know the given teamId ${rac.teamId}.", ctx)
            }

            AuditLogger.submission(apiSubmission, DbAuditLogSource.REST, ctx.sessionToken(), ctx.req().remoteAddr)

            logger.info("Submission ${apiSubmission.id} received status $result.")

            return@transactional when (result) {
                DbVerdictStatus.CORRECT -> SuccessfulSubmissionsStatus(DbVerdictStatus.CORRECT.toApi(), "Submission correct!")
                DbVerdictStatus.WRONG -> SuccessfulSubmissionsStatus(DbVerdictStatus.WRONG.toApi(), "Submission incorrect! Try again")
                DbVerdictStatus.INDETERMINATE -> {
                    ctx.status(202) /* HTTP Accepted. */
                    SuccessfulSubmissionsStatus(DbVerdictStatus.INDETERMINATE.toApi(), "Submission received. Waiting for verdict!")
                }

                DbVerdictStatus.UNDECIDABLE -> SuccessfulSubmissionsStatus(DbVerdictStatus.UNDECIDABLE.toApi(),"Submission undecidable. Try again!")
                else -> throw ErrorStatusException(500, "Unsupported submission status. This is very unusual!", ctx)
            }

        }

    }
}