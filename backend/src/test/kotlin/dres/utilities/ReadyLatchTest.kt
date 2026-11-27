package dres.utilities

import dev.dres.utilities.ReadyLatch
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class ReadyLatchTest {

    @Test
    fun `clear removes timeout so newly registered viewers must be ready`() {
        val latch = ReadyLatch<String>()
        latch.register("viewer")
        latch.reset(0)
        assert(latch.allReadyOrTimedOut())

        latch.clear()
        latch.register("viewer")

        assertFalse(latch.allReadyOrTimedOut())
    }

    @Test
    fun `notReady names the viewers a timeout waited for`() {
        val latch = ReadyLatch<String>()
        latch.register("fast")
        latch.register("slow")
        latch.setReady("fast")

        /* Before the timeout the answer is visible through allReady()... */
        assertFalse(latch.allReady())
        assertEquals(setOf("slow"), latch.notReady())

        /* ...and after it allReadyOrTimedOut() is true, so notReady() is the only remaining trace. */
        latch.reset(0)
        latch.setReady("fast")
        assert(latch.allReadyOrTimedOut())
        assertEquals(setOf("slow"), latch.notReady())
    }

    @Test
    fun `notReady is empty once every viewer reported ready`() {
        val latch = ReadyLatch<String>()
        latch.register("a")
        latch.setReady("a")

        assertEquals(emptySet<String>(), latch.notReady())
    }
}
