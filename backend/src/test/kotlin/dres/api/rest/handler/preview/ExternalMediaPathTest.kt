package dev.dres.api.rest.handler.preview

import dev.dres.api.rest.handler.preview.ExternalMediaPath
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class ExternalMediaPathTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `resolves regular files inside root`() {
        val root = Files.createDirectory(tempDir.resolve("external"))
        val file = Files.writeString(root.resolve("clip.mp4"), "media")

        assertEquals(file.toRealPath(), ExternalMediaPath.resolve(root, "clip.mp4"))
    }

    @Test
    fun `rejects traversal and missing files`() {
        val root = Files.createDirectory(tempDir.resolve("external"))
        Files.writeString(tempDir.resolve("secret.txt"), "secret")

        assertNull(ExternalMediaPath.resolve(root, "../secret.txt"))
        assertNull(ExternalMediaPath.resolve(root, "missing.mp4"))
        assertNull(ExternalMediaPath.resolve(root, "."))
    }

    @Test
    fun `rejects symlinks that escape root`() {
        val root = Files.createDirectory(tempDir.resolve("external"))
        val secret = Files.writeString(tempDir.resolve("secret.txt"), "secret")
        try {
            Files.createSymbolicLink(root.resolve("alias.txt"), secret)
        } catch (_: UnsupportedOperationException) {
            return
        } catch (_: SecurityException) {
            return
        } catch (_: java.io.IOException) {
            return
        }

        assertNull(ExternalMediaPath.resolve(root, "alias.txt"))
    }
}
