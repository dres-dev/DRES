package dev.dres.api.rest.handler.audit

import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.api.rest.types.audit.ApiAuditLogEntry
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.data.model.audit.DbAuditLogEntry
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.*
import org.joda.time.DateTime

/**
 * [AbstractAuditLogHandler] to list all [DbAuditLogEntry] between two points in time.
 *
 * @author Loris Sauter
 * @version 1.0.0
 */
class ListAuditLogsInRangeHandler(store: TransientEntityStore): AbstractAuditLogHandler(store), GetRestHandler<List<ApiAuditLogEntry>> {

    override val route = "audit/log/list/since/{since}/{upto}"

    @OpenApi(
        summary = "Lists all audit logs matching the query",
        path = "/api/v2/audit/log/list/since/{since}/{upto}",
        pathParams = [
            OpenApiParam("since", Long::class, "Timestamp of the earliest audit log to include", required = true),
            OpenApiParam("upto", Long::class, "Timestamp of the latest audit log to include.", required = true)
        ],
        operationId = OpenApiOperation.AUTO_GENERATE,
        tags = ["Audit"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(Array<ApiAuditLogEntry>::class)], description = "The audit logs"),
            OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)], description = "Whenever a non-admin user starts the call")
        ],
        methods = [HttpMethod.GET]
    )
    override fun doGet(ctx: Context): List<ApiAuditLogEntry> {
        val since = DateTime(ctx.pathParamMap()["since"]?.toLongOrNull() ?: 0L)
        val upto = DateTime(ctx.pathParamMap()["upto"]?.toLongOrNull() ?:Long.MAX_VALUE)

        if (since < upto) throw ErrorStatusException(400, "Since must be smaller or equal to upto.", ctx)

        return this.store.transactional(true) {
            DbAuditLogEntry.query((DbAuditLogEntry::timestamp gt since) and (DbAuditLogEntry::timestamp lt upto)).asSequence().map {
                it.toApi()
            }.toList()
        }
    }
}
