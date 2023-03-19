package dev.dres.api.rest.handler.submission

import dev.dres.api.rest.AccessManager
import dev.dres.api.rest.handler.AccessManagedRestHandler
import dev.dres.api.rest.handler.PostRestHandler
import dev.dres.api.rest.types.evaluation.ApiSubmission
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.api.rest.types.status.SuccessStatus
import dev.dres.api.rest.types.users.ApiRole
import dev.dres.data.model.config.Config
import dev.dres.data.model.audit.DbAuditLogSource
import dev.dres.data.model.run.RunActionContext
import dev.dres.run.audit.DbAuditLogger
import dev.dres.run.exceptions.IllegalRunStateException
import dev.dres.run.exceptions.IllegalTeamIdException
import dev.dres.run.filter.SubmissionRejectedException
import dev.dres.utilities.extensions.sessionToken
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.http.bodyAsClass
import jetbrains.exodus.database.TransientEntityStore
import org.slf4j.LoggerFactory

class SubmissionHandler(private val store: TransientEntityStore, private val config: Config): PostRestHandler<SuccessStatus>, AccessManagedRestHandler {

    override val permittedRoles = setOf(ApiRole.PARTICIPANT)

    /** All [LegacySubmissionHandler]s are part of the v1 API. */
    override val apiVersion = "v2"

    override val route = "submit/{evaluationId}"

    private val logger = LoggerFactory.getLogger(this.javaClass)

    override fun doPost(ctx: Context): SuccessStatus {

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

            try {
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

            DbAuditLogger.submission(apiSubmission, DbAuditLogSource.REST, ctx.sessionToken(), ctx.req().remoteAddr)


            return@transactional SuccessStatus("Submission received")


        }

    }
}