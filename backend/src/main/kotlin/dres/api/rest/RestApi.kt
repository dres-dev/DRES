package dres.api.rest

import dres.api.rest.handler.*
import dres.api.rest.types.status.ErrorStatus
import dres.data.dbo.DataAccessLayer
import dres.data.model.Config
import dres.run.RunExecutor
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.core.security.SecurityUtil.roles
import io.javalin.plugin.openapi.OpenApiOptions
import io.javalin.plugin.openapi.OpenApiPlugin
import io.javalin.plugin.openapi.ui.ReDocOptions
import io.javalin.plugin.openapi.ui.SwaggerOptions
import io.swagger.v3.oas.models.info.Info
import org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory
import org.eclipse.jetty.http.HttpCookie
import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory
import org.eclipse.jetty.server.*
import org.eclipse.jetty.server.session.DefaultSessionCache
import org.eclipse.jetty.server.session.FileSessionDataStore
import org.eclipse.jetty.server.session.SessionHandler
import org.eclipse.jetty.util.ssl.SslContextFactory
import org.slf4j.LoggerFactory
import org.slf4j.MarkerFactory
import java.io.File

object RestApi {

    private var javalin: Javalin? = null

    private val logMarker = MarkerFactory.getMarker("REST")
    private val logger = LoggerFactory.getLogger(this.javaClass)

    fun init(config: Config, dataAccessLayer: DataAccessLayer) {

        val runExecutor = RunExecutor


        val apiRestHandlers = listOf(

                //user
                LoginHandler(dataAccessLayer.audit),
                LogoutHandler(dataAccessLayer.audit),
                ListUsersHandler(),
                CurrentUsersHandler(),
                DeleteUsersHandler(),
                CreateUsersHandler(),
                UpdateUsersHandler(),
                CurrentUsersSessionIdHandler(),
                ActiveSessionsHandler(dataAccessLayer.users),

                //media
                MediaPreviewHandler(dataAccessLayer.collections, dataAccessLayer.mediaItemCollectionNameIndex, config),
                SubmissionPreviewHandler(dataAccessLayer.collections, dataAccessLayer.mediaItemCollectionNameIndex, config),
                GetMediaHandler(dataAccessLayer.collections, dataAccessLayer.mediaItemCollectionNameIndex, dataAccessLayer.collectionNameIndex),

                //collection
                ListCollectionHandler(dataAccessLayer.collections, dataAccessLayer.mediaItems),
                ShowCollectionHandler(dataAccessLayer.collections, dataAccessLayer.mediaItems),
                AddMediaItemHandler(dataAccessLayer.collections, dataAccessLayer.mediaItems),
                GetMediaItemHandler(dataAccessLayer.collections, dataAccessLayer.mediaItems),
                RandomMediaItemHandler(dataAccessLayer.collections, dataAccessLayer.mediaItems, dataAccessLayer.mediaItemCollectionIndex), // Must be before ListMediaItem
                ListMediaItemHandler(dataAccessLayer.collections, dataAccessLayer.mediaItems, dataAccessLayer.mediaItemCollectionIndex),

                //competition
                ListCompetitionHandler(dataAccessLayer.competitions),
                CreateCompetitionHandler(dataAccessLayer.competitions),
                UpdateCompetitionHandler(dataAccessLayer.competitions, dataAccessLayer.mediaItems),
                GetCompetitionHandler(dataAccessLayer.competitions),
                DeleteCompetitionHandler(dataAccessLayer.competitions),
                ListTeamHandler(dataAccessLayer.competitions),
                ListTaskHandler(dataAccessLayer.competitions),

                //competition run
                ListCompetitionRunInfosHandler(),
                ListCompetitionRunStatesHandler(),
                GetCompetitionRunInfoHandler(),
                GetCompetitionRunStateHandler(),
                ListCompetitionScoreHandler(),
                CurrentTaskScoreHandler(),
                CurrentQueryHandler(config),
                CurrentTaskInfoHandler(),
                SubmissionInfoHandler(),
                RecentSubmissionInfoHandler(),
                PastSubmissionInfoHandler(),

                //Competition run admin
                CreateCompetitionRunAdminHandler(dataAccessLayer.competitions, dataAccessLayer.collections, config),
                StartCompetitionRunAdminHandler(),
                NextTaskCompetitionRunAdminHandler(),
                PreviousTaskCompetitionRunAdminHandler(),
                SwitchTaskCompetitionRunAdminHandler(),
                StartTaskCompetitionRunAdminHandler(),
                AbortTaskCompetitionRunAdminHandler(),
                TerminateCompetitionRunAdminHandler(),
                AdjustDurationRunAdminHandler(),
                ListViewersRunAdminHandler(),
                ForceViewerRunAdminHandler(),

                NextOpenJudgementHandler(dataAccessLayer.collections),
                PostJudgementHandler(),
                JudgementStatusHandler()
        )

        javalin = Javalin.create {
            it.enableCorsForAllOrigins()
            it.server { setupHttpServer(config) }
            it.registerPlugin(getConfiguredOpenApiPlugin())
            it.defaultContentType = "application/json"
            it.prefer405over404 = true
            it.sessionHandler { fileSessionHandler(config) }
            it.accessManager(AccessManager::manage)
            it.addStaticFiles("html")
            it.addSinglePageRoot("/", "html/index.html")
            it.enforceSsl = config.enableSsl
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
                ws("ws/run", runExecutor)
            }

            path("submit") {
                val submissionHandler = SubmissionHandler(dataAccessLayer.collections, dataAccessLayer.mediaItemCollectionNameIndex, dataAccessLayer.mediaSegmentItemIdIndex, config)
                get(submissionHandler::get, submissionHandler.permittedRoles)
            }

            path("log/query"){
                val queryLogHandler = QueryLogHandler()
                post(queryLogHandler::post, queryLogHandler.permittedRoles)
            }

            path("log/result"){
                val resultLogHandler = ResultLogHandler()
                post(resultLogHandler::post, resultLogHandler.permittedRoles)
            }

        }.before {
            logger.info(logMarker, "${it.req.method} request to ${it.path()} with params (${it.queryParamMap().map { e -> "${e.key}=${e.value}" }.joinToString()}) from ${it.req.remoteAddr}")
        }.error(401) {
            it.json(ErrorStatus("Unauthorized request!"))
        }.exception(Exception::class.java) { e, ctx ->
            ctx.status(500).json(ErrorStatus("Internal server error!"))
            logger.error("Exception during hadling of request to ${ctx.path()}", e)
        }
        .start(config.httpPort)
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

    private fun fileSessionHandler(config: Config) = SessionHandler().apply {
        sessionCache = DefaultSessionCache(this).apply {
            sessionDataStore = FileSessionDataStore().apply {
                val baseDir = File(".")
                this.storeDir = File(baseDir, "session-store").apply { mkdir() }
            }
        }

        if (config.enableSsl) {
            sameSite = HttpCookie.SameSite.NONE
            sessionCookieConfig.isSecure = true
            isSecureRequestOnly = true
        }

    }

    private fun setupHttpServer(config: Config): Server {

        val httpConfig = HttpConfiguration().apply {
            sendServerVersion = false
            sendXPoweredBy = false
            if (config.enableSsl) {
                secureScheme = "https"
                securePort = config.httpsPort
            }
        }

        if (config.enableSsl) {
            val httpsConfig = HttpConfiguration(httpConfig).apply {
                addCustomizer(SecureRequestCustomizer())
            }

            val alpn = ALPNServerConnectionFactory().apply {
                defaultProtocol = "http/1.1"
            }

            val sslContextFactory = SslContextFactory.Server().apply {
                keyStorePath = config.keystorePath
                setKeyStorePassword(config.keystorePassword)
                //cipherComparator = HTTP2Cipher.COMPARATOR
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
        } else {

            return Server().apply {
                //HTTP Connector
                addConnector(ServerConnector(server, HttpConnectionFactory(httpConfig), HTTP2ServerConnectionFactory(httpConfig)).apply {
                    port = config.httpPort
                })

            }

        }

    }



}
