package dres.utilities

import com.github.kokorin.jaffree.ffmpeg.FFmpeg
import com.github.kokorin.jaffree.ffmpeg.UrlInput
import com.github.kokorin.jaffree.ffmpeg.UrlOutput
import dres.data.model.competition.interfaces.MediaSegmentTaskDescription
import java.io.File
import java.nio.file.Path

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

    fun extractFrame(video: Path, timecode: String, outputImage: Path) {
        FFmpeg.atPath(ffmpegBin)
            .addInput(UrlInput.fromPath(video))
            .addOutput(UrlOutput.toPath(outputImage))
            .setOverwriteOutput(true)
            .addArguments("-ss", timecode)
            .addArguments("-vframes", "1")
            .execute()
    }

    fun extractSegment(video: Path, startTimecode: String, endTimecode: String, outputVideo: Path) {
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
    }

    fun prepareMediaSegmentTask(description: MediaSegmentTaskDescription, collectionBasePath: String, cacheLocation: File) {

        cacheLocation.mkdirs()

        val input = File(File(collectionBasePath), description.item.location).toPath()
        val output = File(cacheLocation, description.cacheItemName()).toPath()
        val range = TimeUtil.toMilliseconds(description.temporalRange)

        extractSegment(input, "${range.first / 1000}", "${range.second / 1000}", output)

    }



}