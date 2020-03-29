package dres.api.rest

import dres.api.rest.handler.*
import dres.api.rest.types.status.ErrorStatus
import dres.data.dbo.DataAccessLayer
import dres.data.model.Config
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.core.security.SecurityUtil.roles
import io.javalin.plugin.openapi.OpenApiOptions
import io.javalin.plugin.openapi.OpenApiPlugin
import io.javalin.plugin.openapi.ui.ReDocOptions
import io.javalin.plugin.openapi.ui.SwaggerOptions
import io.swagger.v3.oas.models.info.Info
import org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory
import org.eclipse.jetty.http2.HTTP2Cipher
import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory
import org.eclipse.jetty.server.*
import org.eclipse.jetty.server.session.DefaultSessionCache
import org.eclipse.jetty.server.session.FileSessionDataStore
import org.eclipse.jetty.server.session.SessionHandler
import org.eclipse.jetty.util.ssl.SslContextFactory
import java.io.File

object RestApi {

    private var javalin: Javalin? = null


    fun init(config: Config, dataAccessLayer: DataAccessLayer) {


        val apiRestHandlers = listOf(
                //misc
                GetVersionHandler(),

                //user
                LoginHandler(dataAccessLayer.users),
                LogoutHandler(),
                ListUsersHandler(dataAccessLayer.users),
                CurrentUsersHandler(dataAccessLayer.users),

                //media
                GetFrameHandler(dataAccessLayer.collections, dataAccessLayer.mediaItems),

                //competition
                ListCompetitionHandler(dataAccessLayer.competitions),
                CreateCompetitionHandler(dataAccessLayer.competitions),

                GetCompetitionHandler(dataAccessLayer.competitions),
                DeleteCompetitionHandler(dataAccessLayer.competitions),

                ListTeamHandler(dataAccessLayer.competitions),
                AddTeamHandler(dataAccessLayer.competitions),

                ListTaskHandler(dataAccessLayer.competitions),
                AddTaskHandler(dataAccessLayer.competitions)
        )

        javalin = Javalin.create {
            it.enableCorsForAllOrigins()
            it.server { setupHttpServer(config) }
            it.registerPlugin(getConfiguredOpenApiPlugin())
            it.defaultContentType = "application/json"
            it.sessionHandler(::fileSessionHandler)
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

                        if (handler is GetRestHandler<*>){
                            get(handler::get, permittedRoles)
                        }

                        if (handler is PostRestHandler<*>){
                            post(handler::post, permittedRoles)
                        }

                        if (handler is PatchRestHandler<*>){
                            patch(handler::patch, permittedRoles)
                        }

                        if (handler is DeleteRestHandler<*>){
                            delete(handler::delete, permittedRoles)
                        }

                    }
                }

            }


        }.before {
            //TODO log request
        }
        .error(401) {
            it.json(ErrorStatus("Unauthorized request!"))
        }
        .exception(Exception::class.java) { e, _ -> e.printStackTrace() }.start(config.httpPort)
    }

    fun stop() {
        javalin?.stop()
        javalin = null
    }

    private fun getConfiguredOpenApiPlugin() = OpenApiPlugin(
            OpenApiOptions(
                    Info().apply {
                        title("DRES API")
                        version("1.0")
                        description("API for DRES (Distributed Retrieval Evaluation Server), Version 1.0")
                    }
            ).apply {
                path("/swagger-docs") // endpoint for OpenAPI json
                swagger(SwaggerOptions("/swagger-ui")) // endpoint for swagger-ui
                reDoc(ReDocOptions("/redoc")) // endpoint for redoc
                activateAnnotationScanningFor("dres.api.rest.handler")
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

    private fun setupHttpServer(config: Config): Server {

        val httpConfig = HttpConfiguration().apply {
            sendServerVersion = false
            sendXPoweredBy = false
            secureScheme = "https"
            securePort = config.httpsPort
        }

        val httpsConfig = HttpConfiguration(httpConfig).apply {
            addCustomizer(SecureRequestCustomizer())
        }

        val alpn = ALPNServerConnectionFactory().apply {
            defaultProtocol = "h2"
        }

        val sslContextFactory = SslContextFactory.Server().apply {
            keyStorePath = config.keystorePath
            setKeyStorePassword(config.keystorePassword)
            cipherComparator = HTTP2Cipher.COMPARATOR
            provider = "Conscrypt"
        }

        val ssl = SslConnectionFactory(sslContextFactory, alpn.protocol)

        val http2 = HTTP2ServerConnectionFactory(httpsConfig)

        val fallback = HttpConnectionFactory(httpsConfig)


        return Server().apply {
            //HTTP Connector
            addConnector(ServerConnector(server, HttpConnectionFactory(httpConfig), HTTP2ServerConnectionFactory(httpConfig)).apply {
                port = config.httpPort
            })
            // HTTPS Connector
            addConnector(ServerConnector(server, ssl, alpn, http2, fallback).apply {
                port = config.httpsPort
            })
        }


    }



}