package dres.api.rest.handler.preview

import dev.dres.api.rest.handler.preview.MediaRequestGuard
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class MediaRequestGuardTest {

    @BeforeEach
    fun resetGuard() = MediaRequestGuard.reset()

    @Test
    fun `requests up to the budget are counted, the next one is over`() {
        var last = 0
        repeat(MediaRequestGuard.MAX_REQUESTS_PER_WINDOW) {
            last = MediaRequestGuard.record("session-a", 1_000L)
        }
        assertEquals(MediaRequestGuard.MAX_REQUESTS_PER_WINDOW, last)
        assertTrue(MediaRequestGuard.record("session-a", 1_000L) > MediaRequestGuard.MAX_REQUESTS_PER_WINDOW)
    }

    @Test
    fun `budgets are tracked per client`() {
        repeat(MediaRequestGuard.MAX_REQUESTS_PER_WINDOW) {
            MediaRequestGuard.record("session-a", 1_000L)
        }
        /* A different client starts with a fresh budget. */
        assertEquals(1, MediaRequestGuard.record("session-b", 1_000L))
    }

    @Test
    fun `the budget resets after the window elapsed`() {
        repeat(MediaRequestGuard.MAX_REQUESTS_PER_WINDOW + 5) {
            MediaRequestGuard.record("session-a", 1_000L)
        }
        assertEquals(1, MediaRequestGuard.record("session-a", 1_000L + MediaRequestGuard.WINDOW_MS))
    }

    @Test
    fun `authenticated callers are metered by session`() {
        assertEquals("session-token", MediaRequestGuard.clientOf("session-token", "10.0.0.1"))
    }

    @Test
    fun `anonymous callers are metered by address`() {
        assertEquals("ip:10.0.0.1", MediaRequestGuard.clientOf(null, "10.0.0.1"))
        assertEquals("ip:unknown", MediaRequestGuard.clientOf(null, null))
    }
}
