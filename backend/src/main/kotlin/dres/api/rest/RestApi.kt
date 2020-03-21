package dres.api.rest

import dres.api.rest.handler.*
import dres.data.model.Config
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.core.security.SecurityUtil.roles
import io.javalin.plugin.openapi.OpenApiOptions
import io.javalin.plugin.openapi.OpenApiPlugin
import io.javalin.plugin.openapi.ui.ReDocOptions
import io.javalin.plugin.openapi.ui.SwaggerOptions
import io.swagger.v3.oas.models.info.Info
import org.eclipse.jetty.server.session.DefaultSessionCache
import org.eclipse.jetty.server.session.FileSessionDataStore
import org.eclipse.jetty.server.session.SessionHandler
import java.io.File

object RestApi {

    private var javalin: Javalin? = null


    fun init(config: Config) {

        val apiRestHandlers = listOf<RestHandler>(GetVersionHandler(), LoginHandler())

        javalin = Javalin.create {
            it.registerPlugin(getConfiguredOpenApiPlugin())
            it.defaultContentType = "application/json"
            it.sessionHandler { fileSessionHandler() }
            it.accessManager(AccessManager::manage)
        }.routes {
            path("api") {

                apiRestHandlers.forEach {handler ->
                    path(handler.route){

                        val permittedRoles = if (handler is AccessManagedRestHandler) {
                            handler.permittedRoles
                        } else {
                            roles(RestApiRole.ANYONE)
                        }

                        if (handler is GetRestHandler){
                            get({handler.get(it)}, permittedRoles)
                        }

                        if (handler is PostRestHandler){
                            post({handler.post(it)}, permittedRoles)
                        }

                        if (handler is PatchRestHandler){
                            patch({handler.patch(it)}, permittedRoles)
                        }

                        if (handler is DeleteRestHandler){
                            delete({handler.delete(it)}, permittedRoles)
                        }

                    }
                }

            }
        }.before {
            //TODO log request
        }.exception(Exception::class.java)
        { e, _ -> e.printStackTrace() }.start(config.port)
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