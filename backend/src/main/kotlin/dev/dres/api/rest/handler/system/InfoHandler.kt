package dev.dres.api.rest.handler.system

import com.sun.management.OperatingSystemMXBean
import dev.dres.DRES
import dev.dres.api.rest.AccessManager
import dev.dres.api.rest.RestApi
import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.system.DresInfo
import dev.dres.api.rest.types.system.SystemStatus
import dev.dres.api.rest.types.users.ApiRole
import dev.dres.utilities.extensions.sessionToken
import io.javalin.http.Context
import io.javalin.openapi.*
import java.lang.management.ManagementFactory

/** Returns general information about the DRES instance and admin-only system notices. */
class InfoHandler(
    private val systemStatusProvider: () -> List<SystemStatus> = SystemStatusRegistry::statuses
) : GetRestHandler<DresInfo> {

    override val route = "status/info"
    override val apiVersion = RestApi.LATEST_API_VERSION

    @OpenApi(
        summary = "Returns an overview of the server properties and, for administrators, system notices.",
        path = "/api/v2/status/info",
        operationId = OpenApiOperation.AUTO_GENERATE,
        methods = [HttpMethod.GET],
        tags = ["Status"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(DresInfo::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)])
        ]
    )
    override fun doGet(ctx: Context): DresInfo {
        return infoForRoles(AccessManager.rolesOfSession(ctx.sessionToken()))
    }

    internal fun infoForRoles(roles: Set<ApiRole>): DresInfo {
        val isAdmin = ApiRole.ADMIN in roles
        val runtime = ManagementFactory.getRuntimeMXBean()
        return if (isAdmin) {
            DresInfo(
                version = DRES.VERSION,
                startTime = runtime.startTime,
                uptime = runtime.uptime,
                os = System.getProperty("os.name"),
                jvm = "${System.getProperty("java.version")} (${System.getProperty("java.vendor")})",
                args = runtime.inputArguments.joinToString(),
                cores = Runtime.getRuntime().availableProcessors(),
                freeMemory = Runtime.getRuntime().freeMemory(),
                totalMemory = Runtime.getRuntime().totalMemory(),
                load = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean::class.java)?.cpuLoad ?: -1.0,
                availableSeverThreads = RestApi.readyThreadCount,
                systemStatuses = systemStatusProvider()
            )
        } else {
            DresInfo(DRES.VERSION, runtime.startTime, runtime.uptime)
        }
    }
}
