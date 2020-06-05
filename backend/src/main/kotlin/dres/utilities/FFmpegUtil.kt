package dres.utilities

import com.github.kokorin.jaffree.ffmpeg.FFmpeg
import com.github.kokorin.jaffree.ffmpeg.FFmpegResult
import com.github.kokorin.jaffree.ffmpeg.UrlInput
import com.github.kokorin.jaffree.ffmpeg.UrlOutput
import dres.data.model.competition.interfaces.MediaSegmentTaskDescription
import java.io.File
import java.nio.file.Path
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Future

object FFmpegUtil {

    private val ffmpegBin = Path.of("ext/ffmpeg/${
    
    System.getProperty("os.name").let { 
        when{
            it.contains("Windows") -> "win"
            it.contains("Mac OS") -> "macos"
            else -> "linux"
        }
    }
        
    }") //TODO make configurable

    private data class FrameRequest(val video: Path, val timecode: String, val outputImage: Path)

    private val frameRequestQueue = ConcurrentLinkedQueue<FrameRequest>()

    private const val concurrentFrameRequests = 4

    private val frameExtractionManagementThread = Thread {

        val futureList = mutableListOf<Future<FFmpegResult>>()

        while (true) {

            try {
                futureList.removeIf { it.isDone || it.isCancelled }

                if (futureList.size < concurrentFrameRequests) {

                    val request = frameRequestQueue.poll()

                    if (request != null) {
                        futureList.add(
                                extractFrameAsync(request.video, request.timecode, request.outputImage)
                        )
                    }

                }
            } catch (e: Exception) {
                //TODO ??
            }

            Thread.sleep(50)

        }

    }.also {
        it.isDaemon = true
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
                    .addArguments("-filter:v", "scale=\"120:-1\"")
                    .executeAsync()



    fun extractFrame(video: Path, timecode: String, outputImage: Path) {
        frameRequestQueue.add(FrameRequest(video, timecode, outputImage))
    }

    fun extractFrame(video: Path, ms: Long, outputImage: Path) = extractFrame(video, toMillisecondTimeStamp(ms), outputImage)

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
                    .execute()
        } finally {
            //semaphore.release()
        }
    }

    fun prepareMediaSegmentTask(description: MediaSegmentTaskDescription, collectionBasePath: String, cacheLocation: File) {

        cacheLocation.mkdirs()

        val input = File(File(collectionBasePath), description.item.location).toPath()
        val output = File(cacheLocation, description.cacheItemName()).toPath()
        val range = TimeUtil.toMilliseconds(description.temporalRange)

        extractSegment(input, "${range.first / 1000}", "${range.second / 1000}", output)

    }

}