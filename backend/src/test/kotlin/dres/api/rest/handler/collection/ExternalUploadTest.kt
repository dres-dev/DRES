package dev.dres.api.rest.handler.collection

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path

class ExternalUploadTest {

    @TempDir
    lateinit var tempDir: Path

    private val payload = "0123456789".repeat(100).toByteArray()

    @Test
    fun `writes the body and reports the byte count`() {
        val target = tempDir.resolve("clip.mp4")

        val written = ExternalUpload.receive(target, ByteArrayInputStream(payload))

        assertEquals(payload.size.toLong(), written)
        assertArrayEquals(payload, Files.readAllBytes(target))
    }

    @Test
    fun `leaves no scratch file behind on success`() {
        val target = tempDir.resolve("clip.mp4")

        ExternalUpload.receive(target, ByteArrayInputStream(payload))

        val leftovers = Files.list(tempDir).use { files -> files.filter { it.fileName.toString().endsWith(".part") }.toList() }
        assertTrue(leftovers.isEmpty(), "expected no scratch files, found $leftovers")
    }

    @Test
    fun `replaces an existing file only once the body is complete`() {
        val target = Files.writeString(tempDir.resolve("clip.mp4"), "old")

        ExternalUpload.receive(target, ByteArrayInputStream(payload))

        assertArrayEquals(payload, Files.readAllBytes(target))
    }

    @Test
    fun `keeps the previous file when the upload fails midway`() {
        val target = Files.writeString(tempDir.resolve("clip.mp4"), "old")
        val exploding = object : InputStream() {
            private var sent = 0
            override fun read(): Int = throw IOException("connection reset")
            override fun read(b: ByteArray, off: Int, len: Int): Int {
                if (sent > 0) throw IOException("connection reset")
                sent = 1
                System.arraycopy(payload, 0, b, off, minOf(len, payload.size))
                return minOf(len, payload.size)
            }
        }

        assertThrows(IOException::class.java) { ExternalUpload.receive(target, exploding) }

        assertEquals("old", Files.readString(target))
        val leftovers = Files.list(tempDir).use { files -> files.filter { it.fileName.toString().endsWith(".part") }.toList() }
        assertTrue(leftovers.isEmpty(), "expected the scratch file to be removed, found $leftovers")
    }

    @Test
    fun `refuses a body over the limit and leaves nothing behind`() {
        val target = tempDir.resolve("clip.mp4")
        val tooLarge = ByteArray(2048)

        val failure = assertThrows(UploadTooLargeException::class.java) {
            ExternalUpload.receive(target, ByteArrayInputStream(tooLarge), maxBytes = 1024)
        }

        assertEquals(1024, failure.maxBytes)
        assertFalse(Files.exists(target))

        val leftovers = Files.list(tempDir).use { files -> files.toList() }
        assertTrue(leftovers.isEmpty(), "expected the rejected upload to leave nothing, found $leftovers")
    }

    @Test
    fun `refuses an empty body`() {
        val target = tempDir.resolve("clip.mp4")

        assertThrows(EmptyUploadException::class.java) {
            ExternalUpload.receive(target, ByteArrayInputStream(ByteArray(0)))
        }

        assertFalse(Files.exists(target))
    }

    @Test
    fun `accepts a body exactly at the limit`() {
        val target = tempDir.resolve("clip.mp4")
        val exact = ByteArray(1024) { 7 }

        assertEquals(1024L, ExternalUpload.receive(target, ByteArrayInputStream(exact), maxBytes = 1024))
        assertArrayEquals(exact, Files.readAllBytes(target))
    }

    /** Reads in small pieces so the copy loop is exercised over several iterations. */
    @Test
    fun `handles a body that arrives in small chunks`() {
        val target = tempDir.resolve("clip.mp4")
        val chunked = object : InputStream() {
            private var position = 0
            private val chunk = 7
            override fun read(): Int = if (position < payload.size) payload[position++].toInt() and 0xFF else -1
            override fun read(b: ByteArray, off: Int, len: Int): Int {
                if (position >= payload.size) return -1
                val count = minOf(chunk, len, payload.size - position)
                System.arraycopy(payload, position, b, off, count)
                position += count
                return count
            }
        }

        assertEquals(payload.size.toLong(), ExternalUpload.receive(target, chunked))
        assertArrayEquals(payload, Files.readAllBytes(target))
    }

    private fun assertArrayEquals(expected: ByteArray, actual: ByteArray) =
        org.junit.jupiter.api.Assertions.assertArrayEquals(expected, actual)
}
