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

    private val frameRequestQueue = ConcurrentLinkedQueue<FrameRequest>()

    private const val concurrentFrameRequests = 4
    private var threadRunning = true

    private val frameExtractionManagementThread = Thread {

        val futureList = mutableListOf<Pair<FrameRequest, Future<FFmpegResult>>>()

        while (this.threadRunning) {

            try {
                futureList.removeIf {
                    val future = it.second
                    if (future.isDone || future.isCancelled) {

                        logger.info("Frame request for ${it.first.video} @ ${it.first.timecode} ${if (future.isDone) "done" else "cancelled"}")
                        return@removeIf true

                    }
                    return@removeIf false
                }

                if (futureList.size < concurrentFrameRequests) {

                    val request = frameRequestQueue.poll()

                    if (request != null) {
                        futureList.add(
                            request to extractFrameAsync(request.video, request.timecode, request.outputImage)
                        )
                        logger.info("Processing frame request for ${request.video} @ ${request.timecode}")
                    }

                }
            } catch (e: Exception) {
                logger.error("Error in frameExtractionManagementThread", e)
            }

            Thread.sleep(50)

        }

    }.also {
        it.name = "frameExtractionManagementThread"
        it.isDaemon = true
    }


    private val imageStreamPool =
        ThreadPoolExecutor(50, 500, 1, TimeUnit.MINUTES, ArrayBlockingQueue(10_000), NamedThreadFactory("ImageStreamPool"))

    fun previewImageStream(path: Path): CompletableFuture<InputStream>? {

        if (!Files.exists(path) && frameRequestQueue.none { it.outputImage == path }) {
            return null //image neither exists nor is scheduled to be generated
        }

        val future = CompletableFuture<InputStream>()

        imageStreamPool.execute {

            var tries = 0

            while (!Files.exists(path) && tries < 250) {
                ++tries
                Thread.sleep(100)
            }


            val stream = if (Files.exists(path)) {
                try {
                    path.toFile().inputStream()
                } catch (e: FileNotFoundException) {
                    //should not happen
                    null
                }
            } else {
                null
            }

            if (stream != null) {
                future.complete(stream)
            } else {
                //return empty result
                future.complete(InputStream.nullInputStream())
            }
        }

        return future

    }

    init {
        frameExtractionManagementThread.start()
    }

    private fun toMillisecondTimeStamp(ms: Long): String {
        val hours = ms / (1000 * 3600)
        val minutes = (ms % (1000 * 3600)) / (60_000)
        val seconds = (ms % 60_000) / 1000
        val milliseconds = ms % 1000

        return "$hours:$minutes:$seconds.$milliseconds"
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
        if (!Files.exists(outputImage) && !frameRequestQueue.contains(request)) {
            frameRequestQueue.add(request)
        }

    }

    fun extractFrame(video: Path, ms: Long, outputImage: Path) =
        extractFrame(video, toMillisecondTimeStamp(ms), outputImage)

    fun extractSegment(video: Path, startTimecode: String, endTimecode: String, outputVideo: Path) {
        try {
            //semaphore.acquire()
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

    fun prepareMediaSegmentTask(description: CachedVideoItem, collectionBasePath: String, cacheLocation: File) {

        cacheLocation.mkdirs()

        val input = File(File(collectionBasePath), description.item.location).toPath()
        val output = File(cacheLocation, description.cacheItemName()).toPath()
        val range = description.temporalRange.toMilliseconds()

        extractSegment(input, toMillisecondTimeStamp(range.first), toMillisecondTimeStamp(range.second), output)

    }

    fun analyze(videoPath: Path, countFrames: Boolean = false): FFprobeResult = FFprobe.atPath(ffmpegBin)
        .setInput(videoPath)
        .setShowStreams(true)
        .setCountFrames(countFrames)
        .setSelectStreams(StreamType.VIDEO)
        .execute()

    fun stop() {
        threadRunning = false
        imageStreamPool.shutdownNow()
    }

}