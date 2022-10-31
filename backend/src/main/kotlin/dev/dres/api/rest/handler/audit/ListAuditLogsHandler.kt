package dev.dres.api.rest.handler.audit

import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.api.rest.types.audit.ApiAuditLogEntry
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.data.model.audit.AuditLogEntry
import dev.dres.utilities.extensions.toPathParamKey
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.asSequence
import kotlinx.dnq.query.drop
import kotlinx.dnq.query.take

/**
 * [AbstractAuditLogHandler] to list all [AuditLogEntry]. Allows for pagination.
 *
 * @author Loris Sauter
 * @version 1.0.0
 */
class ListAuditLogsHandler(store: TransientEntityStore) : AbstractAuditLogHandler(store), GetRestHandler<List<ApiAuditLogEntry>> {
    override val route = "audit/log/list/limit/${LIMIT_PARAM.toPathParamKey()}/${PAGE_INDEX_PARAM.toPathParamKey()}"

    companion object {
        const val LIMIT_PARAM = "limit"
        const val PAGE_INDEX_PARAM = "page"
        const val DEFAULT_LIMIT = 500

        const val TYPE_PARAM = "type" // Coupled to 1047, not yet ready
        const val RELATION_PARAM = "when"
        const val BEFORE_PARAM_KEY = "before"
        const val UPTO_PARAM_KEY = "upto"
    }

    @OpenApi(
        summary = "Lists all audit logs matching the query.",
        path = "/api/v1/audit/log/list/limit/{limit}/{page}",
        pathParams = [
            OpenApiParam(LIMIT_PARAM, Int::class, "The maximum number of results. Default: 500"),
            OpenApiParam(PAGE_INDEX_PARAM, Int::class, "The page index offset, relative to the limit.")
        ],
        tags = ["Audit"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(Array<ApiAuditLogEntry>::class)], description = "The audit logs"),
            OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)], description = "Whenever a non-admin user starts the call")
        ],
        methods = [HttpMethod.GET]
    )
    override fun doGet(ctx: Context): List<ApiAuditLogEntry> = this.store.transactional(true) {
        val limit = (ctx.pathParamMap()[LIMIT_PARAM]?.toIntOrNull() ?: DEFAULT_LIMIT).coerceAtLeast(0)
        val index = (ctx.pathParamMap()[PAGE_INDEX_PARAM]?.toIntOrNull() ?: 0).coerceAtLeast(0)
        AuditLogEntry.all().drop(index * limit).take(limit).asSequence().map { it.toApi() }.toList()
    }
}