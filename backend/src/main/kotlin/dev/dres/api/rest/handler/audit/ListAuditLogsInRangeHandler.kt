package dev.dres.api.rest.handler.audit

import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.api.rest.types.audit.RestAuditLogEntry
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.data.model.audit.AuditLogEntry
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.*
import org.joda.time.DateTime

/**
 * [AbstractAuditLogHandler] to list all [AuditLogEntry] between two points in time.
 *
 * @author Loris Sauter
 * @version 1.0.0
 */
class ListAuditLogsInRangeHandler(store: TransientEntityStore): AbstractAuditLogHandler(store), GetRestHandler<List<RestAuditLogEntry>> {

    override val route = "audit/log/list/since/{since}/{upto}"

    @OpenApi(
        summary = "Lists all audit logs matching the query",
        path = "/api/v1/audit/log/list/since/{since}/{upto}",
        pathParams = [
            OpenApiParam("since", Long::class, "Timestamp of the earliest audit log to include"),
            OpenApiParam("upto", Long::class, "Timestamp of the latest audit log to include.")
        ],
        tags = ["Audit"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(Array<RestAuditLogEntry>::class)], description = "The audit logs"),
            OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)], description = "Whenever a non-admin user starts the call")
        ],
        methods = [HttpMethod.GET]
    )
    override fun doGet(ctx: Context): List<RestAuditLogEntry> {
        val since = DateTime(ctx.pathParamMap()["since"]?.toLongOrNull() ?: 0L)
        val upto = DateTime(ctx.pathParamMap()["upto"]?.toLongOrNull() ?:Long.MAX_VALUE)

        if (since < upto) throw ErrorStatusException(400, "Since must be smaller or equal to upto.", ctx)

        return this.store.transactional(true) {
            AuditLogEntry.query((AuditLogEntry::timestamp gt since) and (AuditLogEntry::timestamp lt upto)).asSequence().map {
                RestAuditLogEntry.convert(it)
            }.toList()
        }
    }
}
