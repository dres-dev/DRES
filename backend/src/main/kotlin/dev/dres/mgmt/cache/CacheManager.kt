package dev.dres.mgmt.cache

import com.github.kokorin.jaffree.ffmpeg.FFmpeg
import com.github.kokorin.jaffree.ffmpeg.UrlInput
import com.github.kokorin.jaffree.ffmpeg.UrlOutput
import dev.dres.DRES
import dev.dres.data.model.config.Config
import dev.dres.data.model.media.DbMediaItem
import dev.dres.data.model.media.DbMediaType
import dev.dres.data.model.media.MediaItemType
import dev.dres.data.model.run.DbEvaluation
import dev.dres.data.model.run.DbEvaluationStatus
import dev.dres.utilities.CompletedFuture
import dev.dres.utilities.FailedFuture
import jetbrains.exodus.database.TransientEntityStore
import jetbrains.exodus.kotlin.synchronized
import kotlinx.dnq.query.asSequence
import kotlinx.dnq.query.filter
import kotlinx.dnq.query.flatMapDistinct
import org.slf4j.LoggerFactory
import org.slf4j.MarkerFactory
import java.awt.Image
import java.awt.image.BufferedImage
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.concurrent.*
import javax.imageio.ImageIO
import kotlin.io.path.*
import kotlin.system.exitProcess

/**
 * A [CacheManager] used to manager and generate, access and manage cached image and video previews.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class CacheManager(private val config: Config, private val store: TransientEntityStore) {
    companion object {
        private val LOGGER = LoggerFactory.getLogger(PreviewImageFromVideoRequest::class.java)
        private val MARKER = MarkerFactory.getMarker("FFMPEG")
    }

    /** The path to the FFmpeg binary used by this [CacheManager] instance. */
    private val ffmpegBin = this.config.cache.ffmpegPath()

    /** The [Path] to the cache location. */
    private val cacheLocation = DRES.CONFIG.cache.cachePath

    /** A [ConcurrentHashMap] of all [Path]s* that are currently being calculated. */
    private val inTransit = ConcurrentHashMap<Path,Future<Path>>()

    /** The [ExecutorService] used by this [CacheManager]. */
    private val executor: ExecutorService = Executors.newFixedThreadPool(this.config.cache.maxRenderingThreads)

    init {
        println("Found FFmpeg at ${this.ffmpegBin}...")
        /* Validating that FFmpeg path exists and is a directory */
        if(!this.ffmpegBin.exists() || !this.ffmpegBin.isDirectory()){
            System.err.println("ERROR: FFmpeg path ${this.ffmpegBin} does not exist! Shutting down!")
            exitProcess(-100)
        }

        /* Validating that FFmpeg and FFprobe exist and are executable */
        this.ffmpegBin.listDirectoryEntries("ff*").forEach {
            /* Slightly convoluted in order to not hassle with OS dependent things like .exe extension on Windows */
            val isFFmpeg = it.nameWithoutExtension.startsWith("ffmpeg") && it.nameWithoutExtension.endsWith("ffmpeg")
            val isFFprobe = it.nameWithoutExtension.startsWith("ffprobe") && it.nameWithoutExtension.endsWith("ffprobe")
            if(isFFprobe || isFFmpeg){
                if(!it.exists() || !it.isExecutable()){
                    System.err.println("ERROR: $it in ${this.ffmpegBin} does not exist or is not executable! Shutting down!")
                    exitProcess(-101)
                }
            }
        }
        if (!Files.exists(cacheLocation)) {
            println("Created cache location at ${this.cacheLocation}...")
            Files.createDirectories(cacheLocation)
        }
    }

    /**
     * Generates a preview image from a Path to an external image.
     *
     * @param imagePath the source path.
     * @return [Future] of the [Path] to the generated image.
     */
    fun asyncPreviewImage(imagePath: Path): Future<Path> {
        val output = this@CacheManager.cacheLocation.resolve("${imagePath.fileName}.jpg")
        if (Files.exists(output)) {
            this.inTransit.remove(output)
            return CompletedFuture(output)
        }
        val inTransit = this.inTransit[output]
        if (inTransit != null) {
            return inTransit
        }
        synchronized {
            return try {
                val ret = this.executor.submit(PreviewImageFromImageRequest(imagePath, output))
                this@CacheManager.inTransit[output] = ret
                ret
            } catch (e: Throwable) {
                LOGGER.error(e.message)
                FailedFuture("Could generate preview image: ${e.message}")
            }
        }
    }

    /**
     * Generates a preview image from a [DbMediaItem].
     *
     * @param item The [DbMediaItem] to generate the preview image from. Must be of [DbMediaType.VIDEO] or [DbMediaType.IMAGE].
     * @param timestamp The timestamp in milliseconds to generate the preview image from. Defaults to 0.
     * @return [Future] of the [Path] to the generated image.
     */
    fun asyncPreviewImage(item: DbMediaItem, timestamp: Long = 0): Future<Path> {
        require(timestamp >= 0) { "Frame numbers cannot be negative." }
        /** The output path generated by this [PreviewImageFromVideoRequest]. */
        val output = this@CacheManager.cacheLocation.resolve("${item.mediaItemId}_${timestamp}-${timestamp}.jpg")
        if (Files.exists(output)) {
            this.inTransit.remove(output)
            return CompletedFuture(output)
        }
        val inTransit = this.inTransit[output]
        if (inTransit != null) {
            return inTransit
        }
        synchronized {
            return try {
                when(item.type()) {
                    MediaItemType.IMAGE -> {
                        val ret = this.executor.submit(PreviewImageFromImageRequest(item.pathToOriginal(), output))
                        this@CacheManager.inTransit[output] = ret
                        ret
                    }
                    MediaItemType.VIDEO -> {
                        if (timestamp <= item.durationMs!!) { //bounds check
                            val ret = this.executor.submit(
                                PreviewImageFromVideoRequest(
                                    item.pathToOriginal(),
                                    output,
                                    timestamp
                                )
                            )
                            this@CacheManager.inTransit[output] = ret
                            ret
                        }else {
                            FailedFuture("Requested time is outside of video.")
                        }
                    }
                    else -> FailedFuture("Cannot generate a preview image from a media item that is neither a video nor an image.")
                }
            } catch (e: Throwable) {
                LOGGER.error(e.message)
                FailedFuture("Could generate preview image: ${e.message}")
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
        val output = this@CacheManager.cacheLocation.resolve("${item.mediaItemId}_${start}-${end}.mp4")
        if (Files.exists(output)) {
            this.inTransit.remove(output)
            return CompletedFuture(output)
        }
        val inTransit = this.inTransit[output]
        if (inTransit != null) {
            return inTransit
        }
        synchronized {
            return try {
                when(item.type()) {
                    MediaItemType.VIDEO -> {
                        if (start < item.durationMs!! && end <= item.durationMs!!) {
                            val ret = this.executor.submit(
                                PreviewVideoFromVideoRequest(
                                    item.pathToOriginal(),
                                    output,
                                    start,
                                    end
                                )
                            )
                            this@CacheManager.inTransit[output] = ret
                            ret
                        } else {
                            FailedFuture("Requested time is outside of video.")
                        }
                    }
                    else -> FailedFuture("Cannot generate a video preview from a media item that is not a video.")
                }
            } catch (e: Throwable) {
                LOGGER.error(e.message)
                FailedFuture("Could generate preview video: ${e.message}")
            }
        }
    }
    /**
     * Generates a preview video from a Path.
     *
     * @param item The [DbMediaItem] to generate the preview video from. Must be of [DbMediaType.VIDEO].
     * @param start The start position in milliseconds to generate the preview video from.
     * @param start The end position in milliseconds to generate the preview video from. Defaults to 0.
     * @return [Future] of the [Path] to the generated video.
     */
    fun asyncPreviewVideo(input: Path, start: Long, end: Long): Future<Path> {
        require(start >= 0) { "Start timestamp cannot be negative." }
        require(end > start) { "End timestamp must be greater than start." }
        /** The output path generated by this [PreviewImageFromVideoRequest]. */
        val output = this@CacheManager.cacheLocation.resolve("${input.fileName}_${start}-${end}.mp4")
        if (Files.exists(output)) {
            this.inTransit.remove(output)
            return CompletedFuture(output)
        }
        val inTransit = this.inTransit[output]
        if (inTransit != null) {
            return inTransit
        }
        synchronized {
            return try {
                val ret = this.executor.submit(
                    PreviewVideoFromVideoRequest(
                        input, output, start, end
                    )
                )
                this@CacheManager.inTransit[output] = ret
                ret
            } catch (e: Throwable) {
                LOGGER.error(e.message)
                FailedFuture("Could generate preview video: ${e.message}")
            }
        }
    }

    /**
     * Cleans the cache directory associated with this [CacheManager]. The cleanup mechanism makes sure, that all
     * as part of an ongoing evaluation are preserved and not deleted.
     *
     * Requires an active transaction.
     */
    fun cleanup() = try {
        /* Fetch all items related to a hint preview. */
        val blackList = mutableSetOf<Path>()
        this.store.transactional(true) {
            DbEvaluation.filter { it.status eq DbEvaluationStatus.ACTIVE }
                .flatMapDistinct { it.template.tasks }
                .flatMapDistinct { it.hints }
                .filter { it.item ne null }
                .asSequence().forEach {
                    blackList.add(this@CacheManager.cacheLocation.resolve("${it.item!!.mediaItemId}_${it.start ?: 0}-${it.end ?: 0}"))
                }

            /* Fetch all items related to a target preview. */
            DbEvaluation.filter { it.status eq DbEvaluationStatus.ACTIVE }
                .flatMapDistinct { it.template.tasks }
                .flatMapDistinct { it.targets }
                .filter { it.item ne null }
                .asSequence().forEach {
                    blackList.add(this@CacheManager.cacheLocation.resolve("${it.item!!.mediaItemId}_${it.range?.start ?: 0}-${it.range?.end ?: 0}"))
                }
        }

        /* Delete all files that have not been blacklisted. */
        Files.walk(this.cacheLocation).use { walk ->
            walk.sorted(Comparator.reverseOrder()).forEach {
                val time = Files.getLastModifiedTime(it)
                if ((System.currentTimeMillis() - time.toMillis()) > 300000L && !blackList.contains(it)) {
                    Files.delete(it)
                }
            }
        }
    } catch(e: Throwable) {
        LOGGER.warn("Failed to cleanup cache directory ${this.cacheLocation}: ${e.message}")
    }

    /**
     * Clears the cache directory associated with this [CacheManager].
     */
    fun clear() = try {
        Files.walk(this.cacheLocation).use { walk ->
            walk.sorted(Comparator.reverseOrder()).forEach {
                Files.delete(it)
            }
        }
    } catch(e: Throwable) {
        LOGGER.warn("Failed to clear cache directory ${this.cacheLocation}: ${e.message}")
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
        return "$hours:${"%02d".format(minutes)}:${"%02d".format(seconds)}.${"%03d".format(milliseconds)}"
    }

    /**
     * A [Callable] that generates a preview image from an image file.
     */
    inner class PreviewImageFromImageRequest constructor(input: Path, output: Path, private val size: Int = this@CacheManager.config.cache.previewImageMaxSize): AbstractPreviewRequest(input, output) {

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
    inner class PreviewImageFromVideoRequest constructor(input: Path, output: Path, private val start: Long, private val size: Int = this@CacheManager.config.cache.previewImageMaxSize): AbstractPreviewRequest(input, output) {
        override fun call(): Path = try {
            FFmpeg.atPath(this@CacheManager.ffmpegBin).
            addInput(UrlInput.fromPath(this.input))
                .addOutput(UrlOutput.toPath(this.output))
                .setOverwriteOutput(true)
                .addArguments("-ss", millisecondToTimestamp(this.start))
                .addArguments("-frames:v", "1")
                .addArguments("-filter:v", "scale=${this@CacheManager.config.cache.previewImageMaxSize}:-1")
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
    inner class PreviewVideoFromVideoRequest constructor(input: Path, output: Path, private val start: Long, private val end: Long): AbstractPreviewRequest(input, output){
        override fun call(): Path = try {
            val startTimecode = millisecondToTimestamp(this.start)
            val endTimecode = millisecondToTimestamp(this.end)
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
                .addArguments("-filter:v", "scale=${this@CacheManager.config.cache.previewVideoMaxSize}:-1")
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
