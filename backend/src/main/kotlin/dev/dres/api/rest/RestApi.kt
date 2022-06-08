package dev.dres.api.rest

import com.fasterxml.jackson.databind.SerializationFeature
import dev.dres.api.rest.handler.*
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.data.dbo.DataAccessLayer
import dev.dres.data.model.Config
import dev.dres.run.RunExecutor
import dev.dres.utilities.NamedThreadFactory
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.http.staticfiles.Location
import io.javalin.plugin.openapi.OpenApiOptions
import io.javalin.plugin.openapi.OpenApiPlugin
import io.javalin.plugin.openapi.jackson.JacksonToJsonMapper
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
import org.eclipse.jetty.util.thread.QueuedThreadPool
import org.slf4j.LoggerFactory
import org.slf4j.MarkerFactory
import java.io.File

object RestApi {

    private var javalin: Javalin? = null

    private lateinit var openApiPlugin: OpenApiPlugin

    private val logMarker = MarkerFactory.getMarker("REST")
    private val logger = LoggerFactory.getLogger(this.javaClass)

    private val mapper = JacksonToJsonMapper.defaultObjectMapper.enable(SerializationFeature.INDENT_OUTPUT)

    fun getOpenApiPlugin(): OpenApiPlugin {
        return openApiPlugin
    }

    fun init(config: Config, dataAccessLayer: DataAccessLayer) {

        val runExecutor = RunExecutor


        /**
         * The list of API operations, each as a handler.
         * Did you follow our convention?
         *  - `GET  <entity>/{<entityId>}` with entity being an entity of the system in singular. Note the id is prefixed with the entity name
         *  - `GET <entity>/list` with entity being an entity of the system in singular, returning a list of all of these entities
         *  - Above naming scheme applies also for nested / context sensitive entities
         *  - REST conventions for `POST`, `PATCH` and `DELETE` methods apply
         */
        val apiRestHandlers = listOf(

            // User
            LoginHandler(dataAccessLayer.audit),
            LogoutHandler(dataAccessLayer.audit),
            ListUsersHandler(),
            CurrentUsersHandler(),
            DeleteUsersHandler(),
            CreateUsersHandler(),
            UpdateUsersHandler(),
            CurrentUsersSessionIdHandler(),
            UserDetailsHandler(), // Must be AFTER CurrentUserHandler
            ActiveSessionsHandler(dataAccessLayer.users),

            // Media
            MediaPreviewHandler(dataAccessLayer.collections, dataAccessLayer.mediaItemCollectionNameIndex, config),
            SubmissionPreviewHandler(dataAccessLayer.collections, dataAccessLayer.mediaItemCollectionNameIndex, config),
            GetMediaHandler(dataAccessLayer.mediaItemCollectionUidIndex, dataAccessLayer.collectionUidIndex),

            // Collection
            ListCollectionHandler(dataAccessLayer.collections, dataAccessLayer.mediaItems),
            ShowCollectionHandler(dataAccessLayer.collections, dataAccessLayer.mediaItems),
            AddCollectionHandler(dataAccessLayer.collections, dataAccessLayer.mediaItems),
            UpdateCollectionHandler(dataAccessLayer.collections, dataAccessLayer.mediaItems),
            DeleteCollectionHandler(dataAccessLayer.collections, dataAccessLayer.mediaItems),
            AddMediaItemHandler(dataAccessLayer.collections, dataAccessLayer.mediaItems),
            UpdateMediaItemHandler(dataAccessLayer.collections, dataAccessLayer.mediaItems),
            DeleteMediaItemHandler(dataAccessLayer.collections, dataAccessLayer.mediaItems),
            GetMediaItemHandler(dataAccessLayer.collections, dataAccessLayer.mediaItems),
            RandomMediaItemHandler(
                dataAccessLayer.collections,
                dataAccessLayer.mediaItems,
                dataAccessLayer.mediaItemCollectionUidIndex
            ), // Must be before ListMediaItem
            ResolveMediaItemListByNameHandler(
                dataAccessLayer.collections,
                dataAccessLayer.mediaItems,
                dataAccessLayer.mediaItemCollectionNameIndex
            ), // Must be before ListMediaItem
            ListMediaItemHandler(
                dataAccessLayer.collections,
                dataAccessLayer.mediaItems,
                dataAccessLayer.mediaItemCollectionNameIndex
            ),
            ListExternalItemHandler(config),

            // Competition
            ListCompetitionHandler(dataAccessLayer.competitions),
            CreateCompetitionHandler(dataAccessLayer.competitions),
            UpdateCompetitionHandler(dataAccessLayer.competitions, config, dataAccessLayer.mediaItems),
            GetCompetitionHandler(dataAccessLayer.competitions),
            DeleteCompetitionHandler(dataAccessLayer.competitions),
            ListTeamHandler(dataAccessLayer.competitions),
            ListDetailedTeamHandler(dataAccessLayer.competitions),
            ListTaskHandler(dataAccessLayer.competitions),
            GetTeamLogoHandler(config),

            // Submission
            SubmissionHandler(
                dataAccessLayer.collections,
                dataAccessLayer.mediaItemCollectionNameIndex,
                dataAccessLayer.mediaSegmentItemIdIndex,
                config
            ),

            // Log
            QueryLogHandler(),
            ResultLogHandler(),

            // Competition run
            ListCompetitionRunInfosHandler(),
            ListCompetitionRunStatesHandler(),
            GetCompetitionRunInfoHandler(),
            GetCompetitionRunStateHandler(),
            CurrentTaskHintHandler(config),
            CurrentTaskTargetHandler(config, dataAccessLayer.collections),
            CurrentTaskInfoHandler(),
            SubmissionInfoHandler(),
            RecentSubmissionInfoHandler(),
            HistorySubmissionInfoHandler(),

            // Competition run scores
            ListCompetitionScoreHandler(),
            CurrentTaskScoreHandler(),
            HistoryTaskScoreHandler(),
            ListScoreSeriesHandler(),
            ListScoreboardsHandler(),
            TeamGroupScoreHandler(),

            // Competition run admin
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
            ListSubmissionsPerTaskRunAdminHandler(),
            OverwriteSubmissionStatusRunAdminHandler(),
            ListPastTasksPerTaskRunAdminHandler(),
            OverviewRunAdminHandler(),

            // Judgement
            NextOpenJudgementHandler(dataAccessLayer.collections),
            NextOpenVoteJudgementHandler(dataAccessLayer.collections),
            PostJudgementHandler(),
            JudgementStatusHandler(),
            JudgementVoteHandler(),

            // Audit Log
            GetAuditLogInfoHandler(dataAccessLayer.auditTimes),
            ListAuditLogsInRangeHandler(dataAccessLayer.auditTimes, dataAccessLayer.audit),
            ListAuditLogsHandler(dataAccessLayer.auditTimes, dataAccessLayer.audit),

            // Status
            CurrentTimeHandler(),

            //API Client
            ListCompetitionRunClientInfoHandler(),
            CompetitionRunClientCurrentTaskInfoHandler(),

            // Downloads
            DownloadHandler.CompetitionRun(dataAccessLayer.runs),
            DownloadHandler.CompetitionRunScore(dataAccessLayer.runs),
            DownloadHandler.CompetitionDesc(dataAccessLayer.competitions)
        )

        javalin = Javalin.create {
            it.enableCorsForAllOrigins()
            it.server { setupHttpServer(config) }
            it.registerPlugin(
                OpenApiPlugin(
                    /* "Internal" DRES openapi (<host>/swagger-ui) */
                    getOpenApiOptionsFor(),
                    /* "Public" client endpoint (<host>/swagger-client */
                    getOpenApiOptionsFor(OpenApiEndpointOptions.dresSubmittingClientOptions)
                )
            )
            it.defaultContentType = "application/json"
            it.prefer405over404 = true
            it.sessionHandler { fileSessionHandler(config) }
            it.accessManager(AccessManager::manage)
            it.addStaticFiles("html", Location.CLASSPATH)
            it.addSinglePageRoot("/vote", "vote/index.html")
            it.addSinglePageRoot("/", "html/index.html")
            it.enforceSsl = config.enableSsl
        }.routes {
            path("api") {
                apiRestHandlers.groupBy { it.apiVersion }.forEach { apiGroup ->
                    path(apiGroup.key) {
                        apiGroup.value.forEach { handler ->
                            path(handler.route) {

                                val permittedRoles = if (handler is AccessManagedRestHandler) {
                                    handler.permittedRoles.toTypedArray()
                                } else {
                                    arrayOf(RestApiRole.ANYONE)
                                }

                                if (handler is GetRestHandler<*>) {
                                    get(handler::get, *permittedRoles)
                                }

                                if (handler is PostRestHandler<*>) {
                                    post(handler::post, *permittedRoles)
                                }

                                if (handler is PatchRestHandler<*>) {
                                    patch(handler::patch, *permittedRoles)
                                }

                                if (handler is DeleteRestHandler<*>) {
                                    delete(handler::delete, *permittedRoles)
                                }

                            }
                        }
                    }
                }
                ws("ws/run", runExecutor)
            }
        }.before {
            logger.info(
                logMarker,
                "${it.req.method} request to ${it.path()} with params (${
                    it.queryParamMap().map { e -> "${e.key}=${e.value}" }.joinToString()
                }) from ${it.req.remoteAddr}"
            )
            if (it.path().startsWith("/api/")) { //do not cache api requests
                it.header("Cache-Control", "max-age=0")
            }
        }.error(401) {
            it.json(ErrorStatus("Unauthorized request!"))
        }.exception(Exception::class.java) { e, ctx ->
            ctx.status(500).json(ErrorStatus("Internal server error!"))
            logger.error("Exception during handling of request to ${ctx.path()}", e)
        }
            .start(config.httpPort)
    }

    fun stop() {
        javalin?.stop()
        javalin = null
    }


    private fun getOpenApiOptionsFor(options: OpenApiEndpointOptions = OpenApiEndpointOptions.dresDefaultOptions) =
        OpenApiOptions(
            Info().apply {
                title("DRES API")
                version("1.0")
                description("API for DRES (Distributed Retrieval Evaluation Server), Version 1.0")
            }
        ).apply {
            path(options.oasPath) // endpoint for OpenAPI json
            swagger(SwaggerOptions(options.swaggerUi)) // endpoint for swagger-ui
            activateAnnotationScanningFor("dev.dres.api.rest.handler")
            options.ignored().forEach { it.second.forEach { method -> ignorePath(it.first, method) } }
            toJsonMapper(JacksonToJsonMapper(jacksonMapper))
        }


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

    private val pool = QueuedThreadPool(
        1000, 8, 60000, -1, null, null, NamedThreadFactory("JavalinPool")
    )

    val readyThreadCount: Int
        get() = pool.readyThreads

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

            return Server(pool).apply {
                //HTTP Connector
                addConnector(
                    ServerConnector(
                        server,
                        HttpConnectionFactory(httpConfig),
                        HTTP2ServerConnectionFactory(httpConfig)
                    ).apply {
                        port = config.httpPort
                    })
                // HTTPS Connector
                addConnector(ServerConnector(server, ssl, alpn, http2, fallback).apply {
                    port = config.httpsPort
                })
            }
        } else {
            return Server(pool).apply {
                //HTTP Connector
                addConnector(
                    ServerConnector(
                        server,
                        HttpConnectionFactory(httpConfig),
                        HTTP2ServerConnectionFactory(httpConfig)
                    ).apply {
                        port = config.httpPort
                    })

            }
        }
    }
}
