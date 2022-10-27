package dev.dres.api.rest

import dev.dres.api.rest.handler.*
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.data.dbo.DataAccessLayer
import dev.dres.data.model.Config
import dev.dres.run.RunExecutor
import dev.dres.utilities.NamedThreadFactory
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.community.ssl.SSLPlugin
import io.javalin.http.staticfiles.Location
import io.javalin.openapi.plugin.OpenApiConfiguration
import io.javalin.openapi.plugin.OpenApiPlugin
import io.javalin.openapi.plugin.swagger.SwaggerConfiguration
import io.javalin.openapi.plugin.swagger.SwaggerPlugin
import org.eclipse.jetty.http.HttpCookie
import org.eclipse.jetty.server.*
import org.eclipse.jetty.server.session.DefaultSessionCache
import org.eclipse.jetty.server.session.FileSessionDataStore
import org.eclipse.jetty.server.session.SessionHandler
import org.eclipse.jetty.util.thread.QueuedThreadPool
import org.slf4j.LoggerFactory
import org.slf4j.MarkerFactory
import java.io.File

object RestApi {

    private var javalin: Javalin? = null

    private val logMarker = MarkerFactory.getMarker("REST")
    private val logger = LoggerFactory.getLogger(this.javaClass)

    fun init(config: Config, store: Entity) {

        val runExecutor = RunExecutor


        /**
         * The list of API operations, each as a handler.
         * Did you follow our convention?
         *  - `GET  <entity>/{<entityId>}` with entity being an entity of the system in singular. Note the id is prefixed with the entity name
         *  - `GET <entity>/list` with entity being an entity of the system in singular, returning a list of all of these entities
         *  - Above naming scheme applies also for nested / context-sensitive entities
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
            JsonBatchSubmissionHandler(
                dataAccessLayer.collections,
                dataAccessLayer.mediaItemCollectionNameIndex,
                dataAccessLayer.mediaSegmentItemIdIndex
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
            UpdateRunPropertiesAdminHandler(),

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
            InfoHandler(),
            AdminInfoHandler(),

            //API Client
            ListCompetitionRunClientInfoHandler(),
            CompetitionRunClientCurrentTaskInfoHandler(),

            // Downloads
            DownloadHandler.CompetitionRun(dataAccessLayer.runs),
            DownloadHandler.CompetitionRunScoreHandler(dataAccessLayer.runs),
            DownloadHandler.CompetitionDesc(dataAccessLayer.competitions)
        )

        javalin = Javalin.create {
            it.plugins.enableCors { cors ->
                cors.add { corsPluginConfig ->
                    corsPluginConfig.anyHost()
                }
            }

            it.plugins.register(
                OpenApiPlugin(
                    OpenApiConfiguration().apply {
                        this.info.title = "DRES API"
                        this.info.version = "1.0"
                        this.info.description = "API for DRES (Distributed Retrieval Evaluation Server), Version 1.0"
                        this.documentationPath = "/swagger-docs"
                    }
                )
            )

            it.plugins.register(
                ClientOpenApiPlugin()
            )

//            it.plugins.register(
//                SwaggerPlugin(
//                    SwaggerConfiguration().apply {
//                        this.documentationPath = "/swagger-docs"
//                        this.uiPath = "/swagger-ui"
//                    }
//                )
//            )

            it.plugins.register(ClientSwaggerPlugin())

            it.http.defaultContentType = "application/json"
            it.http.prefer405over404 = true
            it.jetty.server { setupHttpServer() }
            it.jetty.sessionHandler { fileSessionHandler(config) }
            it.accessManager(AccessManager::manage)
            it.staticFiles.add("html", Location.CLASSPATH)
            it.spaRoot.addFile("/vote", "vote/index.html")
            it.spaRoot.addFile("/", "html/index.html")

            if (config.enableSsl) {
                val ssl = SSLPlugin { conf ->
                    conf.keystoreFromPath(config.keystorePath, config.keystorePassword)
                    conf.http2 = true
                    conf.secure = true
                    conf.insecurePort = config.httpPort
                    conf.securePort = config.httpsPort
                    conf.sniHostCheck = false
                }
                it.plugins.register(ssl)
            }

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
                "${it.req().method} request to ${it.path()} with params (${
                    it.queryParamMap().map { e -> "${e.key}=${e.value}" }.joinToString()
                }) from ${it.req().remoteAddr}"
            )
            if (it.path().startsWith("/api/")) { //do not cache api requests
                it.header("Cache-Control", "no-store")
            }
        }.error(401) {
            it.json(ErrorStatus("Unauthorized request!"))
        }.exception(Exception::class.java) { e, ctx ->
            ctx.status(500).json(ErrorStatus("Internal server error: ${e.localizedMessage}"))
            logger.error("Exception during handling of request to ${ctx.path()}", e)
        }
            .start(config.httpPort)
    }

    fun stop() {
        javalin?.stop()
        javalin = null
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

    private fun setupHttpServer(): Server {
        return Server(pool)
    }

}
