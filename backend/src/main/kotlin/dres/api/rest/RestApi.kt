package dres.api.rest

import dres.data.Config
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.plugin.openapi.OpenApiOptions
import io.javalin.plugin.openapi.OpenApiPlugin
import io.javalin.plugin.openapi.ui.ReDocOptions
import io.javalin.plugin.openapi.ui.SwaggerOptions
import io.swagger.v3.oas.models.info.Info
import org.eclipse.jetty.server.session.DefaultSessionCache
import org.eclipse.jetty.server.session.FileSessionDataStore
import org.eclipse.jetty.server.session.SessionHandler
import java.io.File
import java.util.logging.LogManager

object RestApi {

    private var javalin: Javalin? = null


    fun init(config: Config) {
        javalin = Javalin.create {
            it.registerPlugin(getConfiguredOpenApiPlugin())
            it.defaultContentType = "application/json"
            it.sessionHandler { fileSessionHandler() }
        }.routes {
            path("api") {

                path("version"){
                    get { ctx -> ctx.result("0.1") }
                }

            }
        }.before {
            //TODO log request
        }.start(config.port)
    }

    fun stop() {
        javalin?.stop()
        javalin = null
    }

    private fun getConfiguredOpenApiPlugin() = OpenApiPlugin(
            OpenApiOptions(
                    Info().apply {
                        version("1.0")
                        description("DRES API")
                    }
            ).apply {
                path("/swagger-docs") // endpoint for OpenAPI json
                swagger(SwaggerOptions("/swagger-ui")) // endpoint for swagger-ui
                reDoc(ReDocOptions("/redoc")) // endpoint for redoc

            }
    )

    private fun fileSessionHandler() = SessionHandler().apply {
        sessionCache = DefaultSessionCache(this).apply {
            sessionDataStore = FileSessionDataStore().apply {
                val baseDir = File(".")
                this.storeDir = File(baseDir, "session-store").apply { mkdir() }
            }
        }
    }

}