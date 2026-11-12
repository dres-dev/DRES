package dres.utilities

import dev.dres.utilities.extensions.clientIP
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ClientIpTest {
    @Test
    fun `uses first address in X-Forwarded-For`() {
        assertEquals("198.51.100.24", clientIP("198.51.100.24, 10.0.0.3", "203.0.113.5", "127.0.0.1"))
    }

    @Test
    fun `falls back to X-Real-IP when forwarded for is blank`() {
        assertEquals("203.0.113.5", clientIP("  ,  ", " 203.0.113.5 ", "127.0.0.1"))
    }

    @Test
    fun `falls back to socket address when proxy headers are absent`() {
        assertEquals("127.0.0.1", clientIP(null, null, "127.0.0.1"))
    }
}
