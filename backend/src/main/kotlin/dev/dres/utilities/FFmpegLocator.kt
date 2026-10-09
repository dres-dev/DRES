package dev.dres.utilities

import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/** Locates an FFmpeg installation containing both ffmpeg and ffprobe. */
object FFmpegLocator {
    private val ffmpegNames = listOf("ffmpeg", "ffmpeg.exe")
    private val ffprobeNames = listOf("ffprobe", "ffprobe.exe")

    data class Installation(val directory: Path, val ffmpeg: Path, val ffprobe: Path)

    /**
     * Searches configured location, candidate directories, then PATH entries, in that order.
     * A configured location may be either a directory or a path to one of the binaries.
     */
    fun find(
        configured: Path?,
        candidates: List<Path>,
        pathVariable: String? = System.getenv("PATH"),
        pathSeparator: String = File.pathSeparator
    ): Installation? {
        if (configured != null) {
            val directory = if (Files.isDirectory(configured)) configured else configured.parent
            if (directory != null) installationIn(directory)?.let { return it }
        }

        candidates.forEach { candidate -> installationIn(candidate)?.let { return it } }

        if (pathVariable != null) {
            pathVariable.split(pathSeparator).asSequence()
                .filter(String::isNotBlank)
                .map { Paths.get(it) }
                .forEach { directory -> installationIn(directory)?.let { return it } }
        }
        return null
    }

    private fun installationIn(directory: Path): Installation? {
        if (!Files.isDirectory(directory)) return null
        val ffmpeg = ffmpegNames.asSequence().map(directory::resolve).firstOrNull(::isUsable) ?: return null
        val ffprobe = ffprobeNames.asSequence().map(directory::resolve).firstOrNull(::isUsable) ?: return null
        return Installation(directory, ffmpeg, ffprobe)
    }

    private fun isUsable(path: Path): Boolean = Files.isRegularFile(path) &&
        (Files.isExecutable(path) || !FileSystems.getDefault().supportedFileAttributeViews().contains("posix"))
}
