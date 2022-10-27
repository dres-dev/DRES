package dev.dres.api.rest.handler

import com.sun.management.OperatingSystemMXBean
import dev.dres.DRES
import dev.dres.api.rest.RestApi
import dev.dres.api.rest.RestApiRole
import dev.dres.api.rest.types.status.ErrorStatus
import io.javalin.http.Context
import io.javalin.openapi.HttpMethod
import io.javalin.openapi.OpenApi
import io.javalin.openapi.OpenApiContent
import io.javalin.openapi.OpenApiResponse
import io.javalin.security.RouteRole
import java.lang.management.ManagementFactory

data class CurrentTime(val timeStamp: Long = System.currentTimeMillis())

class CurrentTimeHandler : GetRestHandler<CurrentTime> {

    override val route = "status/time"
    override val apiVersion = "v1"

    @OpenApi(summary = "Returns the current time on the server.",
        path = "/api/v1/status/time",
        methods = [HttpMethod.GET],
        tags = ["Status"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(CurrentTime::class)])
        ])
    override fun doGet(ctx: Context): CurrentTime = CurrentTime()

}

data class DresInfo(
    val version: String = DRES.VERSION,
    val startTime: Long,
    val uptime: Long
)

class InfoHandler : GetRestHandler<DresInfo> {

    override val route = "status/info"
    override val apiVersion = "v1"

    private val startTime = System.currentTimeMillis()

    @OpenApi(summary = "Returns an overview of the server properties.",
        path = "/api/v1/status/info",
        methods = [HttpMethod.GET],
        tags = ["Status"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(DresInfo::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)])
        ])
    override fun doGet(ctx: Context): DresInfo = DresInfo(
        startTime = startTime,
        uptime = System.currentTimeMillis() - startTime
    )

}

data class DresAdminInfo(
    val version: String = DRES.VERSION,
    val startTime: Long,
    val uptime: Long,
    val os: String = System.getProperty("os.name"),
    val jvm: String = "${System.getProperty("java.version")} (${System.getProperty("java.vendor")})",
    val args: String = ManagementFactory.getRuntimeMXBean().inputArguments.joinToString(),
    val cores: Int = Runtime.getRuntime().availableProcessors(),
    val freeMemory: Long = Runtime.getRuntime().freeMemory(),
    val totalMemory: Long = Runtime.getRuntime().totalMemory(),
    val load: Double = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean::class.java)?.systemCpuLoad ?: -1.0,
    val availableSeverThreads: Int = RestApi.readyThreadCount
)

class AdminInfoHandler: AccessManagedRestHandler, GetRestHandler<DresAdminInfo> {

    override val permittedRoles: Set<RouteRole> = setOf(RestApiRole.ADMIN)
    override val route = "status/info/admin"
    override val apiVersion = "v1"

    private val startTime = System.currentTimeMillis()

    @OpenApi(summary = "Returns an extensive overview of the server properties.",
        path = "/api/v1/status/info/admin",
        methods = [HttpMethod.GET],
        tags = ["Status"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(DresAdminInfo::class)])
        ])
    override fun doGet(ctx: Context): DresAdminInfo = DresAdminInfo(
        startTime = startTime,
        uptime = System.currentTimeMillis() - startTime
    )

}