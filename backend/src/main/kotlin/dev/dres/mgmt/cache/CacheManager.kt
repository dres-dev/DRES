package dev.dres.mgmt.cache

import com.github.kokorin.jaffree.ffmpeg.FFmpeg
import com.github.kokorin.jaffree.ffmpeg.UrlInput
import com.github.kokorin.jaffree.ffmpeg.UrlOutput
import dev.dres.DRES
import dev.dres.data.model.Config
import dev.dres.data.model.media.DbMediaItem
import dev.dres.data.model.media.DbMediaType
import dev.dres.data.model.media.MediaItemType
import dev.dres.utilities.CompletedFuture
import dev.dres.utilities.FailedFuture
import jetbrains.exodus.kotlin.synchronized
import org.slf4j.LoggerFactory
import org.slf4j.MarkerFactory
import java.awt.Image
import java.awt.image.BufferedImage
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.concurrent.*
import javax.imageio.ImageIO

/**
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class CacheManager(config: Config) {
    companion object {
        private val LOGGER = LoggerFactory.getLogger(PreviewImageFromVideoRequest::class.java)
        private val MARKER = MarkerFactory.getMarker("FFMPEG")
    }

    /** The path to the FFmpeg binary used by this [CacheManager] instance. */
    private val ffmpegBin = when {
        config.ffmpegBinary != null && Files.isDirectory(config.ffmpegBinary) -> config.ffmpegBinary /* Explicitly configured. */
        Files.isDirectory(DRES.APPLICATION_ROOT.parent.resolve("ffmpeg")) -> DRES.APPLICATION_ROOT.parent.resolve("ffmpeg") /* Distribution */
        Files.isDirectory(DRES.APPLICATION_ROOT.parent.parent.parent.resolve("ext/ffmpeg")) -> DRES.APPLICATION_ROOT.parent.parent.parent.resolve("ext/ffmpeg") /* Debug mode. */
        Files.isDirectory(Paths.get("ext/ffmpeg")) -> Paths.get("ext/ffmpeg")
        Files.isDirectory(Paths.get("ffmpeg")) -> Paths.get("ffmpeg")
        else -> throw IllegalStateException("Could not find valid FFmpeg binary path.")
    }

    /** A [ConcurrentHashMap] of aall [Path]s* that are currently being calculated. */
    private val inTransit = HashMap<Path,Future<Path>>()

    /** The [ExecutorService] used by this [CacheManager]. */
    private val executor: ExecutorService = Executors.newFixedThreadPool(2)

    init {
        println("Found FFmpeg at ${this.ffmpegBin}...")
    }

    /**
     * Generates a preview image from a [DbMediaItem].
     *
     * @param item The [DbMediaItem] to generate the preview image from. Must be of [DbMediaType.VIDEO] or [DbMediaType.IMAGE].
     * @param frame The frame to generate the preview image from. Defaults to 0.
     * @return [Future] of the [Path] to the generated image.
     */
    fun asyncPreviewImage(item: DbMediaItem, frame: Long = 0): Future<Path> {
        require(frame >= 0) { "Frame numbers cannot be negative." }
        /** The output path generated by this [PreviewImageFromVideoRequest]. */
        val output = DRES.CACHE_ROOT.resolve("previews").resolve("${item.mediaItemId}_${frame}.jpg")
        synchronized {
            if (Files.exists(output)) {
                return CompletedFuture(output)
            }
            val inTransit = this.inTransit[output]
            if (inTransit != null) {
                return inTransit
            }
            return when(item.type()) {
                MediaItemType.IMAGE -> this.executor.submit(PreviewImageFromImageRequest(item.pathToOriginal(), output))
                MediaItemType.VIDEO ->  this.executor.submit(PreviewImageFromVideoRequest(item.pathToOriginal(), output, frame))
                MediaItemType.TEXT -> return FailedFuture("Cannot generate a preview from a textual media item.")
            }
        }
    }

    /**
     * Generates a preview video from a [DbMediaItem].
     *
     * @param item The [DbMediaItem] to generate the preview video from. Must be of [DbMediaType.VIDEO].
     * @param start The start position in milliseconds to generate the preview video from.
     * @param start The end position in milliseconds to generate the preview video from. Defaults to 0.
     * @return [Future] of the [Path] to the generated video.
     */
    fun asyncPreviewVideo(item: DbMediaItem, start: Long, end: Long): Future<Path> {
        require(start >= 0) { "Start timestamp cannot be negative." }
        require(end > start) { "End timestamp must be greater than start." }
        /** The output path generated by this [PreviewImageFromVideoRequest]. */
        val output = DRES.CACHE_ROOT.resolve("previews").resolve("${item.mediaItemId}_${start}-${end}.mp4")
        synchronized {
            if (Files.exists(output)) {
                return CompletedFuture(output)
            }
            val inTransit = this.inTransit[output]
            if (inTransit != null) {
                return inTransit
            }
            return when(item.type()) {
                MediaItemType.VIDEO ->  this.executor.submit(PreviewVideoFromVideoRequest(item.pathToOriginal(), output, start, end))
                MediaItemType.IMAGE ->  return FailedFuture("Cannot generate a video preview from a image media item.")
                MediaItemType.TEXT -> return FailedFuture("Cannot generate a video preview from a textual media item.")
            }
        }
    }

    /**
     * Stops this [CacheManager] and all requests that are in transit.
     */
    fun stop() {
        this.executor.shutdown()
        this.executor.awaitTermination(5000, TimeUnit.MILLISECONDS)
    }

    /**
     * Utility method to convert from milliseconds to timecode representation.
     *
     * @param ms The timestamp in milliseconds.
     * @return The timecode [String].
     */
    private fun millisecondToTimestamp(ms: Long): String {
        val hours = ms / (1000 * 3600)
        val minutes = (ms % (1000 * 3600)) / (60_000)
        val seconds = (ms % 60_000) / 1000
        val milliseconds = ms % 1000
        return "$hours:$minutes:$seconds.${"%03d".format(milliseconds)}"
    }

    /**
     * A [Callable] that generates a preview image from an image file.
     */
    inner class PreviewImageFromImageRequest constructor(private val input: Path, private  val output: Path, val size: Int = 500): Callable<Path> {
        override fun call(): Path = try {/* Try to read image. */
            Files.newInputStream(this.input, StandardOpenOption.READ).use {i ->
                val image = ImageIO.read(i)

                /* Scale image to a maximum of 500x500 pixels. */
                val scaled: Image = if (image.width > image.height) {
                    image.getScaledInstance(size, -1, Image.SCALE_DEFAULT)
                } else {
                    image.getScaledInstance(-1, size, Image.SCALE_DEFAULT)
                }
                val outputImage = BufferedImage(scaled.getWidth(null), scaled.getHeight(null), BufferedImage.TYPE_INT_RGB)
                outputImage.graphics.drawImage(scaled, 0, 0, null)

                Files.newOutputStream(this.output, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING).use { o ->
                    ImageIO.write(outputImage, "jpg", o)
                }
            }
            this.output
        } catch (e: Exception) {
            LOGGER.error("Error in FFMpeg: ${e.message}")
            throw e
        } finally {
            this@CacheManager.inTransit.remove(this.output) /* Remove this PreviewImageFromVideoRequest. */
        }
    }

    /**
     * A [Callable] that generates a preview image from a video file.
     */
    inner class PreviewImageFromVideoRequest constructor(private val input: Path, private  val output: Path, private val start: Long): Callable<Path> {
        override fun call(): Path = try {
            FFmpeg.atPath(this@CacheManager.ffmpegBin).
            addInput(UrlInput.fromPath(this.input))
                .addOutput(UrlOutput.toPath(this.output))
                .setOverwriteOutput(true)
                .addArguments("-ss", millisecondToTimestamp(this.start))
                .addArguments("-vframes", "1")
                .addArguments("-filter:v", "scale=120:-1")
                .setOutputListener { l -> LOGGER.debug(MARKER, l); }
                .execute()
            this.output
        } catch (e: Exception) {
            LOGGER.error("Error in FFMpeg: ${e.message}")
            throw e
        } finally {
            this@CacheManager.inTransit.remove(this.output) /* Remove this PreviewImageFromVideoRequest. */
        }
    }

    /**
     * A [Callable] that generates a preview video from a video file.
     */
    inner class PreviewVideoFromVideoRequest constructor(private val input: Path, private  val output: Path, private val start: Long, private val end: Long): Callable<Path> {
        override fun call(): Path = try {
            val startTimecode = millisecondToTimestamp(this.start)
            val endTimecode = millisecondToTimestamp(this.start)
            LOGGER.info(MARKER, "Start rendering segment for video $input from $startTimecode to $endTimecode")
            FFmpeg.atPath(this@CacheManager.ffmpegBin)
                .addInput(UrlInput.fromPath(this.input))
                .addOutput(UrlOutput.toPath(this.output))
                .setOverwriteOutput(true)
                .addArguments("-ss", startTimecode)
                .addArguments("-to", endTimecode)
                .addArguments("-c:v", "libx264")
                .addArguments("-c:a", "aac")
                .addArguments("-b:v", "2000k")
                .addArguments("-tune", "zerolatency")
                .addArguments("-preset", "slow")
                .setOutputListener { l -> LOGGER.debug(MARKER, l); }
                .execute()
            this.output
        } catch (e: Exception) {
            LOGGER.error("Error in FFMpeg: ${e.message}")
            throw e
        } finally {
            this@CacheManager.inTransit.remove(this.output) /* Remove this PreviewImageFromVideoRequest. */
        }
    }
}