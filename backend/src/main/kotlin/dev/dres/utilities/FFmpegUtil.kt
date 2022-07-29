package dev.dres.utilities

import com.github.kokorin.jaffree.StreamType
import com.github.kokorin.jaffree.ffmpeg.FFmpeg
import com.github.kokorin.jaffree.ffmpeg.FFmpegResult
import com.github.kokorin.jaffree.ffmpeg.UrlInput
import com.github.kokorin.jaffree.ffmpeg.UrlOutput
import com.github.kokorin.jaffree.ffprobe.FFprobe
import com.github.kokorin.jaffree.ffprobe.FFprobeResult
import dev.dres.DRES
import dev.dres.data.model.competition.CachedVideoItem
import org.slf4j.LoggerFactory
import org.slf4j.MarkerFactory
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.*

object FFmpegUtil {

    /** Path to FFMPEG binary; TODO: Make configurable. */
    val ffmpegBin = when {
        Files.isDirectory(DRES.rootPath.parent.parent.parent.resolve("ext/ffmpeg")) -> DRES.rootPath.parent.parent.parent.resolve(
            "ext/ffmpeg"
        )
        Files.isDirectory(DRES.rootPath.parent.resolve("ffmpeg")) -> DRES.rootPath.parent.resolve("ffmpeg") /* Distribution */
        Files.isDirectory(Paths.get("ext/ffmpeg")) -> Paths.get("ext/ffmpeg")
        Files.isDirectory(Paths.get("ffmpeg")) -> Paths.get("ffmpeg")
        else -> throw IllegalStateException("Could not find valid FFmpeg binary path.")
    }

    private val logger = LoggerFactory.getLogger(this.javaClass)
    private val logMarker = MarkerFactory.getMarker("FFMPEG")

    private data class FrameRequest(val video: Path, val timecode: String, val outputImage: Path)

    private val frameRequestStack = ConcurrentLinkedDeque<FrameRequest>()

    private fun toMillisecondTimeStamp(ms: Long): String {
        val hours = ms / (1000 * 3600)
        val minutes = (ms % (1000 * 3600)) / (60_000)
        val seconds = (ms % 60_000) / 1000
        val milliseconds = ms % 1000

        return "$hours:$minutes:$seconds.${"%03d".format(milliseconds)}"
    }

    private val numberOfProcessors = Runtime.getRuntime().availableProcessors()

    private val ffmpegThreadPool = ThreadPoolExecutor(
        1.coerceAtLeast(numberOfProcessors / 2),
        1.coerceAtLeast(numberOfProcessors / 2),
        1,
        TimeUnit.MINUTES,
        LinkedBlockingDeque(),
        NamedThreadFactory("FFmpegThreadPool")
    )


    private val pendingFutures = ConcurrentHashMap<Path, Future<Path?>>()

    fun executeFFmpegAsync(
        video: Path,
        timestamp: Long,
        outputImage: Path
    ): Future<Path?> {

        //currently in progress, return existing
        val pending = pendingFutures[outputImage]
        if (pending != null) {
            if (pending.isDone || pending.isCancelled) {
                pendingFutures.remove(outputImage)
            }
            return pending
        }

        //already done, should not be requested, return dummy
        if (Files.exists(outputImage)) {
            val dummy = CompletableFuture<Path?>()
            dummy.complete(outputImage)
            return dummy
        }

        //schedule generation
        return ffmpegThreadPool.submit(Callable {

            try {
                if (Files.exists(outputImage)) {
                    return@Callable outputImage
                }

                FFmpeg.atPath(ffmpegBin)
                    .addInput(UrlInput.fromPath(video))
                    .addOutput(UrlOutput.toPath(outputImage))
                    .setOverwriteOutput(true)
                    .addArguments("-ss", toMillisecondTimeStamp(timestamp))
                    .addArguments("-vframes", "1")
                    .addArguments("-filter:v", "scale=120:-1")
                    .setOutputListener { logger.debug(logMarker, it); true }
                    .execute()

                if (Files.exists(outputImage)) {
                    outputImage
                } else {
                    logger.info("Failed generating preview at ${outputImage.toFile().absolutePath}")
                    null
                }
            } catch (t: Throwable) {
                logger.info(t.localizedMessage)
                return@Callable null
            }
        }

        )

    }

    private fun extractFrameAsync(video: Path, timecode: String, outputImage: Path) =
        FFmpeg.atPath(ffmpegBin)
            .addInput(UrlInput.fromPath(video))
            .addOutput(UrlOutput.toPath(outputImage))
            .setOverwriteOutput(true)
            .addArguments("-ss", timecode)
            .addArguments("-vframes", "1")
            .addArguments("-filter:v", "scale=120:-1")
            .setOutputListener { logger.debug(logMarker, it); true }
            .executeAsync()


    fun extractFrame(video: Path, timecode: String, outputImage: Path) {
        val request = FrameRequest(video, timecode, outputImage)
        if (!Files.exists(outputImage) && !frameRequestStack.contains(request)) {
            frameRequestStack.push(request)
            logger.info(logMarker, "Enqueued frame request $request")
        }

    }

    fun extractFrame(video: Path, ms: Long, outputImage: Path) =
        extractFrame(video, toMillisecondTimeStamp(ms), outputImage)

    private fun extractFrameAsync(video: Path, ms: Long, outputImage: Path) {
        extractFrameAsync(video, toMillisecondTimeStamp(ms), outputImage)
    }

    fun extractSegment(video: Path, startTimecode: String, endTimecode: String, outputVideo: Path) {
        try {
            //semaphore.acquire()
            logger.info(
                logMarker,
                "Start rendering segment for video $video from $startTimecode to $endTimecode"
            )
            FFmpeg.atPath(ffmpegBin)
                .addInput(UrlInput.fromPath(video))
                .addOutput(UrlOutput.toPath(outputVideo))
                .setOverwriteOutput(true)
                .addArguments("-ss", startTimecode)
                .addArguments("-to", endTimecode)
                .addArguments("-c:v", "libx264")
                .addArguments("-c:a", "aac")
                .addArguments("-b:v", "2000k")
                .addArguments("-tune", "zerolatency")
                .addArguments("-preset", "slow")
                .setOutputListener { logger.debug(logMarker, it); true }
                .execute()
        } finally {
            //semaphore.release()
        }
    }

    fun prepareMediaSegmentTask(
        description: CachedVideoItem,
        collectionBasePath: String,
        cacheLocation: File
    ) {

        cacheLocation.mkdirs()

        val input = File(File(collectionBasePath), description.item.location).toPath()
        val output = File(cacheLocation, description.cacheItemName()).toPath()
        val range = description.temporalRange.toMilliseconds()

        extractSegment(
            input,
            toMillisecondTimeStamp(range.first),
            toMillisecondTimeStamp(range.second),
            output
        )

    }

    fun analyze(videoPath: Path, countFrames: Boolean = false): FFprobeResult =
        FFprobe.atPath(ffmpegBin)
            .setInput(videoPath)
            .setShowStreams(true)
            .setCountFrames(countFrames)
            .setSelectStreams(StreamType.VIDEO)
            .execute()

    fun stop() {
        ffmpegThreadPool.shutdown()
    }

}
