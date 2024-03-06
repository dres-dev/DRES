package dev.dres.api.rest

import GetTaskHintHandler
import dev.dres.DRES
import dev.dres.api.rest.handler.*
import dev.dres.api.rest.handler.collection.*
import dev.dres.api.rest.handler.download.EvaluationDownloadHandler
import dev.dres.api.rest.handler.download.EvaluationTemplateDownloadHandler
import dev.dres.api.rest.handler.download.ScoreDownloadHandler
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
import dev.dres.api.rest.handler.preview.*
import dev.dres.api.rest.handler.template.*
import dev.dres.api.rest.handler.scores.ListEvaluationScoreHandler
import dev.dres.api.rest.handler.submission.SubmissionHandler
import dev.dres.api.rest.handler.system.CurrentTimeHandler
import dev.dres.api.rest.handler.system.InfoHandler
import dev.dres.api.rest.handler.system.LoginHandler
import dev.dres.api.rest.handler.system.LogoutHandler
import dev.dres.api.rest.handler.users.*
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.users.ApiRole
import dev.dres.data.model.config.Config
import dev.dres.mgmt.cache.CacheManager
import dev.dres.run.RunExecutor
import dev.dres.utilities.NamedThreadFactory
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.http.staticfiles.Location
import io.javalin.community.ssl.SslPlugin
import io.javalin.http.Cookie
import io.javalin.http.HttpStatus
import io.javalin.http.SameSite
import io.javalin.openapi.CookieAuth
import io.javalin.openapi.OpenApiContact
import io.javalin.openapi.OpenApiLicense
import io.javalin.openapi.plugin.*
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

    const val LATEST_API_VERSION = "v2"

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
        val apiRestHandlers = listOfNotNull(

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
            PreviewImageHandler(
                store,
                cache
            ), /* [PreviewImageHandler] vs [PreviewImageTimelessHandler]: Optional path parameters are not allowed in OpenApi. [PreviewImageHandler] has timestamp as path parameter and must be initialised first */
            PreviewImageTimelessHandler(
                store,
                cache
            ), /* [PreviewImageHandler] vs [PreviewImageTimelessHandler]: Optional path parameters are not allowed in OpenApi */
            PreviewVideoHandler(store, cache),
            GetExternalMediaHandler(), // Must be registered before GetMediaHandler, as route is similar
            GetMediaHandler(store),

            // Collection
            ListCollectionHandler(),
            ShowCollectionHandler(),
            AddCollectionHandler(),
            UpdateCollectionHandler(),
            DeleteCollectionHandler(),
            AddMediaItemHandler(),
            UpdateMediaItemHandler(),
            DeleteMediaItemHandler(),
            RandomMediaItemHandler(), // Must be before ListMediaItem
            ShowMediaItemHandler(),
            ResolveMediaItemListByNameHandler(), // Must be before ListMediaItem
            ListMediaItemHandler(),
            if (DRES.CONFIG.externalMediaEndpointsEnabled) {
                UploadExternalItemHandler()
            } else {
                null
            },
            ListExternalItemHandler(),
            FindExternalItemHandler(),
            if (DRES.CONFIG.externalMediaEndpointsEnabled) {
                DeleteExternalItemHandler()
            } else {
                null
            }, // Must be last of external/ route


            // Template
            ListEvaluationTemplatesHandler(),
            CreateEvaluationTemplateHandler(),
            UpdateEvaluationTemplateHandler(),
            CloneEvaluationTemplateHandler(),
            ShowEvaluationTemplateHandler(),
            DeleteEvaluationTemplateHandler(),
            ListTeamHandler(),
            ListTasksHandler(),
            ListTaskTypePresetsHandler(),
            GetTeamLogoHandler(),

            // Submission
            SubmissionHandler(store),

            // Log
            QueryLogHandler(),
            ResultLogHandler(),

            // Evaluation
            ListEvaluationInfoHandler(),
            ListEvaluationStatesHandler(),
            GetEvaluationInfoHandler(),
            GetEvaluationStateHandler(),
            GetTaskHintHandler(store, cache),
            GetTaskTargetHandler(store, cache),
            GetCurrentTaskHandler(),
            GetSubmissionInfoHandler(store),
            GetSubmissionAfterInfoHandler(),
            GetSubmissionHistoryInfoHandler(),
            ViewerReadyHandler(),

            // Evaluation run scores
            ListEvaluationScoreHandler(),
            CurrentTaskScoreHandler(),
            HistoryTaskScoreHandler(),
            ListScoreSeriesHandler(),
            ListScoreboardsHandler(),
            TeamGroupScoreHandler(),

            // Evaluation administration
            CreateEvaluationHandler(store, cache),
            StartEvaluationHandler(),
            StopEvaluationHandler(),
            NextTaskHandler(),
            PreviousTaskHandler(),
            SwitchTaskHandler(),
            StartTaskHandler(),
            StopTaskHandler(),
            AdjustDurationHandler(),
            AdjustPropertiesHandler(),
            OverrideAnswerSetVerdictHandler(store),
            ForceViewerHandler(),
            ListViewersHandler(),
            ListSubmissionsHandler(),
            ListPastTaskHandler(),
            EvaluationOverviewHandler(),
            ListAllTeamsHandler(store),
            CreateTeamHandler(store),
            UpdateTeamHandler(store),
            GetEvaluationHandler(store),

            // Judgement
            DequeueJudgementHandler(),
            DequeueVoteHandler(),
            PostJudgementHandler(),
            PostVoteHandler(),
            JudgementStatusHandler(),

            // Status
            CurrentTimeHandler(),
            InfoHandler(),

            //API Client
            ClientListEvaluationsHandler(),
            ClientTaskInfoHandler(),

            // Downloads
            EvaluationDownloadHandler(store),
            EvaluationTemplateDownloadHandler(store),
            ScoreDownloadHandler()
        )

        javalin = Javalin.create { it ->

            it.jsonMapper(KotlinxJsonMapper)

            it.bundledPlugins.enableCors { cors ->
                cors.addRule { corsPluginConfig ->
                    corsPluginConfig.reflectClientOrigin =
                        true // anyHost() has similar implications and might be used in production? I'm not sure how to cope with production and dev here simultaneously
                    corsPluginConfig.allowCredentials = true
                }
            }

            it.registerPlugin(
                OpenApiPlugin{ oapConfig ->
                    oapConfig
                        .withDocumentationPath("/openapi.json")
                        .withDefinitionConfiguration { version, openApiDef ->
                            openApiDef
                                .withInfo { info ->
                                info.title = "DRES API"
                                info.version = DRES.VERSION
                                info.description =
                                    "API for DRES (Distributed Retrieval Evaluation Server), Version ${DRES.VERSION}"
                                val contact = OpenApiContact()
                                contact.url = "https://dres.dev"
                                contact.name = "The DRES Dev Team"
                                info.contact = contact
                                val license = OpenApiLicense()
                                license.name = "MIT"
                                info.license = license
                             }

                            .withSecurity(
                                SecurityComponentConfiguration()
                                    .withSecurityScheme("CookieAuth", CookieAuth(AccessManager.SESSION_COOKIE_NAME))
                            )
                        }
                }
            )

            it.registerPlugin(ClientOpenApiPlugin())
            it.registerPlugin(SwaggerPlugin{ swaggerConfig ->
                swaggerConfig.documentationPath = "/openapi.json"
                swaggerConfig.uiPath = "/swagger-ui"
            })
            it.registerPlugin(SwaggerPlugin{ swaggerConfig ->
                swaggerConfig.documentationPath = "/clientapi.json"
                swaggerConfig.uiPath = "/swagger-client"
                swaggerConfig.title = "Client Swagger UI"
            })

            it.http.defaultContentType = "application/json"
            it.http.prefer405over404 = true
            it.http.maxRequestSize = 20 * 1024 * 1024 //20mb
            it.jetty.threadPool = pool
            it.staticFiles.add("html", Location.CLASSPATH)
            it.spaRoot.addFile("/vote", "vote/index.html")
            it.spaRoot.addFile("/", "html/index.html")

            if (config.enableSsl) {
                val ssl = SslPlugin { conf ->
                    conf.keystoreFromPath(config.keystorePath, config.keystorePassword)
                    conf.http2 = true
                    conf.secure = true
                    conf.insecurePort = config.httpPort
                    conf.securePort = config.httpsPort
                    conf.sniHostCheck = false
                }
                it.registerPlugin(ssl)
            }

            it.router.apiBuilder{
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
                    //ws("ws/run", runExecutor)
                }
            }

        }.beforeMatched{ctx ->
            /* BeforeMatched handlers are only matched if the request will be matched */
            /* See https://javalin.io/migration-guide-javalin-5-to-6 */
            if(! AccessManager.hasAccess(ctx)){
                ctx.status(HttpStatus.UNAUTHORIZED)
                ctx.skipRemainingHandlers()
            }
        }.before { ctx ->
            /* Before are matched before every request, including static files */

            //check for session cookie
            val cookieId = ctx.cookie(AccessManager.SESSION_COOKIE_NAME)
            if (cookieId != null) {
                val cookie = Cookie(
                    AccessManager.SESSION_COOKIE_NAME,
                    cookieId,
                    maxAge = AccessManager.SESSION_COOKIE_LIFETIME,
                    secure = true,
                    sameSite = SameSite.NONE
                )
                ctx.cookie(cookie) //update cookie lifetime
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
        32,
        10,
        60000,
        -1,
        null,
        null,
        NamedThreadFactory("JavalinPool")
    )

    val readyThreadCount: Int
        get() = pool.readyThreads

    private fun setupHttpServer(): Server {
        return Server(pool)
    }

}
