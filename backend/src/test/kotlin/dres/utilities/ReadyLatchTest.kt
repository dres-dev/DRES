package dres.utilities

import dev.dres.utilities.ReadyLatch
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
}
