import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.api.rest.handler.audit.AbstractAuditLogHandler
import jetbrains.exodus.database.TransientEntityStore
import dev.dres.api.rest.types.audit.AuditLogInfo
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.data.model.audit.AuditLogEntry
import io.javalin.http.Context
import io.javalin.openapi.*
import kotlinx.dnq.query.lastOrNull
import kotlinx.dnq.query.size
import kotlinx.dnq.query.sortedBy

class GetAuditLogInfoHandler(store: TransientEntityStore) : AbstractAuditLogHandler(store), GetRestHandler<AuditLogInfo> {

    override val route = "audit/info"

    @OpenApi(
            summary = "Gives information about the audit log. Namely size and latest timestamp of known entries.",
            path = "/api/v1/audit/info",
            tags = ["Audit"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(AuditLogInfo::class)], description = "The audit log info."),
                OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)], description = "Whenever a non-admin user executes the call.")
            ],
        methods = [HttpMethod.GET]
    )
    override fun doGet(ctx: Context): AuditLogInfo = this.store.transactional(true) {
        AuditLogInfo(size = AuditLogEntry.all().size(), latest = AuditLogEntry.all().sortedBy(AuditLogEntry::timestamp, true).lastOrNull()?.timestamp?.millis)
    }
}