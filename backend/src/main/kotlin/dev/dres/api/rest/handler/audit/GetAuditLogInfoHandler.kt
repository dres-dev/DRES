import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.api.rest.handler.audit.AbstractAuditLogHandler
import jetbrains.exodus.database.TransientEntityStore
import dev.dres.api.rest.types.audit.AuditLogInfo
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.data.model.audit.DbAuditLogEntry
import io.javalin.http.Context
import io.javalin.openapi.*
import kotlinx.dnq.query.lastOrNull
import kotlinx.dnq.query.size
import kotlinx.dnq.query.sortedBy

class GetAuditLogInfoHandler(store: TransientEntityStore) : AbstractAuditLogHandler(store), GetRestHandler<AuditLogInfo> {

    override val route = "audit/info"

    @OpenApi(
            summary = "Gives information about the audit log. Namely size and latest timestamp of known entries.",
            path = "/api/v2/audit/info",
            tags = ["Audit"],
        operationId = OpenApiOperation.AUTO_GENERATE,
            responses = [
                OpenApiResponse("200", [OpenApiContent(AuditLogInfo::class)], description = "The audit log info."),
                OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)], description = "Whenever a non-admin user executes the call.")
            ],
        methods = [HttpMethod.GET]
    )
    override fun doGet(ctx: Context): AuditLogInfo = this.store.transactional(true) {
        AuditLogInfo(size = DbAuditLogEntry.all().size(), latest = DbAuditLogEntry.all().sortedBy(DbAuditLogEntry::timestamp, true).lastOrNull()?.timestamp?.millis)
    }
}
