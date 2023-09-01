package dev.dres.api.rest.handler.system

import com.sun.management.OperatingSystemMXBean
import dev.dres.DRES
import dev.dres.api.rest.AccessManager
import dev.dres.api.rest.types.users.ApiRole
import dev.dres.api.rest.RestApi
import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.system.DresInfo
import dev.dres.utilities.extensions.sessionToken
import io.javalin.http.Context
import io.javalin.openapi.*
import java.lang.management.ManagementFactory

/**
 * A [GetRestHandler] that returns general information about the DRES instance.
 *
 * @author Luca Rossetto
 * @version 1.0.0
 */
class InfoHandler : GetRestHandler<DresInfo> {

    override val route = "status/info"

    override val apiVersion = RestApi.LATEST_API_VERSION


    @OpenApi(summary = "Returns an overview of the server properties.",
        path = "/api/v2/status/info",
        operationId = OpenApiOperation.AUTO_GENERATE,
        methods = [HttpMethod.GET],
        tags = ["Status"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(DresInfo::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)])
        ])
    override fun doGet(ctx: Context): DresInfo {
       return if (AccessManager.rolesOfSession(ctx.sessionToken()).contains(ApiRole.ADMIN)) {
            DresInfo (
                DRES.VERSION,
                ManagementFactory.getRuntimeMXBean().startTime,
                ManagementFactory.getRuntimeMXBean().uptime,
                System.getProperty("os.name"),
                "${System.getProperty("java.version")} (${System.getProperty("java.vendor")})",
                ManagementFactory.getRuntimeMXBean().inputArguments.joinToString(),
                Runtime.getRuntime().availableProcessors(),
                Runtime.getRuntime().freeMemory(),
                Runtime.getRuntime().totalMemory(),
                ManagementFactory.getPlatformMXBean(OperatingSystemMXBean::class.java)?.systemCpuLoad ?: -1.0,
                RestApi.readyThreadCount)
        } else {
            DresInfo (DRES.VERSION, ManagementFactory.getRuntimeMXBean().startTime, ManagementFactory.getRuntimeMXBean().uptime)
        }
    }
}
