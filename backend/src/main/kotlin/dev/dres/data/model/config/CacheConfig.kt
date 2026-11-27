package dev.dres.data.model.config

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import dev.dres.DRES
import dev.dres.utilities.FFmpegLocator
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Configuration related to the handling of the preview cache by DRES.
 *
 * @author Ralph Gasser
 * @author Loris Sauter
 * @version 1.1.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class CacheConfig(
    /** Interval in ms at which cache cleanup is triggered. Defaults to 1 minute. */
    val cleanupIntervalMs: Long = 6000L,

    /** Threshold after which unused files are evicted from cache. */
    val evictionThresholdMs: Long = 30000L,

    /** The maximum number of threads used for generating preview images and videos. */
    val maxRenderingThreads: Int = 2,

    /** Custom directory (or binary path) for FFmpeg. If not set, default locations and PATH are used. */
    val ffmpegPath: Path? = null,

    /** The maximum bounding box for a logo. Defaults to 500px. */
    val logoMaxSize: Int = 500,

    /** The maximum bounding box for a preview image. Defaults to 500px. */
    val previewImageMaxSize: Int = 500,

    /** The maximum bounding box for a preview video. Defaults to 480p. */
    val previewVideoMaxSize: Int = 480,

    /** The location of the cache, relative to APPLICATION_ROOT */
    val cachePath: Path = DRES.APPLICATION_ROOT.parent.resolve("cache"),

) {

    /**
     * Returns the directory containing executable ffmpeg and ffprobe binaries, or null when unavailable.
     * Searches the configured location, application-relative locations, the DRES data directory, and PATH.
     */
    fun ffmpegPath(): Path? = FFmpegLocator.find(
        configured = this.ffmpegPath,
        candidates = listOf(
            DRES.APPLICATION_ROOT.parent.resolve("ffmpeg"),
            DRES.APPLICATION_ROOT.parent.parent.parent.resolve("ext/ffmpeg"),
            Paths.get("ext/ffmpeg"),
            Paths.get("ffmpeg"),
            DRES.DATA_ROOT.resolve("ffmpeg")
        )
    )?.directory

}
