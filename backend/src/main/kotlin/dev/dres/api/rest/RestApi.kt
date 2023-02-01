package dev.dres.api.rest

import GetAuditLogInfoHandler
import GetTaskHintHandler
import dev.dres.api.rest.handler.*
import dev.dres.api.rest.handler.audit.ListAuditLogsHandler
import dev.dres.api.rest.handler.audit.ListAuditLogsInRangeHandler
import dev.dres.api.rest.handler.collection.*
import dev.dres.api.rest.handler.download.EvaluationDownloadHandler
import dev.dres.api.rest.handler.download.EvaluationTemplateDownloadHandler
import dev.dres.api.rest.handler.evaluation.admin.*
import dev.dres.api.rest.handler.evaluation.client.ClientListEvaluationsHandler
import dev.dres.api.rest.handler.evaluation.client.ClientTaskInfoHandler
import dev.dres.api.rest.handler.evaluation.scores.*
import dev.dres.api.rest.handler.evaluation.viewer.*
import dev.dres.api.rest.handler.judgement.*
import dev.dres.api.rest.handler.log.QueryLogHandler
import dev.dres.api.rest.handler.log.ResultLogHandler
import dev.dres.api.rest.handler.template.*
import dev.dres.api.rest.handler.preview.GetMediaHandler
import dev.dres.api.rest.handler.preview.MediaPreviewHandler
import dev.dres.api.rest.handler.preview.SubmissionPreviewHandler
import dev.dres.api.rest.handler.scores.ListCompetitionScoreHandler
import dev.dres.api.rest.handler.submission.BatchSubmissionHandler
import dev.dres.api.rest.handler.submission.SubmissionHandler
import dev.dres.api.rest.handler.system.CurrentTimeHandler
import dev.dres.api.rest.handler.system.InfoHandler
import dev.dres.api.rest.handler.system.LoginHandler
import dev.dres.api.rest.handler.system.LogoutHandler
import dev.dres.api.rest.handler.users.*
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.users.ApiRole
import dev.dres.data.model.Config
import dev.dres.run.RunExecutor
import dev.dres.utilities.NamedThreadFactory
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.community.ssl.SSLPlugin
import io.javalin.http.staticfiles.Location
import io.javalin.openapi.plugin.OpenApiConfiguration
import io.javalin.openapi.plugin.OpenApiPlugin
import jetbrains.exodus.database.TransientEntityStore
import org.eclipse.jetty.http.HttpCookie
import org.eclipse.jetty.server.*
import org.eclipse.jetty.server.session.DefaultSessionCache
import org.eclipse.jetty.server.session.FileSessionDataStore
import org.eclipse.jetty.server.session.SessionHandler
import org.eclipse.jetty.util.thread.QueuedThreadPool
import org.slf4j.LoggerFactory
import org.slf4j.MarkerFactory
import java.io.File

/**
 * This is a singleton instance of the RESTful API
 *
 * @version 1.1.0
 * @author Luca Rossetto
 */
object RestApi {

    private var javalin: Javalin? = null

    private val logMarker = MarkerFactory.getMarker("REST")
    private val logger = LoggerFactory.getLogger(this.javaClass)

    fun init(config: Config, store: TransientEntityStore) {

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
            LoginHandler(),
            LogoutHandler(),
            ListUsersHandler(),
            ListActiveUsersHandler(),
            ShowCurrentUserHandler(),
            ShowCurrentSessionHandler(),
            CreateUsersHandler(),
            DeleteUsersHandler(),
            UpdateUsersHandler(),
            UserDetailsHandler(),

            // Media
            MediaPreviewHandler(store, config),
            SubmissionPreviewHandler(store, config),
            GetMediaHandler(store),

            // Collection
            ListCollectionHandler(store),
            ShowCollectionHandler(store),
            AddCollectionHandler(store),
            UpdateCollectionHandler(store),
            DeleteCollectionHandler(store),
            AddMediaItemHandler(store),
            UpdateMediaItemHandler(store),
            DeleteMediaItemHandler(store),
            RandomMediaItemHandler(store), // Must be before ListMediaItem
            ResolveMediaItemListByNameHandler(store), // Must be before ListMediaItem
            ListMediaItemHandler(store),
            ListExternalItemHandler(config),

            // Competition
            ListEvaluationTemplatesHandler(store),
            CreateEvaluationTemplateHandler(store),
            UpdateCompetitionHandler(store, config),
            ShowEvaluationTemplateHandler(store),
            DeleteEvaluationTemplateHandler(store),
            ListTeamHandler(store),
            ListTasksHandler(store),
            GetTeamLogoHandler(store),

            // Submission
            SubmissionHandler(store, config),
            BatchSubmissionHandler(store, config),

            // Log
            QueryLogHandler(),
            ResultLogHandler(),

            // Evaluation
            ListEvaluationInfoHandler(store),
            ListEvaluationStatesHandler(store),
            GetEvaluationInfoHandler(store),
            GetEvaluationStateHandler(store),
            GetTaskHintHandler(store, config),
            GetTaskTargetHandler(store, config),
            GetCurrentTaskHandler(store),
            GetSubmissionInfoHandler(store),
            GetSubmissionAfterInfoHandler(store),
            GetSubmissionHistoryInfoHandler(store),

            // Competition run scores
            ListCompetitionScoreHandler(store),
            CurrentTaskScoreHandler(store),
            HistoryTaskScoreHandler(store),
            ListScoreSeriesHandler(store),
            ListScoreboardsHandler(store),
            TeamGroupScoreHandler(store),

            // Evaluation administration
            CreateEvaluationHandler(store, config),
            StartEvaluationHandler(store),
            StopEvaluationHandler(store),
            NextTaskHandler(store),
            PreviousTaskHandler(store),
            SwitchTaskHandler(store),
            StartTaskHandler(store),
            StopTaskHandler(store),
            AdjustDurationHandler(store),
            AdjustPropertiesHandler(store),
            OverrideSubmissionHandler(store),
            ForceViewerHandler(store),
            ListViewersHandler(store),
            ListSubmissionsHandler(store),
            ListPastTaskHandler(store),
            EvaluationOverviewHandler(store),

            // Judgement
            DequeueJudgementHandler(store),
            DequeueVoteHandler(store),
            PostJudgementHandler(store),
            PostVoteHandler(store),
            JudgementStatusHandler(store),

            // Audit Log
            GetAuditLogInfoHandler(store),
            ListAuditLogsInRangeHandler(store),
            ListAuditLogsHandler(store),

            // Status
            CurrentTimeHandler(),
            InfoHandler(),

            //API Client
            ClientListEvaluationsHandler(store),
            ClientTaskInfoHandler(store),

            // Downloads
            EvaluationDownloadHandler(store),
            EvaluationTemplateDownloadHandler(store)
            /* DownloadHandler.CompetitionRunScoreHandler(store), */
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
                    conf.secure = false
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
                                    arrayOf(ApiRole.ANYONE)
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
        }.start(config.httpPort)
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
