package dres.api.rest

import dev.dres.DRES
import dev.dres.api.rest.AccessManager
import dev.dres.api.rest.handler.ServerError
import dev.dres.api.rest.types.users.ApiRole
import dev.dres.api.rest.types.users.ApiUser
import dev.dres.data.model.config.Config
import dev.dres.data.model.config.CorsConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.net.ServerSocket
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.util.UUID

/**
 * Tests for the access-control and API-surface fixes.
 *
 * These cover decisions that are cheap to get wrong and expensive to get wrong silently:
 * which roles may be stored on a user, what a cross-origin caller is allowed to do, and whether a
 * failed request tells the caller anything about the server's internals.
 */
class AccessControlFixesTest {

    companion object {
        init {
            val audit = Files.createTempDirectory("dres-access-audit-")
            DRES.CONFIG = Config(eventsLocation = Files.createTempDirectory("dres-access-events-"), auditLocation = audit)
            DRES.AUDIT_LOG_ROOT = audit
        }
    }

    private fun session(role: ApiRole): String {
        /* Ids are opaque here: AccessManager only stores and expands them. */
        val token = "access-test-${UUID.randomUUID()}"
        AccessManager.registerUserForSession(token, ApiUser(id = "user-${UUID.randomUUID()}", username = "u", role = role))
        return token
    }

    // ── Role expansion (item 26) ─────────────────────────────────────────────

    /**
     * `ANYONE` describes a request that needs no authentication, so it must not be a role a user can hold:
     * such an account could never log in, because session registration rejects every role it cannot expand.
     */
    @Test
    fun `anyone is not an assignable role`() {
        assertFalse(ApiRole.ANYONE in AccessManager.assignableRoles)
        assertTrue(AccessManager.assignableRoles.containsAll(setOf(ApiRole.VIEWER, ApiRole.PARTICIPANT, ApiRole.JUDGE, ApiRole.ADMIN)))
    }

    /** Every role an API user may have must produce a usable session. */
    @Test
    fun `every assignable role yields a session with permissions`() {
        for (role in AccessManager.assignableRoles) {
            val token = this.session(role)
            assertTrue(
                AccessManager.rolesOfSession(token).isNotEmpty(),
                "Role $role must expand into permissions, otherwise the account cannot do anything"
            )
        }
    }

    /**
     * Regression: a judge could not change its own password because the update handler did not list
     * JUDGE among its permitted roles, while a participant could.
     */
    @Test
    fun `a judge session has the viewer capability a judge needs to render media`() {
        val token = this.session(ApiRole.JUDGE)
        val roles = AccessManager.rolesOfSession(token)

        assertTrue(roles.contains(ApiRole.JUDGE))
        assertTrue(roles.contains(ApiRole.VIEWER), "A judge views runs, so it needs the viewer capability")
    }

    /** Logging out must drop the eligibility entry, not just the session, or it outlives the login. */
    @Test
    fun `deregistering a session drops the user`() {
        val token = this.session(ApiRole.ADMIN)
        val userId = AccessManager.userIdForSession(token)
        assertTrue(userId != null)

        AccessManager.deregisterUserSession(token)

        assertNull(AccessManager.userIdForSession(token))
        assertTrue(AccessManager.rolesOfSession(token).isEmpty())
    }

    // ── Error responses (item 25) ────────────────────────────────────────────

    /** The generic message must not be built from exception text, which names files, ids and entities. */
    @Test
    fun `the server error message carries no exception detail`() {
        assertEquals("Internal server error.", ServerError.MESSAGE)
        assertFalse(ServerError.MESSAGE.contains("Exception"))
        assertFalse(ServerError.MESSAGE.contains("/"))
    }

    // ── CORS (item 24) ───────────────────────────────────────────────────────

    private fun corsHeaders(origin: String?, cors: CorsConfig = CorsConfig()): Map<String, String> {
        val port = ServerSocket(0).use { it.localPort }
        val app = dev.dres.api.rest.JavalinTestSupport.startCorsEcho(port) { cors }
        return try {
            val builder = HttpRequest.newBuilder(URI("http://localhost:$port/api/v2/status/info")).GET()
            if (origin != null) builder.header("Origin", origin)
            val response = HttpClient.newHttpClient().send(builder.build(), HttpResponse.BodyHandlers.discarding())
            response.headers().map().entries
                .filter { it.key.lowercase().startsWith("access-control") }
                .associate { it.key.lowercase() to it.value.joinToString(",") }
        } finally {
            app.stop()
        }
    }

    /**
     * Regression: the CORS plugin reflected the caller's own origin while also allowing credentials, so any
     * page on the internet could read authenticated responses from an installation that is not behind TLS.
     */
    @Test
    fun `a foreign origin is not granted credentialed access by default`() {
        val headers = this.corsHeaders("https://evil.example")

        assertFalse(
            headers["access-control-allow-credentials"] == "true" && headers["access-control-allow-origin"] == "https://evil.example",
            "A default installation must not let an arbitrary origin send credentialed requests: $headers"
        )
    }

    @Test
    fun `a configured origin is granted a reflected origin`() {
        val headers = this.corsHeaders("https://dres.example", CorsConfig(allowedOrigins = listOf("dres.example")))

        assertEquals("https://dres.example", headers["access-control-allow-origin"])
        assertEquals("true", headers["access-control-allow-credentials"])
    }
}
