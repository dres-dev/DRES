package dev.dres.data.model.config

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import dev.dres.DRES
import java.nio.file.Files
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

    /** Custom path to FFmpeg. If not set, default paths will be tried instead. */
    val ffmpegPath: Path? = null,

    /** The maximum bounding box for a logo. Defaults to 500px. */
    val logoMaxSize: Int = 500,

    /** The maximum bounding box for a preview image. Defaults to 500px. */
    val previewImageMaxSize: Int = 500,

    /** The maximum bounding box for a preview video. Defaults to 480p. */
    val previewVideoMaxSize: Int = 480,

    /** The location of the cache, relative to APPLICATION_ROOT */
    val cachePath: Path = DRES.DATA_ROOT.resolve("cache"),

) {

    /**
     * Returns the path to FFmpeg.
     */
    fun ffmpegPath() = when {
        this.ffmpegPath != null && Files.isDirectory(ffmpegPath) -> ffmpegPath /* Explicitly configured. */
        Files.isDirectory(DRES.APPLICATION_ROOT.parent.resolve("ffmpeg")) -> DRES.APPLICATION_ROOT.parent.resolve("ffmpeg") /* Distribution */
        Files.isDirectory(DRES.APPLICATION_ROOT.parent.parent.parent.resolve("ext/ffmpeg")) -> DRES.APPLICATION_ROOT.parent.parent.parent.resolve("ext/ffmpeg") /* Debug mode. */
        Files.isDirectory(Paths.get("ext/ffmpeg")) -> Paths.get("ext/ffmpeg")
        Files.isDirectory(Paths.get("ffmpeg")) -> Paths.get("ffmpeg")
        else -> throw IllegalStateException("Could not find valid FFmpeg binary path.")
    }

}
