package dres.api.rest.handler.preview

import dev.dres.api.rest.handler.preview.MediaGrantPayload
import dev.dres.api.rest.handler.preview.MediaGrantRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Paths

/**
 * Tests for [MediaGrantRegistry], the single place that decides whether a caller may read a
 * specific media payload.
 */
class MediaGrantRegistryTest {

    private val evaluationId = "evaluation-1"
    private val taskId = "task-1"
    private val session = "session-a"

    @BeforeEach
    fun clearRegistry() = MediaGrantRegistry.clear()

    private fun mint(
        payload: MediaGrantPayload = MediaGrantPayload.Preview(Paths.get("/tmp/preview.mp4")),
        session: String = this.session,
        task: String? = this.taskId,
        ttlMs: Long = MediaGrantRegistry.DEFAULT_TTL_MS,
        budget: Int = MediaGrantRegistry.DEFAULT_BUDGET
    ) = MediaGrantRegistry.mint(
        session = session,
        evaluationId = this.evaluationId,
        taskTemplateId = task,
        payload = payload,
        ttlMs = ttlMs,
        budget = budget
    )

    @Test
    fun `a minted grant can be redeemed by the session it was minted for`() {
        val grant = this.mint()
        val redeemed = MediaGrantRegistry.redeem(grant.id, this.session)
        assertNotNull(redeemed)
        assertSame(grant, redeemed)
        assertEquals(MediaGrantRegistry.DEFAULT_BUDGET - 1, redeemed!!.remainingBudget)
    }

    @Test
    fun `a grant is bound to one session`() {
        val grant = this.mint()
        assertNull(MediaGrantRegistry.redeem(grant.id, "session-b"))
        assertEquals(MediaGrantRegistry.DEFAULT_BUDGET, grant.remainingBudget)
        assertNotNull(MediaGrantRegistry.redeem(grant.id, this.session))
    }

    @Test
    fun `an anonymous caller cannot redeem a grant`() {
        val grant = this.mint()
        assertNull(MediaGrantRegistry.redeem(grant.id, null))
    }

    @Test
    fun `an unknown grant id is rejected`() {
        assertNull(MediaGrantRegistry.redeem("does-not-exist", this.session))
    }

    @Test
    fun `an expired grant is rejected and dropped`() {
        val grant = this.mint(ttlMs = 1_000L)
        assertNull(MediaGrantRegistry.redeem(grant.id, this.session, now = grant.expiresAt))
        assertNull(MediaGrantRegistry.get(grant.id))
        assertNull(MediaGrantRegistry.redeem(grant.id, this.session))
    }

    @Test
    fun `the redemption budget is finite`() {
        val grant = this.mint(budget = 2)
        assertNotNull(MediaGrantRegistry.redeem(grant.id, this.session))
        assertNotNull(MediaGrantRegistry.redeem(grant.id, this.session))
        assertNull(MediaGrantRegistry.redeem(grant.id, this.session))
    }

    @Test
    fun `ending an evaluation revokes its grants`() {
        val grant = this.mint()
        val foreign = this.mint(session = "session-c").let { it }

        MediaGrantRegistry.revokeEvaluation(this.evaluationId)

        assertNull(MediaGrantRegistry.get(grant.id))
        assertNull(MediaGrantRegistry.get(foreign.id))
        assertEquals(0, MediaGrantRegistry.size)
    }

    @Test
    fun `ending a task revokes only that task's grants`() {
        val first = this.mint(task = "task-1")
        val second = this.mint(task = "task-2")

        MediaGrantRegistry.revokeTask(this.evaluationId, "task-1")

        assertNull(MediaGrantRegistry.get(first.id))
        assertNotNull(MediaGrantRegistry.get(second.id))
    }

    @Test
    fun `grant identifiers are unguessable and unique`() {
        val ids = (1..200).map { this.mint().id }
        assertEquals(ids.size, ids.toSet().size)
        /* 32 bytes of CSPRNG output in unpadded url-safe base64 is 43 characters. */
        assertTrue(ids.all { it.length >= 43 })
        assertTrue(ids.all { it.matches(Regex("[A-Za-z0-9_-]+")) })
    }

    @Test
    fun `a payload is preserved verbatim`() {
        val payload = MediaGrantPayload.ItemSegment("item-42", 4_000L, 12_000L)
        val grant = this.mint(payload = payload)
        assertEquals(payload, MediaGrantRegistry.get(grant.id)?.payload)
        /* A grant always names exactly one payload, so redemption can never fall back to
         * "any media of this item". */
        assertNull(MediaGrantRegistry.redeem(grant.id, "session-b"))
    }
}
