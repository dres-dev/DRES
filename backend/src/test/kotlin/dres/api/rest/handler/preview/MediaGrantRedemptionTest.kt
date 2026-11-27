package dres.api.rest.handler.preview

import dev.dres.api.rest.AccessManager
import dev.dres.api.rest.handler.preview.GetMediaGrantHandler
import dev.dres.api.rest.handler.preview.MediaGrantPayload
import dev.dres.api.rest.handler.preview.MediaGrantRegistry
import dev.dres.api.rest.handler.preview.MediaRequestGuard
import dev.dres.api.rest.types.users.ApiRole
import dev.dres.mgmt.cache.CacheManager
import dres.integration.AbstractDresIntegrationTest
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.path
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.ServerSocket
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.util.UUID

/**
 * End-to-end tests for grant redemption over HTTP.
 *
 * The unit tests in [MediaGrantRegistryTest] pin the registry's rules; this class pins the part that
 * actually reaches a client: the route, the session requirement, the error codes, and the fact that the
 * bytes are served with a private, lifetime-bounded `Cache-Control` header.
 */
class MediaGrantRedemptionTest : AbstractDresIntegrationTest() {

    private val http: HttpClient = HttpClient.newHttpClient()
    private val started = mutableListOf<Javalin>()
    private lateinit var cache: CacheManager

    @BeforeEach
    fun resetState() {
        MediaGrantRegistry.clear()
        MediaRequestGuard.reset()
        /* CacheManager reads DRES.CONFIG.cache.cachePath and locates FFmpeg relative to DRES.DATA_ROOT, so
         * both globals have to exist before it is constructed. Payloads of the `Preview` kind are served
         * straight from disk, so no rendering happens in this test. */
        val config = dev.dres.data.model.config.Config(eventsLocation = Files.createTempDirectory("dres-grant-events-"))
        dev.dres.DRES.CONFIG = config
        dev.dres.DRES.DATA_ROOT = Files.createTempDirectory("dres-grant-data-")
        this.cache = CacheManager(config, store)
    }

    @AfterEach
    fun stopServers() {
        this.started.forEach { runCatching { it.stop() } }
        this.started.clear()
        MediaGrantRegistry.clear()
    }

    /** Starts the handler the way `RestApi` mounts it, including the cookie→session step. */
    private fun startServer(): Int {
        val port = ServerSocket(0).use { it.localPort }
        val handler = GetMediaGrantHandler(store, this.cache)
        val app = Javalin.create { config ->
            config.http.defaultContentType = "application/json"
            config.routes.apiBuilder {
                path("api") {
                    path("v2") {
                        path(handler.route) {
                            get(handler::get, *handler.permittedRoles.toTypedArray())
                        }
                    }
                }
            }
            /* Mirrors the production `routes.before`: the session comes from the cookie or ?session=. */
            config.routes.before { ctx ->
                ctx.cookie(AccessManager.SESSION_COOKIE_NAME)?.let { ctx.attribute("session", it) }
                ctx.queryParam("session")?.let { ctx.attribute("session", it) }
            }
        }.start(port)
        this.started += app
        return port
    }

    private fun get(port: Int, grantId: String, session: String?): HttpResponse<ByteArray> {
        val builder = HttpRequest.newBuilder(URI("http://localhost:$port/api/v2/media/grant/$grantId"))
        if (session != null) builder.header("Cookie", "${AccessManager.SESSION_COOKIE_NAME}=$session")
        return this.http.send(builder.GET().build(), HttpResponse.BodyHandlers.ofByteArray())
    }

    private fun tempMedia(bytes: ByteArray = "granted-media-bytes".toByteArray()): java.nio.file.Path {
        val path = Files.createTempFile("dres-grant-", ".mp4")
        Files.write(path, bytes)
        return path
    }

    @Test
    fun `a prepared payload is served to the session it was minted for`() {
        val file = this.tempMedia()
        val session = "grant-session-${UUID.randomUUID()}"
        val grant = MediaGrantRegistry.mint(session, "evaluation-1", "task-1", MediaGrantPayload.Preview(file))

        val response = this.get(this.startServer(), grant.id, session)

        assertEquals(200, response.statusCode())
        assertArrayEquals(Files.readAllBytes(file), response.body())
        assertTrue(
            response.headers().firstValue("Cache-Control").orElse("").startsWith("private, max-age="),
            "A grant response must stay out of shared caches and expire with the grant"
        )
    }

    /** The whole point of grants: the URL alone is useless to another session. */
    @Test
    fun `another session cannot redeem a grant`() {
        val grant = MediaGrantRegistry.mint("session-owner", "evaluation-1", "task-1", MediaGrantPayload.Preview(this.tempMedia()))

        val response = this.get(this.startServer(), grant.id, "session-intruder")

        assertEquals(410, response.statusCode())
    }

    @Test
    fun `an anonymous request is rejected before the grant is touched`() {
        val grant = MediaGrantRegistry.mint("session-owner", "evaluation-1", "task-1", MediaGrantPayload.Preview(this.tempMedia()))

        val response = this.get(this.startServer(), grant.id, null)

        assertEquals(401, response.statusCode())
        assertEquals(MediaGrantRegistry.DEFAULT_BUDGET, MediaGrantRegistry.get(grant.id)?.remainingBudget)
    }

    @Test
    fun `an unknown grant id is rejected`() {
        val response = this.get(this.startServer(), "not-a-grant", "some-session")
        assertEquals(410, response.statusCode())
    }

    @Test
    fun `an expired grant is rejected`() {
        val grant = MediaGrantRegistry.mint(
            session = "session-a",
            evaluationId = "evaluation-1",
            taskTemplateId = "task-1",
            payload = MediaGrantPayload.Preview(this.tempMedia()),
            ttlMs = 1L
        )
        Thread.sleep(5)

        val response = this.get(this.startServer(), grant.id, "session-a")

        assertEquals(410, response.statusCode())
    }

    /** Ending the run must make outstanding URLs useless, not merely expire them eventually. */
    @Test
    fun `revoking an evaluation makes its grants unusable`() {
        val grant = MediaGrantRegistry.mint("session-a", "evaluation-1", "task-1", MediaGrantPayload.Preview(this.tempMedia()))
        val port = this.startServer()
        MediaGrantRegistry.revokeEvaluation("evaluation-1")

        assertEquals(410, this.get(port, grant.id, "session-a").statusCode())
    }

    @Test
    fun `a grant whose payload vanished reports a missing file instead of serving nothing`() {
        val file = this.tempMedia()
        val grant = MediaGrantRegistry.mint("session-a", "evaluation-1", "task-1", MediaGrantPayload.Preview(file))
        Files.delete(file)

        val response = this.get(this.startServer(), grant.id, "session-a")

        assertEquals(404, response.statusCode())
    }

    @Test
    fun `the redemption budget is enforced over HTTP`() {
        val grant = MediaGrantRegistry.mint(
            session = "session-a",
            evaluationId = "evaluation-1",
            taskTemplateId = "task-1",
            payload = MediaGrantPayload.Preview(this.tempMedia()),
            budget = 1
        )
        val port = this.startServer()

        assertEquals(200, this.get(port, grant.id, "session-a").statusCode())
        assertEquals(410, this.get(port, grant.id, "session-a").statusCode())
    }
}
