package dres.utilities

import com.github.kokorin.jaffree.ffmpeg.FFmpeg
import com.github.kokorin.jaffree.ffmpeg.UrlInput
import com.github.kokorin.jaffree.ffmpeg.UrlOutput
import dres.data.model.competition.interfaces.MediaSegmentTaskDescription
import java.io.File
import java.nio.file.Path
import java.util.concurrent.Semaphore

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

    private val semaphore = Semaphore(16) //TODO make number configurable

    private fun toMillisecondTimeStamp(ms: Long): String {
        val hours = ms / (1000 * 3600)
        val minutes = (ms % (1000 * 3600)) / (60_000)
        val seconds = (ms % 60_000) / 1000
        val milliseconds = ms % 1000

        return "$hours:$minutes:$seconds.$milliseconds"
    }


    fun extractFrame(video: Path, timecode: String, outputImage: Path) {
        try {
            semaphore.acquire()
            FFmpeg.atPath(ffmpegBin)
                    .addInput(UrlInput.fromPath(video))
                    .addOutput(UrlOutput.toPath(outputImage))
                    .setOverwriteOutput(true)
                    .addArguments("-ss", timecode)
                    .addArguments("-vframes", "1")
                    .executeAsync()
        } finally {
            semaphore.release()
        }
    }

    fun extractFrame(video: Path, ms: Long, outputImage: Path) = extractFrame(video, toMillisecondTimeStamp(ms), outputImage)

    fun extractSegment(video: Path, startTimecode: String, endTimecode: String, outputVideo: Path) {
        try {
            semaphore.acquire()
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
            semaphore.release()
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