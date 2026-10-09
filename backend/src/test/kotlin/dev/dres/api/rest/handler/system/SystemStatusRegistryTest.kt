package dev.dres.api.rest.handler.system

import dev.dres.api.rest.types.system.SystemStatus
import dev.dres.api.rest.types.system.SystemStatusLevel
import dev.dres.api.rest.types.system.SystemStatusProvider
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class SystemStatusRegistryTest {
    @Test
    fun `publishes and clears transient statuses by code`() {
        val registry = SystemStatusRegistry
        registry.clear(SystemStatusRegistry.FFMPEG_VIDEO_RENDER_FAILED)
        val status = SystemStatus(
            SystemStatusRegistry.FFMPEG_VIDEO_RENDER_FAILED,
            SystemStatusLevel.WARNING,
            "A video preview failed"
        )

        registry.publish(status)
        assertEquals(status, registry.statuses().single { it.code == status.code })

        registry.clear(status.code)
        assertNull(registry.statuses().firstOrNull { it.code == status.code })
    }

    @Test
    fun `isolates failures from other status providers`() {
        val brokenProvider = SystemStatusProvider { throw IllegalStateException("test failure") }
        val healthyProvider = SystemStatusProvider {
            listOf(SystemStatus("HEALTHY_PROVIDER_TEST", SystemStatusLevel.INFO, "Healthy provider ran"))
        }
        val registry = SystemStatusRegistry
        registry.register(brokenProvider)
        registry.register(healthyProvider)

        try {
            val statuses = registry.statuses()
            assertEquals(1, statuses.count { it.code == "HEALTHY_PROVIDER_TEST" })
            assertFalse(statuses.any { it.code == "FFMPEG_UNAVAILABLE" })
        } finally {
            registry.unregister(brokenProvider)
            registry.unregister(healthyProvider)
        }
    }
}
