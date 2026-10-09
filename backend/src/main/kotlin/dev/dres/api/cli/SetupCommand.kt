package dev.dres.api.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.option
import dev.dres.DRES
import dev.dres.utilities.FFmpegDownloader
import java.nio.file.Path
import java.nio.file.Paths

/** Commands for installing optional runtime dependencies. */
class SetupCommand : NoOpCliktCommand(name = "setup") {
    init {
        subcommands(FFmpeg())
    }

    override fun help(context: Context): String = "Sets up optional external dependencies."

    companion object {
        fun installDefault(targetDir: Path? = null, log: (String) -> Unit = { println(it) }): List<Path> {
            val platform = FFmpegDownloader.platform()
                ?: throw IllegalArgumentException("No FFmpeg build is available for ${System.getProperty("os.name")}/${System.getProperty("os.arch")}.")
            val target = targetDir ?: DRES.DATA_ROOT.resolve("ffmpeg")
            log("Installing FFmpeg ${FFmpegDownloader.VERSION} ($platform) to $target...")
            return FFmpegDownloader.install(target, platform, log)
        }
    }

    class FFmpeg : CliktCommand(name = "ffmpeg") {
        private val target: String? by option("--target", help = "Target directory (default: <data root>/ffmpeg)")

        override fun help(context: Context): String = "Downloads a pinned FFmpeg build for this platform."

        override fun run() {
            try {
                val targetDir = target?.let(Paths::get) ?: DRES.DATA_ROOT.resolve("ffmpeg")
                val installed = installDefault(targetDir)
                println("Installed: ${installed.joinToString(", ")}")
                println("FFmpeg installed at $targetDir. Restart DRES to use it.")
            } catch (error: Exception) {
                System.err.println("ERROR: FFmpeg setup failed: ${error.message}")
                throw UsageError("FFmpeg setup failed: ${error.message}")
            }
        }
    }
}
