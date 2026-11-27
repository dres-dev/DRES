package dev.dres.utilities

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class FFmpegLocatorTest {
    @TempDir
    lateinit var tempDir: Path

    private fun binaries(directory: Path, names: List<String> = listOf("ffmpeg", "ffprobe")): Path {
        Files.createDirectories(directory)
        names.forEach { name ->
            val binary = Files.write(directory.resolve(name), byteArrayOf(1))
            binary.toFile().setExecutable(true)
        }
        return directory
    }

    @Test
    fun `prefers configured directory`() {
        val configured = binaries(tempDir.resolve("configured"))
        val fallback = binaries(tempDir.resolve("fallback"))

        val found = FFmpegLocator.find(configured, listOf(fallback), pathVariable = "")

        assertNotNull(found)
        assertEquals(configured, found.directory)
        assertEquals(configured.resolve("ffmpeg"), found.ffmpeg)
        assertEquals(configured.resolve("ffprobe"), found.ffprobe)
    }

    @Test
    fun `configured binary path resolves through its parent directory`() {
        val configured = binaries(tempDir.resolve("configured"))

        val found = FFmpegLocator.find(configured.resolve("ffmpeg"), emptyList(), pathVariable = "")

        assertNotNull(found)
        assertEquals(configured, found.directory)
    }

    @Test
    fun `searches candidate directories in order`() {
        val first = binaries(tempDir.resolve("first"))
        val second = binaries(tempDir.resolve("second"))

        val found = FFmpegLocator.find(null, listOf(first, second), pathVariable = "")

        assertNotNull(found)
        assertEquals(first, found.directory)
    }

    @Test
    fun `finds binaries in PATH directories`() {
        val first = binaries(tempDir.resolve("path-one"))
        val second = binaries(tempDir.resolve("path-two"))

        val found = FFmpegLocator.find(null, emptyList(), "$first${File.pathSeparator}$second")

        assertNotNull(found)
        assertEquals(first, found.directory)
    }

    @Test
    fun `accepts Windows executable names`() {
        val directory = binaries(tempDir.resolve("windows"), listOf("ffmpeg.exe", "ffprobe.exe"))

        val found = FFmpegLocator.find(null, listOf(directory), pathVariable = "")

        assertNotNull(found)
        assertEquals(directory.resolve("ffmpeg.exe"), found.ffmpeg)
        assertEquals(directory.resolve("ffprobe.exe"), found.ffprobe)
    }

    @Test
    fun `ignores directory that does not contain both tools`() {
        val incomplete = binaries(tempDir.resolve("incomplete"), listOf("ffmpeg"))

        assertNull(FFmpegLocator.find(null, listOf(incomplete), pathVariable = ""))
    }

    @Test
    fun `returns null when no executable installation is found`() {
        assertNull(FFmpegLocator.find(null, listOf(tempDir.resolve("missing")), pathVariable = ""))
    }
}
