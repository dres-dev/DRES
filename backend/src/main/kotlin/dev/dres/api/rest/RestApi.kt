package dev.dres.api.rest

import GetAuditLogInfoHandler
import GetTaskHintHandler
import dev.dres.DRES
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
import dev.dres.api.rest.handler.evaluation.team.CreateTeamHandler
import dev.dres.api.rest.handler.evaluation.team.ListAllTeamsHandler
import dev.dres.api.rest.handler.evaluation.team.UpdateTeamHandler
import dev.dres.api.rest.handler.evaluation.viewer.*
import dev.dres.api.rest.handler.judgement.*
import dev.dres.api.rest.handler.log.QueryLogHandler
import dev.dres.api.rest.handler.log.ResultLogHandler
import dev.dres.api.rest.handler.template.*
import dev.dres.api.rest.handler.preview.GetMediaHandler
import dev.dres.api.rest.handler.preview.PreviewImageHandler
import dev.dres.api.rest.handler.preview.PreviewVideoHandler
import dev.dres.api.rest.handler.scores.ListEvaluationScoreHandler
import dev.dres.api.rest.handler.submission.LegacySubmissionHandler
import dev.dres.api.rest.handler.system.CurrentTimeHandler
import dev.dres.api.rest.handler.system.InfoHandler
import dev.dres.api.rest.handler.system.LoginHandler
import dev.dres.api.rest.handler.system.LogoutHandler
import dev.dres.api.rest.handler.users.*
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.users.ApiRole
import dev.dres.data.model.Config
import dev.dres.mgmt.cache.CacheManager
import dev.dres.run.RunExecutor
import dev.dres.utilities.NamedThreadFactory
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.http.staticfiles.Location
import io.javalin.community.ssl.SSLPlugin
import io.javalin.openapi.CookieAuth
import io.javalin.openapi.plugin.*
import io.javalin.openapi.plugin.swagger.SwaggerConfiguration
import io.javalin.openapi.plugin.swagger.SwaggerPlugin
import jetbrains.exodus.database.TransientEntityStore
import org.eclipse.jetty.server.*
import org.eclipse.jetty.util.thread.QueuedThreadPool
import org.slf4j.LoggerFactory
import org.slf4j.MarkerFactory

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

    /**
     * Initializes the [RestApi] singleton.
     *
     * @param config The [Config] with which DRES was started.
     * @param store The [TransientEntityStore] instance used to access persistent data.
     * @param cache The [CacheManager] instance used to access the media cache.
     */
    fun init(config: Config, store: TransientEntityStore, cache: CacheManager) {

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
            LoginHandler(store),
            LogoutHandler(store),
            ListUsersHandler(store),
            ListActiveUsersHandler(store),
            ShowCurrentUserHandler(store),
            ShowCurrentSessionHandler(),
            CreateUsersHandler(store),
            DeleteUsersHandler(store),
            UpdateUsersHandler(store),
            UserDetailsHandler(store),

            // Media
            PreviewImageHandler(store, cache),
            PreviewVideoHandler(store, cache),
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
            ShowMediaItemHandler(store),
            ResolveMediaItemListByNameHandler(store), // Must be before ListMediaItem
            ListMediaItemHandler(store),
            ListExternalItemHandler(),

            // Competition
            ListEvaluationTemplatesHandler(store),
            CreateEvaluationTemplateHandler(store),
            UpdateEvaluationTemplateHandler(store, config),
            ShowEvaluationTemplateHandler(store),
            DeleteEvaluationTemplateHandler(store),
            ListTeamHandler(store),
            ListTasksHandler(store),
            GetTeamLogoHandler(store),

            // Submission
            LegacySubmissionHandler(store, cache),

            // Log
            QueryLogHandler(),
            ResultLogHandler(),

            // Evaluation
            ListEvaluationInfoHandler(store),
            ListEvaluationStatesHandler(store),
            GetEvaluationInfoHandler(store),
            GetEvaluationStateHandler(store),
            GetTaskHintHandler(store, cache),
            GetTaskTargetHandler(store, cache),
            GetCurrentTaskHandler(store),
            GetSubmissionInfoHandler(store),
            GetSubmissionAfterInfoHandler(store),
            GetSubmissionHistoryInfoHandler(store),

            // Competition run scores
            ListEvaluationScoreHandler(store),
            CurrentTaskScoreHandler(store),
            HistoryTaskScoreHandler(store),
            ListScoreSeriesHandler(store),
            ListScoreboardsHandler(store),
            TeamGroupScoreHandler(store),

            // Evaluation administration
            CreateEvaluationHandler(store, cache),
            StartEvaluationHandler(store),
            StopEvaluationHandler(store),
            NextTaskHandler(store),
            PreviousTaskHandler(store),
            SwitchTaskHandler(store),
            StartTaskHandler(store),
            StopTaskHandler(store),
            AdjustDurationHandler(store),
            AdjustPropertiesHandler(store),
            OverrideAnswerSetVerdictHandler(store),
            ForceViewerHandler(store),
            ListViewersHandler(store),
            ListSubmissionsHandler(store),
            ListPastTaskHandler(store),
            EvaluationOverviewHandler(store),
            ListAllTeamsHandler(store),
            CreateTeamHandler(store),
            UpdateTeamHandler(store),

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
                    corsPluginConfig.reflectClientOrigin = true // anyHost() has similar implications and might be used in production? I'm not sure how to cope with production and dev here simultaneously
                    corsPluginConfig.allowCredentials = true
                }
            }

            it.plugins.register(
                OpenApiPlugin(
                    OpenApiPluginConfiguration()
                        .withDocumentationPath("/swagger-docs")
                        .withDefinitionConfiguration { _, u ->
                            u.withOpenApiInfo { t ->
                                t.title = "DRES API"
                                t.version = DRES.VERSION
                                t.description = "API for DRES (Distributed Retrieval Evaluation Server), Version ${DRES.VERSION}"
                            }
                            u.withSecurity(SecurityComponentConfiguration()
                                .withSecurityScheme("CookieAuth", CookieAuth(AccessManager.SESSION_COOKIE_NAME))
                            )
                        }


                )
            )

            it.plugins.register(ClientOpenApiPlugin())
            it.plugins.register(
                SwaggerPlugin(
                    SwaggerConfiguration().apply {
                        this.version = "4.10.3"
                        this.documentationPath = "/swagger-docs"
                        this.uiPath = "/swagger-ui"
                    }
                )
            )

            it.http.defaultContentType = "application/json"
            it.http.prefer405over404 = true
            it.jetty.server { setupHttpServer() }
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

        }.before { ctx ->

            //check for session cookie
            val cookieId = ctx.cookie(AccessManager.SESSION_COOKIE_NAME)
            if (cookieId != null) {
                ctx.cookie(AccessManager.SESSION_COOKIE_NAME, cookieId, AccessManager.SESSION_COOKIE_LIFETIME) //update cookie lifetime
                ctx.attribute("session", cookieId) //store id in attribute for later use
            }

            //check for query parameter
            val paramId = ctx.queryParam("session")
            if (paramId != null) {
                ctx.attribute("session", paramId) //store id in attribute for later use
            }

            //logging
            logger.info(
                logMarker,
                "${ctx.req().method} request to ${ctx.path()} with params (${
                    ctx.queryParamMap().map { e -> "${e.key}=${e.value}" }.joinToString()
                }) from ${ctx.req().remoteAddr}"
            )
            if (ctx.path().startsWith("/api/")) { //do not cache api requests
                ctx.header("Cache-Control", "no-store")
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


    private val pool = QueuedThreadPool(
        1000, 8, 60000, -1, null, null, NamedThreadFactory("JavalinPool")
    )

    val readyThreadCount: Int
        get() = pool.readyThreads

    private fun setupHttpServer(): Server {
        return Server(pool)
    }

}
