package dev.dres.api.rest.handler.system

import dev.dres.DRES
import dev.dres.api.rest.types.system.SystemStatus
import dev.dres.api.rest.types.system.SystemStatusLevel
import dev.dres.api.rest.types.system.SystemStatusProvider
import dev.dres.utilities.FFmpegDownloader

/** Reports missing FFmpeg tooling as an administrator-visible system warning. */
class FfmpegSystemStatusProvider(
    private val isAvailable: () -> Boolean = {
        runCatching { DRES.CONFIG.cache.ffmpegPath() }.getOrDefault(null) != null
    },
    private val supportedDownload: () -> Boolean = { FFmpegDownloader.platform() != null },
    private val isInitialized: () -> Boolean = {
        runCatching { DRES.DATA_ROOT }.isSuccess && runCatching { DRES.CONFIG }.isSuccess
    }
) : SystemStatusProvider {
    override fun statuses(): List<SystemStatus> {
        if (!isInitialized() || isAvailable()) return emptyList()

        val setup = if (supportedDownload()) {
            "Run 'setup ffmpeg' in the DRES console or install ffmpeg and ffprobe on PATH."
        } else {
            "Install ffmpeg and ffprobe system-wide or configure cache.ffmpegPath."
        }
        return listOf(
            SystemStatus(
                code = "FFMPEG_UNAVAILABLE",
                level = SystemStatusLevel.WARNING,
                message = "FFmpeg is unavailable; video preview generation and video collection analysis are disabled. $setup"
            )
        )
    }
}
