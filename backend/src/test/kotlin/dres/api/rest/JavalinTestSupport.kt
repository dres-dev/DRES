package dev.dres.api.rest

import dev.dres.data.model.config.CorsConfig
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.path

/**
 * Small helpers that start a real server for tests of API-wide behaviour.
 *
 * Behaviour such as CORS headers and exception mapping is installed by [RestApi] inside a Javalin
 * configuration block that cannot be reached from a test, so the tests mount the same plugin
 * configuration here. Keeping it in one place means the test's rules are written down explicitly
 * instead of being copied into each test.
 */
object JavalinTestSupport {

    /**
     * Starts a server on [port] that answers `GET /api/v2/status/info` and installs the CORS rule used by
     * the production application.
     *
     * @param port The port to bind, usually taken from a temporary [java.net.ServerSocket].
     * @param cors Supplies the origin allowlist.
     */
    fun startCorsEcho(port: Int, cors: () -> CorsConfig): Javalin {
        val config = cors()
        return Javalin.create { cfg ->
            /* Installs the production rule itself, so the test cannot drift from what ships. */
            CorsConfiguration.apply(cfg, config.allowedOrigins)
            cfg.routes.apiBuilder {
                path("api") {
                    path("v2") {
                        get("status/info") { ctx -> ctx.json(mapOf("name" to "dres")) }
                    }
                }
            }
        }.start(port)
    }
}
