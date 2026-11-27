package dev.dres.utilities

import java.net.HttpURLConnection
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.PosixFilePermissions
import java.security.MessageDigest
import java.util.zip.ZipFile

/** Downloads the pinned, platform-specific FFmpeg binaries used by DRES. */
object FFmpegDownloader {
    const val VERSION = "9.0.2"

    data class Artifact(val url: String, val sha256: String, val entry: String)

    private val builds = mapOf(
        "macos-arm64" to mapOf(
            "ffmpeg" to Artifact("https://ffmpeg.martin-riedl.de/download/macos/arm64/1789931890_9.0.2/ffmpeg.zip", "c8ed4c4e6978a03c485edbfe4e0a5dc2380f8a30bba5150531b31b094492d924", "ffmpeg"),
            "ffprobe" to Artifact("https://ffmpeg.martin-riedl.de/download/macos/arm64/1789931890_9.0.2/ffprobe.zip", "fcbe839537485eaee7a7a8bc5cbc0f90d53617e80943e8a5b2e31cb851197ea6", "ffprobe")
        ),
        "macos-amd64" to mapOf(
            "ffmpeg" to Artifact("https://ffmpeg.martin-riedl.de/download/macos/amd64/1789931006_9.0.2/ffmpeg.zip", "7c6b4125b191cbf773832dc51f424cf2b6bb7da43007d1e066f95909e47cacd4", "ffmpeg"),
            "ffprobe" to Artifact("https://ffmpeg.martin-riedl.de/download/macos/amd64/1789931006_9.0.2/ffprobe.zip", "2322438ed2f6319a691291b247d09c69dcaa3a982460d1f269a7e1af335cfdfd", "ffprobe")
        ),
        "windows-amd64" to mapOf(
            "ffmpeg" to Artifact("https://www.gyan.dev/ffmpeg/builds/packages/ffmpeg-9.0.2-essentials_build.zip", "60f467265b1e312373dbcd92200c2618a74850f98d3d078e94296bb3fa2047ba", "bin/ffmpeg.exe"),
            "ffprobe" to Artifact("https://www.gyan.dev/ffmpeg/builds/packages/ffmpeg-9.0.2-essentials_build.zip", "60f467265b1e312373dbcd92200c2618a74850f98d3d078e94296bb3fa2047ba", "bin/ffprobe.exe")
        )
    )

    fun platform(osName: String = System.getProperty("os.name"), osArch: String = System.getProperty("os.arch")): String? {
        val os = osName.lowercase()
        val arch = osArch.lowercase()
        val arm = arch == "aarch64" || arch == "arm64"
        val x86_64 = arch == "amd64" || arch == "x86_64" || arch == "x64"
        return when {
            os.contains("windows") && x86_64 -> "windows-amd64"
            os.contains("mac") && arm -> "macos-arm64"
            os.contains("mac") && x86_64 -> "macos-amd64"
            // Linux auto-download is intentionally unsupported: the audited upstream build enables
            // nonfree DeckLink components. Use a system package/build instead.
            else -> null
        }
    }

    fun artifacts(platform: String): Map<String, Artifact>? = builds[platform]

    fun install(targetDir: Path, platform: String, log: (String) -> Unit = {}): List<Path> =
        install(targetDir, artifacts(platform) ?: throw IllegalArgumentException("No FFmpeg build configured for platform '$platform'."), log)

    /** Installer overload permits deterministic tests using local file URLs. */
    fun install(targetDir: Path, artifacts: Map<String, Artifact>, log: (String) -> Unit = {}): List<Path> {
        require(artifacts.keys == setOf("ffmpeg", "ffprobe")) { "Exactly the ffmpeg and ffprobe artifacts are required." }
        artifacts.forEach { (tool, artifact) ->
            require(tool in setOf("ffmpeg", "ffprobe")) { "Unsupported FFmpeg tool '$tool'." }
            val entryName = artifact.entry.substringAfterLast('/')
            require('/' !in artifact.entry && '\\' !in artifact.entry || artifact.entry.split('/', '\\').none { it == ".." }) { "Invalid archive entry '${artifact.entry}'." }
            require('/' !in entryName && '\\' !in entryName && entryName !in setOf(".", "..")) { "Invalid archive entry '${artifact.entry}'." }
            require(entryName == "$tool.exe" || entryName == tool) { "Archive entry '${artifact.entry}' does not match tool '$tool'." }
            require(artifact.sha256.matches(Regex("[0-9a-fA-F]{64}"))) { "Invalid SHA-256 checksum for '$tool'." }
            require(URI.create(artifact.url).scheme in setOf("file", "https")) { "Unsupported download URL scheme for '$tool'." }
        }
        Files.createDirectories(targetDir)
        val downloads = linkedMapOf<String, Path>()
        val installed = mutableListOf<Path>()
        try {
            artifacts.forEach { (tool, artifact) ->
                val archive = downloads.getOrPut(artifact.url) {
                    val temporary = Files.createTempFile(targetDir, "dres-ffmpeg-", ".zip")
                    log("Downloading ${artifact.url}")
                    try {
                        val connection = URI.create(artifact.url).toURL().openConnection()
                        if (connection is HttpURLConnection) {
                            connection.connectTimeout = 15_000
                            connection.readTimeout = 60_000
                        }
                        connection.getInputStream().use { input ->
                            Files.newOutputStream(temporary, StandardOpenOption.TRUNCATE_EXISTING).use(input::copyTo)
                        }
                    } catch (error: Exception) {
                        Files.deleteIfExists(temporary)
                        throw error
                    }
                    temporary
                }
                verifySha256(archive, artifact.sha256)
                val entryName = artifact.entry.substringAfterLast('/')
                val target = targetDir.resolve(entryName)
                extract(archive, artifact.entry, target)
                makeExecutable(target)
                installed.add(target)
                log("Installed $tool to $target")
            }
        } finally {
            downloads.values.forEach { Files.deleteIfExists(it) }
        }
        return installed
    }

    private fun verifySha256(file: Path, expected: String) {
        val digest = MessageDigest.getInstance("SHA-256")
        Files.newInputStream(file).use { input ->
            val buffer = ByteArray(65536)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        val actual = digest.digest().joinToString("") { "%02x".format(it) }
        if (!actual.equals(expected, ignoreCase = true)) {
            throw IllegalStateException("SHA-256 mismatch for ${file.fileName}: expected $expected, got $actual")
        }
    }

    private fun extract(archive: Path, entry: String, target: Path) {
        ZipFile(archive.toFile()).use { zip ->
            val matchingEntry = zip.entries().asSequence().firstOrNull { it.name == entry || it.name.endsWith("/$entry") }
                ?: throw IllegalStateException("Entry '$entry' not found in ${archive.fileName}.")
            zip.getInputStream(matchingEntry).use { input ->
                Files.newOutputStream(target, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING).use(input::copyTo)
            }
        }
    }

    private fun makeExecutable(path: Path) {
        try {
            Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rwxr-xr-x"))
        } catch (_: UnsupportedOperationException) {
            // Windows filesystems do not expose POSIX permissions.
        }
    }
}
