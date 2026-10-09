package dev.dres.api.rest.handler.system

import dev.dres.api.rest.types.system.SystemStatusLevel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FfmpegSystemStatusProviderTest {
    @Test
    fun `reports warning with setup advice when FFmpeg is absent`() {
        val provider = FfmpegSystemStatusProvider(
            isAvailable = { false },
            supportedDownload = { true },
            isInitialized = { true }
        )

        val status = provider.statuses().single()
        assertEquals("FFMPEG_UNAVAILABLE", status.code)
        assertEquals(SystemStatusLevel.WARNING, status.level)
        assertTrue(status.message.contains("setup ffmpeg"))
    }

    @Test
    fun `reports system install advice when downloader is unsupported`() {
        val provider = FfmpegSystemStatusProvider(
            isAvailable = { false },
            supportedDownload = { false },
            isInitialized = { true }
        )

        val status = provider.statuses().single()
        assertTrue(status.message.contains("configure cache.ffmpegPath"))
    }

    @Test
    fun `does not report a warning when FFmpeg is available or DRES is not initialized`() {
        val available = FfmpegSystemStatusProvider(
            isAvailable = { true },
            supportedDownload = { true },
            isInitialized = { true }
        )
        val notInitialized = FfmpegSystemStatusProvider(
            isAvailable = { false },
            supportedDownload = { true },
            isInitialized = { false }
        )

        assertTrue(available.statuses().isEmpty())
        assertTrue(notInitialized.statuses().isEmpty())
    }
}
