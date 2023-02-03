package dev.dres.api.rest.handler.evaluation.admin

import dev.dres.api.rest.handler.PostRestHandler
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.api.rest.types.status.SuccessStatus
import dev.dres.data.model.audit.DbAuditLogSource
import dev.dres.data.model.run.RunActionContext
import dev.dres.run.audit.AuditLogger
import dev.dres.utilities.extensions.evaluationId
import dev.dres.utilities.extensions.sessionToken
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore

/**
 * [PostRestHandler] to start an [Evaluation].
 *
 * @author Ralph Gasser
 * @author Luca Rossetto
 * @author Loris Sauter
 * @version 2.0.0
 */
class StartEvaluationHandler(store: TransientEntityStore) : AbstractEvaluationAdminHandler(store), PostRestHandler<SuccessStatus> {
    override val route: String = "evaluation/admin/{evaluationId}/start"

    @OpenApi(
        summary = "Starts a evaluation. This is a method for administrators.",
        path = "/api/v2/evaluation/admin/{evaluationId}/start",
        methods = [HttpMethod.POST],
        pathParams = [OpenApiParam("evaluationId", String::class, "The evaluation ID.", required = true, allowEmptyValue = false)],
        tags = ["Evaluation Administrator"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
            OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)])
        ]
    )
    override fun doPost(ctx: Context): SuccessStatus {
        val evaluationId = ctx.evaluationId()
        val evaluationManager = getManager(evaluationId) ?: throw ErrorStatusException(404, "Evaluation $evaluationId not found", ctx)

        return this.store.transactional {
            val rac = RunActionContext.runActionContext(ctx, evaluationManager)
            try {
                evaluationManager.start(rac)
                AuditLogger.competitionStart(evaluationManager.id, evaluationManager.template, DbAuditLogSource.REST, ctx.sessionToken())
                SuccessStatus("Evaluation $evaluationId was successfully started.")
            } catch (e: IllegalStateException) {
                throw ErrorStatusException(400, "Evaluation $evaluationId could not be started because it is in the wrong state (state = ${evaluationManager.status}).", ctx)
            } catch (e: IllegalAccessError) {
                throw ErrorStatusException(403, e.message!!, ctx)
            }
        }
    }
}