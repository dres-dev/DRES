package dres.mgmt.cache

import dev.dres.mgmt.cache.AbstractPreviewRequest
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * A preview is written to a scratch file and moved into place, so `Files.exists(cacheFile)` means "the
 * preview is complete". Before this, jobs wrote straight into the cache file, and every reader that polls
 * for existence - the preview handlers, the hint and target handlers, other jobs' "is it cached yet"
 * check - could be handed a truncated or zero-byte file that then stayed cached as if it were valid.
 */
class PreviewPublicationTest {

    /** Minimal job whose body is supplied by the test. */
    private class TestRequest(input: Path, output: Path, private val body: (Path) -> Unit) : AbstractPreviewRequest(input, output) {
        override fun call(): Path = this.publish(this.body)
    }

    @Test
    fun `a finished file appears at the output path`(@TempDir folder: Path) {
        val input = Files.writeString(folder.resolve("in.mp4"), "source")
        val output = folder.resolve("out.mp4")

        val result = TestRequest(input, output) { scratch -> Files.writeString(scratch, "rendered") }.call()

        assertEquals(output, result)
        assertEquals("rendered", Files.readString(output))
    }

    /** The scratch file must not survive, and must not be visible under its own name afterwards. */
    @Test
    fun `the scratch file does not outlive the request`(@TempDir folder: Path) {
        val input = Files.writeString(folder.resolve("in.mp4"), "source")
        val output = folder.resolve("out.mp4")
        var scratchSeen: Path? = null

        TestRequest(input, output) { scratch ->
            scratchSeen = scratch
            Files.writeString(scratch, "rendered")
        }.call()

        assertTrue(Files.exists(output))
        assertFalse(Files.exists(scratchSeen), "The scratch file $scratchSeen should have been moved away.")
        assertEquals(listOf("in.mp4", "out.mp4"), Files.list(folder).use { s -> s.map { it.fileName.toString() }.sorted().toList() })
    }

    /** A reader polling the output path while a render runs must never observe a partial file. */
    @Test
    fun `the output path is absent until the render is complete`(@TempDir folder: Path) {
        val input = Files.writeString(folder.resolve("in.mp4"), "source")
        val output = folder.resolve("out.mp4")
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        val executor = Executors.newSingleThreadExecutor()

        try {
            val future = executor.submit<Path> {
                TestRequest(input, output) { scratch ->
                    started.countDown()
                    /* Simulate a long FFmpeg run: the scratch file exists and is half written. */
                    Files.writeString(scratch, "half")
                    release.await(10, TimeUnit.SECONDS)
                    Files.writeString(scratch, "fully rendered")
                }.call()
            }

            assertTrue(started.await(10, TimeUnit.SECONDS), "The render never started")
            assertFalse(Files.exists(output), "The output path must not exist while the render is incomplete")

            release.countDown()
            future.get(10, TimeUnit.SECONDS)
            assertEquals("fully rendered", Files.readString(output))
        } finally {
            executor.shutdownNow()
        }
    }

    /** A zero-byte render is a failure, not a valid preview, and must leave no output behind. */
    @Test
    fun `an empty render is rejected and leaves no output`(@TempDir folder: Path) {
        val input = Files.writeString(folder.resolve("in.mp4"), "source")
        val output = folder.resolve("out.mp4")

        assertThrows(IOException::class.java) {
            TestRequest(input, output) { scratch -> Files.createFile(scratch) }.call()
        }
        assertFalse(Files.exists(output))
        assertEquals(listOf("in.mp4"), Files.list(folder).use { s -> s.map { it.fileName.toString() }.toList() })
    }

    /**
     * A failed re-render keeps the previous preview: a transient FFmpeg failure must not destroy a preview
     * that earlier requests are still being served from.
     */
    @Test
    fun `a failed render keeps the previously published file`(@TempDir folder: Path) {
        val input = Files.writeString(folder.resolve("in.mp4"), "source")
        val output = Files.writeString(folder.resolve("out.mp4"), "previous good preview")

        assertThrows(IllegalStateException::class.java) {
            TestRequest(input, output) { scratch ->
                Files.writeString(scratch, "partial")
                throw IllegalStateException("FFmpeg exploded")
            }.call()
        }

        assertArrayEquals("previous good preview".toByteArray(), Files.readAllBytes(output))
        assertEquals(listOf("in.mp4", "out.mp4"), Files.list(folder).use { s -> s.map { it.fileName.toString() }.sorted().toList() })
    }
}
