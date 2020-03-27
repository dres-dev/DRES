package dres.utilities.extensions

import com.github.kokorin.jaffree.ffmpeg.FFmpeg
import com.github.kokorin.jaffree.ffmpeg.UrlInput
import com.github.kokorin.jaffree.ffmpeg.UrlOutput
import java.nio.file.Path

object FFmpegUtil {

    private val ffmpegBin = Path.of("ext/ffmpeg") //TODO make configurable

    fun extractFrame(video: Path, timecode: String, outputImage: Path) {
        FFmpeg.atPath(ffmpegBin)
            .addInput(UrlInput.fromPath(video))
            .addOutput(UrlOutput.toPath(outputImage))
            .setOverwriteOutput(true)
            .addArguments("-ss", timecode)
            .addArguments("-vframes", "1")
            .execute()
    }

}