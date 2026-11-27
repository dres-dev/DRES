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

    /**
     * The window slides: a burst spread over the end of one window and the start of the next is still one
     * burst. A fixed window that resets on its start boundary allowed exactly twice the intended rate for a
     * client that times requests around the boundary.
     */
    @Test
    fun `the window slides instead of resetting at a boundary`() {
        val start = 1_000_000L
        /* Half the budget 30 s in, half 30 s later: at most 60 s apart, so never a full idle window. */
        repeat(MediaRequestGuard.MAX_REQUESTS_PER_WINDOW / 2) {
            MediaRequestGuard.record("boundary", start)
        }
        repeat(MediaRequestGuard.MAX_REQUESTS_PER_WINDOW / 2) {
            MediaRequestGuard.record("boundary", start + 30_000L)
        }
        /* A full minute after the first batch but only 30 s after the second: a fixed window keyed on the
         * first request has reset, a sliding window still holds the second batch. */
        val later = start + MediaRequestGuard.WINDOW_MS + 1
        val count = MediaRequestGuard.record("boundary", later)

        assertTrue(
            count > MediaRequestGuard.MAX_REQUESTS_PER_WINDOW / 2,
            "The second batch must still count against the budget, but only $count requests were seen."
        )
    }

    @Test
    fun `an idle client gets its budget back`() {
        repeat(MediaRequestGuard.MAX_REQUESTS_PER_WINDOW) {
            MediaRequestGuard.record("idle", 0L)
        }
        /* Everything has aged out. */
        assertEquals(1, MediaRequestGuard.record("idle", MediaRequestGuard.WINDOW_MS * 2))
    }

    /** Rotating identifiers must not grow the bookkeeping without limit. */
    @Test
    fun `bookkeeping stays bounded when clients are unique`() {
        repeat(12_000) { i ->
            MediaRequestGuard.record("rotating-$i", 5_000L)
        }
        assertTrue(
            MediaRequestGuard.trackedClients() <= 10_000,
            "Tracked clients were ${MediaRequestGuard.trackedClients()}, which is unbounded growth."
        )
    }
}
