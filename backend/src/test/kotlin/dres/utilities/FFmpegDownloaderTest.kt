package dev.dres.utilities

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FFmpegDownloaderTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `maps supported platforms`() {
        assertNull(FFmpegDownloader.platform("Linux", "amd64"))
        assertEquals("macos-arm64", FFmpegDownloader.platform("Mac OS X", "aarch64"))
        assertEquals("macos-amd64", FFmpegDownloader.platform("Mac OS X", "x86_64"))
        assertEquals("windows-amd64", FFmpegDownloader.platform("Windows 11", "amd64"))
        assertNull(FFmpegDownloader.platform("Linux", "aarch64"))
    }

    @Test
    fun `pinned artifacts have HTTPS URLs and SHA-256 checksums`() {
        for (platform in listOf("macos-arm64", "macos-amd64", "windows-amd64")) {
            val artifacts = FFmpegDownloader.artifacts(platform)
            assertNotNull(artifacts)
            assertEquals(setOf("ffmpeg", "ffprobe"), artifacts.keys)
            artifacts.values.forEach {
                assertTrue(it.url.startsWith("https://"))
                assertTrue(it.sha256.matches(Regex("[0-9a-f]{64}")))
            }
        }
    }

    @Test
    fun `installs and validates binaries from local archives`() {
        val archive = zip("ffmpeg" to "ffmpeg-content", "ffprobe" to "ffprobe-content")
        val checksum = sha256(archive)
        val url = archive.toUri().toString()
        val artifacts = mapOf(
            "ffmpeg" to FFmpegDownloader.Artifact(url, checksum, "ffmpeg"),
            "ffprobe" to FFmpegDownloader.Artifact(url, checksum, "ffprobe")
        )

        val installed = FFmpegDownloader.install(tempDir.resolve("installed"), artifacts)

        assertEquals(setOf("ffmpeg", "ffprobe"), installed.map { it.fileName.toString() }.toSet())
        assertEquals("ffmpeg-content", Files.readString(installed.single { it.fileName.toString() == "ffmpeg" }))
        assertEquals("ffprobe-content", Files.readString(installed.single { it.fileName.toString() == "ffprobe" }))
        if ("posix" in FileSystems.getDefault().supportedFileAttributeViews()) {
            assertTrue(installed.all { Files.isExecutable(it) })
        }
        assertEquals(setOf("ffmpeg", "ffprobe"), Files.list(tempDir.resolve("installed")).use { stream -> stream.map { it.fileName.toString() }.toList() }.toSet())
    }

    @Test
    fun `extracts binaries nested in archive directories`() {
        val archive = zip("ffmpeg-9.0.2-essentials_build/bin/ffmpeg.exe" to "ffmpeg", "ffmpeg-9.0.2-essentials_build/bin/ffprobe.exe" to "ffprobe")
        val checksum = sha256(archive)
        val url = archive.toUri().toString()
        val artifacts = mapOf(
            "ffmpeg" to FFmpegDownloader.Artifact(url, checksum, "bin/ffmpeg.exe"),
            "ffprobe" to FFmpegDownloader.Artifact(url, checksum, "bin/ffprobe.exe")
        )

        val installed = FFmpegDownloader.install(tempDir.resolve("windows"), artifacts)

        assertEquals(setOf("ffmpeg.exe", "ffprobe.exe"), installed.map { it.fileName.toString() }.toSet())
    }

    @Test
    fun `rejects archive with incorrect checksum`() {
        val archive = zip("ffmpeg" to "bad")
        val artifacts = mapOf(
            "ffmpeg" to FFmpegDownloader.Artifact(archive.toUri().toString(), "0".repeat(64), "ffmpeg"),
            "ffprobe" to FFmpegDownloader.Artifact(archive.toUri().toString(), "0".repeat(64), "ffprobe")
        )

        val error = assertFailsWith<IllegalStateException> {
            FFmpegDownloader.install(tempDir.resolve("bad-checksum"), artifacts)
        }
        assertTrue(error.message.orEmpty().contains("SHA-256 mismatch"))
        assertTrue(Files.list(tempDir.resolve("bad-checksum")).use { stream -> stream.noneMatch { it.fileName.toString() in setOf("ffmpeg", "ffprobe") } })
    }

    @Test
    fun `rejects archive entries that escape the target directory`() {
        val archive = zip("ffmpeg" to "ffmpeg-content", "ffprobe" to "ffprobe-content")
        val checksum = sha256(archive)
        val url = archive.toUri().toString()
        val artifacts = mapOf(
            "ffmpeg" to FFmpegDownloader.Artifact(url, checksum, "ffmpeg"),
            "ffprobe" to FFmpegDownloader.Artifact(url, checksum, "../ffprobe")
        )
        val target = tempDir.resolve("safe-target")

        assertFailsWith<IllegalArgumentException> { FFmpegDownloader.install(target, artifacts) }
        assertFalse(Files.exists(target.resolve("ffmpeg")))
        assertFalse(Files.exists(tempDir.resolve("ffprobe")))
    }

    private fun zip(vararg entries: Pair<String, String>): Path {
        val archive = Files.createTempFile(tempDir, "fixture", ".zip")
        ZipOutputStream(Files.newOutputStream(archive)).use { output ->
            entries.forEach { (name, content) ->
                output.putNextEntry(ZipEntry(name))
                output.write(content.toByteArray())
                output.closeEntry()
            }
        }
        return archive
    }

    private fun sha256(file: Path): String = MessageDigest.getInstance("SHA-256")
        .digest(Files.readAllBytes(file))
        .joinToString("") { "%02x".format(it) }
}
