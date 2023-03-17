package dev.dres.utilities

import com.github.kokorin.jaffree.StreamType
import com.github.kokorin.jaffree.ffmpeg.FFmpeg
import com.github.kokorin.jaffree.ffmpeg.UrlInput
import com.github.kokorin.jaffree.ffmpeg.UrlOutput
import com.github.kokorin.jaffree.ffprobe.FFprobe
import com.github.kokorin.jaffree.ffprobe.FFprobeResult
import dev.dres.DRES
import dev.dres.data.model.media.DbMediaItem
import dev.dres.data.model.media.time.TemporalRange
import org.slf4j.LoggerFactory
import org.slf4j.MarkerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.ConcurrentLinkedDeque

object FFmpegUtil {

    /** Path to FFMPEG binary; TODO: Make configurable. */
    val ffmpegBin = when {
        Files.isDirectory(DRES.APPLICATION_ROOT.parent.parent.parent.resolve("ext/ffmpeg")) -> DRES.APPLICATION_ROOT.parent.parent.parent.resolve(
            "ext/ffmpeg"
        )

        Files.isDirectory(DRES.APPLICATION_ROOT.parent.resolve("ffmpeg")) -> DRES.APPLICATION_ROOT.parent.resolve("ffmpeg") /* Distribution */
        Files.isDirectory(Paths.get("ext/ffmpeg")) -> Paths.get("ext/ffmpeg")
        Files.isDirectory(Paths.get("ffmpeg")) -> Paths.get("ffmpeg")
        else -> throw IllegalStateException("Could not find valid FFmpeg binary path.")
    }

    private val logger = LoggerFactory.getLogger(this.javaClass)
    private val logMarker = MarkerFactory.getMarker("FFMPEG")

    private data class FrameRequest(val video: Path, val timestamp: Long, val outputImage: Path)

    private val frameRequestStack = ConcurrentLinkedDeque<FrameRequest>()

    private fun toMillisecondTimeStamp(ms: Long): String {
        val hours = ms / (1000 * 3600)
        val minutes = (ms % (1000 * 3600)) / (60_000)
        val seconds = (ms % 60_000) / 1000
        val milliseconds = ms % 1000

        return "$hours:$minutes:$seconds.${"%03d".format(milliseconds)}"
    }

    private var threadsActive = true

    private val threads: List<Thread>

    init {

        val numberOfWorkerThreads = 1.coerceAtLeast(Runtime.getRuntime().availableProcessors() / 8)

        threads = (1..numberOfWorkerThreads).map {
            val t = Thread {
                while (threadsActive) {

                    val request = frameRequestStack.pollLast()

                    if (request != null) {

                        if (Files.exists(request.outputImage)) {
                            continue
                        }

                        try {
                            FFmpeg.atPath(ffmpegBin)
                                .addInput(UrlInput.fromPath(request.video))
                                .addOutput(UrlOutput.toPath(request.outputImage))
                                .setOverwriteOutput(true)
                                .addArguments("-ss", toMillisecondTimeStamp(request.timestamp))
                                .addArguments("-vframes", "1")
                                .addArguments("-filter:v", "scale=120:-1")
                                .setOutputListener { l -> logger.debug(logMarker, l); true }
                                .execute()
                        } catch (e: Exception) {
                            logger.error("Error in FFMpeg: ${e.message}")
                        }

                    } else {
                        try {
                            Thread.sleep(1000)
                        } catch (e: InterruptedException) {
                            //ignore
                        }
                    }
                }
            }
            t.name = "FFMpegWorkerThread-$it"
            t.start()
            t
        }

    }

    fun extractFrame(video: Path, ms: Long, outputImage: Path) {
        val request = FrameRequest(video, ms, outputImage)
        if (!Files.exists(outputImage) && !frameRequestStack.contains(request)) {
            frameRequestStack.push(request)
            logger.info(logMarker, "Enqueued frame request $request")
        }

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

    /**
     * Extracts and renders the previews for a [DbMediaItem].
     *
     * @param item The [DbMediaItem] to handle.
     * @param range The [TemporalRange] within the [DbMediaItem] to handle.
     * @param cacheLocation The cache location [Path]
     */
    fun extractSegment(item: DbMediaItem, range: TemporalRange, cacheLocation: Path) {
        Files.createDirectories(cacheLocation)
        val start = range.start.toMilliseconds()
        val end = range.end.toMilliseconds()
        val input = item.pathToOriginal()
        val output = cacheLocation.resolve(item.cachedItemName(start, end))
        extractSegment(input, toMillisecondTimeStamp(start), toMillisecondTimeStamp(end), output)
    }

    fun analyze(videoPath: Path, countFrames: Boolean = false): FFprobeResult =
        FFprobe.atPath(ffmpegBin)
            .setInput(videoPath)
            .setShowStreams(true)
            .setCountFrames(countFrames)
            .setSelectStreams(StreamType.VIDEO)
            .execute()

    fun stop() {
        threadsActive = false
    }

}
