package dev.dres.api.rest.handler.evaluation.admin

import dev.dres.api.rest.AccessManager
import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.utilities.extensions.evaluationId
import dev.dres.api.rest.types.evaluation.ApiViewerInfo
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.data.model.admin.DbUser
import dev.dres.mgmt.admin.DbUserManager
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore

/**
 *
 * @author Ralph Gasser
 * @version 1.0
 */
class ListViewersHandler(store: TransientEntityStore): AbstractEvaluationAdminHandler(store), GetRestHandler<List<ApiViewerInfo>> {
    override val route: String = "evaluation/admin/{evaluationId}/viewer/list"

    @OpenApi(
        summary = "Lists all registered viewers for a evaluation. This is a method for admins.",
        path = "/api/v2/evaluation/admin/{evaluationId}/viewer/list",
        operationId = OpenApiOperation.AUTO_GENERATE,
        methods = [HttpMethod.GET],
        pathParams = [OpenApiParam("evaluationId", String::class, "The evaluation ID.", required = true, allowEmptyValue = false)],
        tags = ["Evaluation Administrator"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(Array<ApiViewerInfo>::class)]),
            OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
        ]
    )
    override fun doGet(ctx: Context): List<ApiViewerInfo> {
        val evaluationId = ctx.evaluationId()
        val evaluation = getManager(evaluationId) ?: throw ErrorStatusException(404, "Run $evaluationId not found", ctx)
        return this.store.transactional(true) {
            evaluation.viewers().map {
                ApiViewerInfo(
                    it.key.sessionToken,
                    DbUserManager.get(AccessManager.userIdForSession(it.key.sessionToken))?.username ?: "UNKNOWN",
                    it.key.host,
                    it.value)
            }
        }
    }
}
