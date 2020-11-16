package dev.dres.api.rest.handler

import dev.dres.api.rest.RestApiRole
import dev.dres.api.rest.types.audit.AuditLogInfo
import dev.dres.api.rest.types.audit.RestAuditLogEntry
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.data.dbo.DAO
import dev.dres.data.dbo.NumericDaoIndexer
import dev.dres.run.audit.AuditLogEntry
import dev.dres.utilities.extensions.toPathParamKey
import io.javalin.core.security.Role
import io.javalin.http.Context
import io.javalin.plugin.openapi.annotations.OpenApi
import io.javalin.plugin.openapi.annotations.OpenApiContent
import io.javalin.plugin.openapi.annotations.OpenApiParam
import io.javalin.plugin.openapi.annotations.OpenApiResponse

abstract class AuditLogHandler(val auditTimes: NumericDaoIndexer<AuditLogEntry, Long>) : RestHandler, AccessManagedRestHandler {
    override val permittedRoles: Set<Role> = setOf(RestApiRole.ADMIN)

}

class GetAuditLogInfoHandler(auditTimes: NumericDaoIndexer<AuditLogEntry, Long>) : AuditLogHandler(auditTimes), GetRestHandler<AuditLogInfo> {

    override val route = "audit/info"

    @OpenApi(
            summary = "Gives information about the audit log. Namely size and latest timestamp of known audit logs",
            path = "/api/audit/info",
            tags = ["Audit"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(AuditLogInfo::class)], description = "The audit log info"),
                OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)], description = "Whenever a non-admin user starts the call")
            ]
    )
    override fun doGet(ctx: Context): AuditLogInfo {
        return AuditLogInfo(size = auditTimes.index.size, latest = auditTimes.index.keys.maxOrNull() ?: 0L)
    }
}

class LatestAuditLogHandler(auditTimes: NumericDaoIndexer<AuditLogEntry, Long>) : AuditLogHandler(auditTimes), GetRestHandler<Array<RestAuditLogEntry>> {

    override val route = "audit/latest"

    @OpenApi(
            summary = "Gives the latest audit log entires",
            path = "/api/audit/latest",
            tags = ["Audit"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(Array<AuditLogEntry>::class)], description = "The latest audit logs"),
                OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)], description = "Whenever a non-admin user starts the call")
            ]
    )
    override fun doGet(ctx: Context): Array<RestAuditLogEntry> {
        val latest = auditTimes.index.keys.maxOrNull()
        return if (latest != null) {
            auditTimes[latest].map{RestAuditLogEntry.convert(it)}.toTypedArray()
        } else {
            emptyArray()
        }
    }
}

class ListAuditLogsHandler(auditTimes: NumericDaoIndexer<AuditLogEntry, Long>, val audit: DAO<AuditLogEntry>) : AuditLogHandler(auditTimes), GetRestHandler<Array<RestAuditLogEntry>> {

    override val route = "audit/list/${LIMIT_PARAM.toPathParamKey()}/${PAGE_INDEX_PARAM.toPathParamKey()}"

    companion object {
        const val LIMIT_PARAM = "limit"
        const val PAGE_INDEX_PARAM = "page"
        const val DEFAULT_LIMIT = 500

        const val TYPE_PARAM = "type" // Coupled to 1047, not yet ready
        const val RELATION_PARAM = "when"
        const val BEFORE_PARAM_KEY = "before"
        const val UPTO_PARAM_KEY = "upto"
        /*
        // See https://github.com/tipsy/javalin/issues/1047 - we currently are behind
        enum class TemporalRelation{
            BEFORE,
            UPTO
        }*/
    }

    @OpenApi(
            summary = "Lists all audit logs matching the query",
            path = "/api/audit/list/:limit/:page",
            pathParams = [
                OpenApiParam(LIMIT_PARAM, Int::class, "The maximum number of results. Default: 500"),
                OpenApiParam(PAGE_INDEX_PARAM, Int::class, "The page index offset, relative to the limit")
            ],
            tags = ["Audit"],
            responses = [
//                OpenApiResponse("200", [OpenApiContent(AuditLogPage::class)], description = "The audit logs"),
                OpenApiResponse("200", [OpenApiContent(Array<RestAuditLogEntry>::class)], description = "The audit logs"),
                OpenApiResponse("403", [OpenApiContent(ErrorStatus::class)], description = "Whenever a non-admin user starts the call")
            ]
    )
    override fun doGet(ctx: Context): Array<RestAuditLogEntry> {
        val limit = getLimitFromParams(ctx)
        val index = getIndexFromParams(ctx)


        val list = auditTimes.index.toSortedMap { o1, o2 -> o2.compareTo(o1) }.values.flatten()
        return list.mapNotNull { RestAuditLogEntry.convert(audit[it]!!) }.toTypedArray() // Null safety is kotlin steered. should not contain null elements, as these UIDS come from the idnexer.
    }

    private fun getLimitFromParams(ctx:Context): Int {
        return ctx.pathParam(LIMIT_PARAM).let { l ->
            try {
                return@let l.toInt().let { i ->
                    return@let if (i <= 0) {
                        DEFAULT_LIMIT
                    } else {
                        i
                    }
                }
            } catch (e: NumberFormatException) {
                return@let DEFAULT_LIMIT
            }
        }
    }

    private fun getIndexFromParams(ctx:Context):Int{
        return ctx.pathParam(PAGE_INDEX_PARAM).let{i ->
            try{
                return@let i.toInt().let{
                    return@let if(it <= 0){
                        0
                    }else{
                        it
                    }
                }
            }catch(e: NumberFormatException){
                return@let 0
            }
        }
    }
}