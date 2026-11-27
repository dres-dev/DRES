package dres.api.rest.handler.preview

import dev.dres.DRES
import dev.dres.api.rest.AccessManager
import dev.dres.api.rest.handler.preview.GetExternalMediaHandler
import dev.dres.api.rest.handler.preview.GetMediaHandler
import dev.dres.api.rest.handler.preview.MediaRequestGuard
import dev.dres.api.rest.handler.preview.PreviewImageTimelessHandler
import dev.dres.api.rest.types.users.ApiRole
import dev.dres.api.rest.types.users.ApiUser
import dev.dres.data.model.admin.DbUser
import dev.dres.data.model.config.Config
import dev.dres.data.model.media.DbMediaCollection
import dev.dres.data.model.media.DbMediaItem
import dev.dres.data.model.media.DbMediaType
import dev.dres.data.model.run.InteractiveSynchronousEvaluation
import dev.dres.data.model.run.RunActionContext
import dev.dres.data.model.template.DbEvaluationTemplate
import dev.dres.data.model.template.task.DbTaskGroup
import dev.dres.data.model.template.team.DbTeam
import dev.dres.mgmt.EvaluationManager
import dev.dres.mgmt.TemplateManager
import dev.dres.mgmt.cache.CacheManager
import dev.dres.run.InteractiveSynchronousRunManager
import dev.dres.run.RunExecutor
import dres.integration.AbstractDresIntegrationTest
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.path
import kotlinx.dnq.query.asSequence
import kotlinx.dnq.query.eq
import kotlinx.dnq.query.filter
import kotlinx.dnq.query.first
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.ServerSocket
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID

/**
 * Tests for the media scoping rules applied to whole originals, previews and external files.
 *
 * Rules under test (as decided for this codebase):
 *  - admins may always fetch media;
 *  - a judge is treated exactly like a viewer of the runs it judges;
 *  - a participant is treated like a viewer iff the run allows participants to view, otherwise all of
 *    its media access is denied;
 *  - media outside every run the caller may observe is denied, even for a VIEWER of a different run;
 *  - an anonymous request is rejected outright.
 *
 * The tests mount the real handlers on a real Javalin instance so that the whole chain is exercised:
 * route -> session resolution (cookie or `?session=`) -> media lookup -> [MediaAuthorization] -> bytes.
 */
class MediaAuthorizationTest : AbstractDresIntegrationTest() {

    private val http: HttpClient = HttpClient.newHttpClient()
    private val started = mutableListOf<Javalin>()
    private val scheduled = mutableListOf<InteractiveSynchronousRunManager>()

    private lateinit var mediaRoot: Path
    private lateinit var cache: CacheManager

    @BeforeEach
    fun prepare() {
        /* CacheManager reads DRES.CONFIG.cache.cachePath and DRES.DATA_ROOT for FFmpeg; neither is
         * exercised here because every assertion is about the access decision that precedes rendering. */
        val config = Config(eventsLocation = Files.createTempDirectory("dres-auth-events-"))
        DRES.CONFIG = config
        DRES.DATA_ROOT = Files.createTempDirectory("dres-auth-data-")
        DRES.EXTERNAL_ROOT = Files.createTempDirectory("dres-auth-external-")
        this.cache = CacheManager(config, store)
        this.mediaRoot = Files.createTempDirectory("dres-auth-media-")
        MediaRequestGuard.reset()
    }

    @AfterEach
    fun cleanupServers() {
        this.started.forEach { runCatching { it.stop() } }
        this.started.clear()
        this.scheduled.forEach { manager -> runCatching { manager.end(RunActionContext.INTERNAL) } }
        this.scheduled.clear()
    }

    // ── Fixtures ──────────────────────────────────────────────────────────────

    /**
     * A collection whose items really exist on disk, unlike [createTestCollection], so that the
     * "allowed" cases can assert actual bytes.
     */
    private fun createOnDiskCollection(nameSuffix: String = UUID.randomUUID().toString()): Pair<DbMediaCollection, Path> {
        val dir = Files.createDirectories(this.mediaRoot.resolve("col-$nameSuffix"))
        val file = dir.resolve("clip.mp4")
        Files.write(file, "media-bytes-for-${nameSuffix}".toByteArray())
        val collection = store.transactional {
            val col = DbMediaCollection.new {
                name = "on-disk-$nameSuffix"
                path = dir.toString()
            }
            col.items.add(DbMediaItem.new {
                name = "clip"
                type = DbMediaType.VIDEO
                location = "clip.mp4"
                fps = 25f
                durationMs = 10_000L
            })
            col
        }
        return collection to file
    }

    private fun addViewer(template: DbEvaluationTemplate, user: DbUser) = store.transactional {
        template.viewers.add(DbUser.filter { it.id eq user.id }.first())
    }

    private fun addJudge(template: DbEvaluationTemplate, user: DbUser) = store.transactional {
        template.judges.add(DbUser.filter { it.id eq user.id }.first())
    }

    private fun addTeamMember(template: DbEvaluationTemplate, user: DbUser, teamIndex: Int = 0) = store.transactional {
        val team = template.teams.asSequence().sortedBy { it.name }.toList()[teamIndex]
        team.users.add(DbUser.filter { it.id eq user.id }.first())
    }

    private fun user(): DbUser = createTestUser()

    private fun session(user: DbUser, role: ApiRole): String {
        val (id, name) = store.transactional(true) { user.id!! to user.username }
        val token = "auth-test-${UUID.randomUUID()}"
        AccessManager.registerUserForSession(token, ApiUser(id = id, username = name, role = role))
        return token
    }

    /**
     * Builds and schedules a synchronous run for [collection] and returns its id.
     *
     * [configureTemplate] runs before the instance is created, because a run manager snapshots the
     * template when it is constructed.
     */
    private fun startRun(
        collection: DbMediaCollection,
        participantCanView: Boolean = true,
        configureTemplate: (DbEvaluationTemplate) -> Unit = {}
    ): String {
        TemplateManager.init(store)
        EvaluationManager.init(store)
        val template = createTemplateShell("media-auth-${UUID.randomUUID()}", teamCount = 1)
        val group: DbTaskGroup = addTaskTypeAndGroup(template, "KIS", "KIS", "KIS", "MEDIA_ITEM")
        addTask(template, group, collection, "Task-0", idx = 0)
        configureTemplate(template)
        val dbEvaluation = createEvaluation(template, "media-auth-run-${UUID.randomUUID()}", "INTERACTIVE_SYNCHRONOUS")
        store.transactional { dbEvaluation.participantCanView = participantCanView }
        val evaluation = store.transactional(true) { InteractiveSynchronousEvaluation(store, dbEvaluation) }
        val manager = store.transactional(true) { InteractiveSynchronousRunManager(evaluation, store) }
        RunExecutor.schedule(manager)
        manager.start(RunActionContext.INTERNAL)
        this.scheduled += manager
        return manager.id
    }

    /** Mounts the three media handlers the same way `RestApi` does, including session resolution. */
    private fun startServer(): Int {
        val port = ServerSocket(0).use { it.localPort }
        val media = GetMediaHandler(store)
        val preview = PreviewImageTimelessHandler(store, this.cache)
        val external = GetExternalMediaHandler()
        val app = Javalin.create { config ->
            config.routes.apiBuilder {
                path("api") {
                    path("v2") {
                        path(media.route) { get(media::get, *media.permittedRoles.toTypedArray()) }
                        path(preview.route) { get(preview::get, *preview.permittedRoles.toTypedArray()) }
                        path(external.route) { get(external::get, *external.permittedRoles.toTypedArray()) }
                    }
                }
            }
            /* Mirrors RestApi: cookie first, then the ?session= parameter that client builders rely on. */
            config.routes.before { ctx ->
                ctx.cookie(AccessManager.SESSION_COOKIE_NAME)?.let { ctx.attribute("session", it) }
                ctx.queryParam("session")?.let { ctx.attribute("session", it) }
            }
            config.routes.exception(dev.dres.api.rest.types.status.ErrorStatusException::class.java) { e, ctx ->
                ctx.status(e.statusCode).json(e.errorStatus)
            }
        }.start(port)
        this.started += app
        return port
    }

    private fun get(port: Int, path: String, session: String?): HttpResponse<ByteArray> {
        val builder = HttpRequest.newBuilder(URI("http://localhost:$port$path"))
        if (session != null) builder.header("Cookie", "${AccessManager.SESSION_COOKIE_NAME}=$session")
        return this.http.send(builder.GET().build(), HttpResponse.BodyHandlers.ofByteArray())
    }

    private fun itemId(collection: DbMediaCollection): String = store.transactional(true) {
        collection.items.first().mediaItemId
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    fun `an admin may fetch an item original`() {
        val (collection, file) = this.createOnDiskCollection()
        this.startRun(collection)
        val port = this.startServer()

        val response = this.get(port, "/api/v2/media/${this.itemId(collection)}", this.session(this.user(), ApiRole.ADMIN))

        assertEquals(200, response.statusCode())
        assertArrayEquals(Files.readAllBytes(file), response.body())
    }

    @Test
    fun `a registered viewer may fetch an item original`() {
        val (collection, _) = this.createOnDiskCollection()
        val viewer = this.user()
        this.startRun(collection) { template -> this.addViewer(template, viewer) }
        val port = this.startServer()

        val response = this.get(port, "/api/v2/media/${this.itemId(collection)}", this.session(viewer, ApiRole.VIEWER))

        assertEquals(200, response.statusCode())
    }

    /** A judge counts as a viewer. */
    @Test
    fun `a judge of the run may fetch an item original`() {
        val (collection, _) = this.createOnDiskCollection()
        val judge = this.user()
        this.startRun(collection) { template -> this.addJudge(template, judge) }
        val port = this.startServer()

        val response = this.get(port, "/api/v2/media/${this.itemId(collection)}", this.session(judge, ApiRole.JUDGE))

        assertEquals(200, response.statusCode())
    }

    /** Participant plus `participantCanView` means exactly the viewer treatment. */
    @Test
    fun `a participant may fetch an item original when the run allows participants to view`() {
        val (collection, _) = this.createOnDiskCollection()
        val participant = this.user()
        this.startRun(collection, participantCanView = true) { template -> this.addTeamMember(template, participant) }
        val port = this.startServer()

        val response = this.get(port, "/api/v2/media/${this.itemId(collection)}", this.session(participant, ApiRole.PARTICIPANT))

        assertEquals(200, response.statusCode())
    }

    /** Without the flag a participant is denied everything, even for its own task's collection. */
    @Test
    fun `a participant is denied when the run forbids participants to view`() {
        val (collection, _) = this.createOnDiskCollection()
        val participant = this.user()
        this.startRun(collection, participantCanView = false) { template -> this.addTeamMember(template, participant) }
        val port = this.startServer()

        val response = this.get(port, "/api/v2/media/${this.itemId(collection)}", this.session(participant, ApiRole.PARTICIPANT))

        assertEquals(403, response.statusCode())
    }

    /** Media of a run the caller has no relation to must stay unreachable for a plain viewer. */
    @Test
    fun `a viewer of another run is denied`() {
        val (otherCollection, _) = this.createOnDiskCollection()
        this.startRun(otherCollection)

        val (collection, _) = this.createOnDiskCollection()
        val viewer = this.user()
        this.startRun(collection) { template -> this.addViewer(template, viewer) }
        val port = this.startServer()

        val response = this.get(port, "/api/v2/media/${this.itemId(otherCollection)}", this.session(viewer, ApiRole.VIEWER))

        assertEquals(403, response.statusCode())
    }

    @Test
    fun `an anonymous request is rejected`() {
        val (collection, _) = this.createOnDiskCollection()
        this.startRun(collection)
        val port = this.startServer()

        val response = this.get(port, "/api/v2/media/${this.itemId(collection)}", null)

        assertEquals(401, response.statusCode())
    }

    /** The same scoping applies to cached previews, which reveal content as well. */
    @Test
    fun `a participant is denied a preview when the run forbids participants to view`() {
        val (collection, _) = this.createOnDiskCollection()
        val participant = this.user()
        this.startRun(collection, participantCanView = false) { template -> this.addTeamMember(template, participant) }
        val port = this.startServer()

        val response = this.get(port, "/api/v2/preview/${this.itemId(collection)}", this.session(participant, ApiRole.PARTICIPANT))

        assertEquals(403, response.statusCode())
    }

    @Test
    fun `an admin may request a preview`() {
        val (collection, _) = this.createOnDiskCollection()
        this.startRun(collection)
        val port = this.startServer()

        /* 200/202 both mean "authorised": the image either exists already or is being rendered. */
        val response = this.get(port, "/api/v2/preview/${this.itemId(collection)}", this.session(this.user(), ApiRole.ADMIN))

        assertEquals(true, response.statusCode() in setOf(200, 202), "Expected an authorised preview, got ${response.statusCode()}")
    }

    @Test
    fun `external media is denied to a session without a matching run`() {
        val fileName = "promo-${UUID.randomUUID()}.mp4"
        Files.write(DRES.EXTERNAL_ROOT.resolve(fileName), "external-bytes".toByteArray())

        val (collection, _) = this.createOnDiskCollection()
        val viewer = this.user()
        this.startRun(collection) { template -> this.addViewer(template, viewer) }
        val port = this.startServer()

        val response = this.get(port, "/api/v2/media/external/$fileName", this.session(viewer, ApiRole.VIEWER))

        assertEquals(403, response.statusCode())
    }

    @Test
    fun `external media referenced by an observed run is served`() {
        val fileName = "promo-${UUID.randomUUID()}.mp4"
        Files.write(DRES.EXTERNAL_ROOT.resolve(fileName), "external-bytes".toByteArray())

        val (collection, _) = this.createOnDiskCollection()
        val viewer = this.user()
        this.startRun(collection) { template ->
            this.addViewer(template, viewer)
            /* An external hint is how a template points at DRES.EXTERNAL_ROOT. */
            store.transactional {
                val task = template.tasks.first()
                val hint = dev.dres.data.model.template.task.DbHint.new {
                    this.task = task
                    this.type = dev.dres.data.model.template.task.DbHintType.VIDEO
                    this.path = fileName
                    this.start = 0L
                    /* A VIDEO hint without an item carries its range in the temporal columns. */
                    this.temporalRangeStart = 0L
                    this.temporalRangeEnd = 1000L
                }
                task.hints.add(hint)
            }
        }
        val port = this.startServer()

        val response = this.get(port, "/api/v2/media/external/$fileName", this.session(viewer, ApiRole.VIEWER))

        assertEquals(200, response.statusCode())
    }

    @Test
    fun `the session query parameter is honoured for media`() {
        val (collection, _) = this.createOnDiskCollection()
        this.startRun(collection)
        val port = this.startServer()
        val token = this.session(this.user(), ApiRole.ADMIN)

        val response = this.get(port, "/api/v2/media/${this.itemId(collection)}?session=$token", null)

        assertEquals(200, response.statusCode())
    }
}
