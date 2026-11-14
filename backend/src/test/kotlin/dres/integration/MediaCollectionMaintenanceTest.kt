package dres.integration

import dev.dres.api.rest.types.collection.ApiCollectionScanState
import dev.dres.api.rest.types.collection.ApiMediaItem
import dev.dres.api.rest.types.collection.ApiMediaType
import dev.dres.data.model.media.DbMediaCollection
import dev.dres.data.model.media.DbMediaItem
import dev.dres.mgmt.MediaCollectionMaintenance
import dev.dres.mgmt.MediaCollectionManager
import dev.dres.mgmt.TemplateManager
import kotlinx.dnq.query.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Tests the collection maintenance operations used by the admin GUI and the CLI: scan, check, CSV export and bulk deletion.
 */
class MediaCollectionMaintenanceTest : AbstractDresIntegrationTest() {

    @BeforeAll
    fun initManagers() {
        MediaCollectionManager.init(store)
        TemplateManager.init(store)
        /* No ffprobe in tests: every video analysis fails and has to be reported as an issue. */
        MediaCollectionMaintenance.init(store) { Paths.get("/nonexistent-ffmpeg") }
    }

    private fun collectionAt(folder: Path): String =
        MediaCollectionManager.createCollection("maint-${UUID.randomUUID()}", null, folder.toString())!!.id!!

    private fun itemNames(collectionId: String): Set<String> = store.transactional(true) {
        DbMediaCollection.query(DbMediaCollection::id eq collectionId).first().items.asSequence().map { it.name }.toSet()
    }

    @Test
    fun `scan adds new images, skips existing ones and reports videos that cannot be analysed`(@TempDir folder: Path) {
        Files.createDirectories(folder.resolve("sub"))
        listOf("a.jpg", "B.JPG", "sub/c.png", "ignored.txt", "broken.mp4").forEach { Files.writeString(folder.resolve(it), "x") }
        val id = collectionAt(folder)

        val first = MediaCollectionMaintenance.scan(id, listOf("mp4"), listOf("jpg", ".png")) { }
        assertEquals(ApiCollectionScanState.DONE, first.state)
        assertEquals(4, first.filesFound)
        assertEquals(3, first.itemsAdded)
        assertEquals(1, first.issueCount, "The fake video cannot be analysed")
        assertEquals(setOf("a", "B", "c"), itemNames(id))

        val second = MediaCollectionMaintenance.scan(id, listOf("mp4"), listOf("jpg", "png")) { }
        assertEquals(0, second.itemsAdded)
        assertEquals(3, second.itemsSkipped)
    }

    @Test
    fun `scan skips duplicate item names from nested folders`(@TempDir folder: Path) {
        Files.createDirectories(folder.resolve("one"))
        Files.createDirectories(folder.resolve("two"))
        Files.writeString(folder.resolve("one/same.jpg"), "one")
        Files.writeString(folder.resolve("two/same.jpg"), "two")
        val id = collectionAt(folder)

        val result = MediaCollectionMaintenance.scan(id, emptyList(), listOf("jpg")) { }

        assertEquals(2, result.filesFound)
        assertEquals(1, result.itemsAdded)
        assertEquals(1, result.itemsSkipped)
        assertEquals(setOf("same"), itemNames(id))
    }

    @Test
    fun `scan skips duplicate names across chunk boundaries`(@TempDir folder: Path) {
        val firstFolder = Files.createDirectories(folder.resolve("000"))
        val middleFolder = Files.createDirectories(folder.resolve("middle"))
        val lastFolder = Files.createDirectories(folder.resolve("zzz"))
        Files.writeString(firstFolder.resolve("same.jpg"), "first")
        repeat(199) { Files.writeString(middleFolder.resolve("file-$it.jpg"), "unique") }
        Files.writeString(lastFolder.resolve("same.jpg"), "duplicate")
        val id = collectionAt(folder)

        val result = MediaCollectionMaintenance.scan(id, emptyList(), listOf("jpg")) { }

        assertEquals(201, result.filesFound)
        assertEquals(200, result.itemsAdded)
        assertEquals(1, result.itemsSkipped)
        assertEquals(200, itemNames(id).size)
    }

    @Test
    fun `scan rejects a file replaced by an outside symlink during video analysis`(@TempDir folder: Path) {
        val video = folder.resolve("video.mp4")
        val outside = folder.resolveSibling("outside-${UUID.randomUUID()}.mp4")
        Files.writeString(video, "inside")
        Files.writeString(outside, "outside")
        val id = collectionAt(folder)
        var replacedWithSymlink = false

        try {
            val result = MediaCollectionMaintenance.scan(id, listOf("mp4"), emptyList(), { file ->
                Files.delete(file)
                try {
                    Files.createSymbolicLink(file, outside)
                    replacedWithSymlink = true
                } catch (_: UnsupportedOperationException) {
                    // Symlinks are not available on every platform.
                } catch (_: java.io.IOException) {
                    // Creating symlinks may require extra privileges on some platforms.
                }
                1000L to 25f
            }) { }

            assumeTrue(replacedWithSymlink, "The platform must support creating symlinks")
            assertEquals(ApiCollectionScanState.DONE, result.state)
            assertEquals(1, result.filesFound)
            assertEquals(0, result.itemsAdded)
            assertEquals(1, result.issueCount)
            assertTrue(itemNames(id).isEmpty())
        } finally {
            Files.deleteIfExists(video)
            Files.deleteIfExists(outside)
        }
    }

    @Test
    fun `only one scan of a collection can run at a time`(@TempDir folder: Path) {
        Files.writeString(folder.resolve("video.mp4"), "x")
        val id = collectionAt(folder)
        val analysisStarted = CountDownLatch(1)
        val allowAnalysisToFinish = CountDownLatch(1)

        try {
            val first = MediaCollectionMaintenance.startScan(id, listOf("mp4"), emptyList()) {
                analysisStarted.countDown()
                if (!allowAnalysisToFinish.await(5, TimeUnit.SECONDS)) {
                    throw IllegalStateException("Timed out waiting to finish fake video analysis.")
                }
                1000L to 25f
            }
            assertTrue(first.state == ApiCollectionScanState.RUNNING || first.state == ApiCollectionScanState.DONE)
            assertTrue(analysisStarted.await(5, TimeUnit.SECONDS), "The first scan should reach video analysis")
            val error = assertThrows(IllegalStateException::class.java) {
                MediaCollectionMaintenance.startScan(id, listOf("mp4"), emptyList()) { 1000L to 25f }
            }
            assertTrue(error.message.orEmpty().contains("already running"))
            assertEquals(ApiCollectionScanState.RUNNING, MediaCollectionMaintenance.scanStatus(id).state)
        } finally {
            allowAnalysisToFinish.countDown()
        }

        var status = MediaCollectionMaintenance.scanStatus(id)
        for (i in 0 until 100) {
            if (status.state != ApiCollectionScanState.RUNNING) break
            Thread.sleep(20)
            status = MediaCollectionMaintenance.scanStatus(id)
        }
        assertEquals(ApiCollectionScanState.DONE, status.state)
    }

    @Test
    fun `scan ignores symbolic links that resolve outside the collection`(@TempDir folder: Path) {
        val outside = folder.resolveSibling("outside-${UUID.randomUUID()}.jpg")
        Files.writeString(outside, "outside")
        try {
            Files.createSymbolicLink(folder.resolve("outside-link.jpg"), outside)
        } catch (_: UnsupportedOperationException) {
            Files.deleteIfExists(outside)
            return
        } catch (_: java.io.IOException) {
            Files.deleteIfExists(outside)
            return
        }
        val id = collectionAt(folder)

        try {
            val result = MediaCollectionMaintenance.scan(id, emptyList(), listOf("jpg")) { }

            assertEquals(0, result.filesFound)
            assertTrue(itemNames(id).isEmpty())
        } finally {
            Files.deleteIfExists(outside)
        }
    }

    @Test
    fun `check rejects paths that escape the collection folder`(@TempDir folder: Path) {
        val outside = folder.resolveSibling("outside-${UUID.randomUUID()}.jpg")
        Files.writeString(outside, "outside")
        val id = collectionAt(folder)
        MediaCollectionManager.addMediaItem(ApiMediaItem("", "relative", ApiMediaType.IMAGE, id, "../${outside.fileName}"))
        MediaCollectionManager.addMediaItem(ApiMediaItem("", "absolute", ApiMediaType.IMAGE, id, outside.toString()))

        try {
            val result = MediaCollectionMaintenance.check(id)!!

            assertEquals(2, result.total)
            assertEquals(0, result.ok)
            assertEquals(2, result.issueCount)
            assertEquals(setOf("Invalid path outside collection folder.", "Invalid or absolute path."), result.issues.map { it.problem }.toSet())
        } finally {
            Files.deleteIfExists(outside)
        }
    }

    @Test
    fun `check rejects symlinks that resolve outside the collection folder`(@TempDir folder: Path) {
        val outside = folder.resolveSibling("outside-${UUID.randomUUID()}.jpg")
        Files.writeString(outside, "outside")
        val link = folder.resolve("outside-link.jpg")
        try {
            try {
                Files.createSymbolicLink(link, outside)
            } catch (_: UnsupportedOperationException) {
                return
            } catch (_: java.io.IOException) {
                return
            }
            val id = collectionAt(folder)
            MediaCollectionManager.addMediaItem(ApiMediaItem("", "outside-link", ApiMediaType.IMAGE, id, link.fileName.toString()))

            val result = MediaCollectionMaintenance.check(id)!!

            assertEquals(1, result.total)
            assertEquals(0, result.ok)
            assertEquals("File resolves outside collection folder.", result.issues.single().problem)
        } finally {
            Files.deleteIfExists(link)
            Files.deleteIfExists(outside)
        }
    }

    @Test
    fun `background scan finishes and reports its status`(@TempDir folder: Path) {
        Files.writeString(folder.resolve("one.jpg"), "x")
        val id = collectionAt(folder)
        assertEquals(ApiCollectionScanState.NONE, MediaCollectionMaintenance.scanStatus(id).state)

        MediaCollectionMaintenance.startScan(id, emptyList(), listOf("jpg"))
        var status = MediaCollectionMaintenance.scanStatus(id)
        for (i in 0 until 100) {
            if (status.state != ApiCollectionScanState.RUNNING) break
            Thread.sleep(50)
            status = MediaCollectionMaintenance.scanStatus(id)
        }
        assertEquals(ApiCollectionScanState.DONE, status.state)
        assertEquals(1, status.itemsAdded)
    }

    @Test
    fun `cancelled scan reaches terminal cancelled status and can be replaced`(@TempDir folder: Path) {
        val nested = Files.createDirectories(folder.resolve("nested"))
        repeat(2_000) { Files.writeString(nested.resolve("$it.jpg"), "x") }
        val id = collectionAt(folder)

        MediaCollectionMaintenance.startScan(id, emptyList(), listOf("jpg"))
        val beforeCancel = MediaCollectionMaintenance.scanStatus(id)
        assertTrue(beforeCancel.state == ApiCollectionScanState.RUNNING || beforeCancel.state == ApiCollectionScanState.DONE)
        val result = MediaCollectionMaintenance.cancelScan(id)
        assertTrue(result.state == ApiCollectionScanState.CANCELLING || result.state == ApiCollectionScanState.CANCELLED)

        var status = MediaCollectionMaintenance.scanStatus(id)
        for (i in 0 until 100) {
            if (status.state == ApiCollectionScanState.CANCELLED) break
            Thread.sleep(20)
            status = MediaCollectionMaintenance.scanStatus(id)
        }
        assertEquals(ApiCollectionScanState.CANCELLED, status.state)

        val replacementFolder = Files.createTempDirectory("collection-restart-")
        val replacementId = collectionAt(replacementFolder)
        val replacement = MediaCollectionMaintenance.startScan(replacementId, emptyList(), listOf("jpg"))
        assertTrue(replacement.state == ApiCollectionScanState.RUNNING || replacement.state == ApiCollectionScanState.DONE)
        var replacementStatus = MediaCollectionMaintenance.scanStatus(replacementId)
        for (i in 0 until 100) {
            if (replacementStatus.state != ApiCollectionScanState.RUNNING) break
            Thread.sleep(20)
            replacementStatus = MediaCollectionMaintenance.scanStatus(replacementId)
        }
        assertEquals(ApiCollectionScanState.DONE, replacementStatus.state)
        replacementFolder.toFile().deleteRecursively()
    }

    @Test
    fun `stop cancels running scans and init restarts the service`(@TempDir folder: Path) {
        val nested = Files.createDirectories(folder.resolve("nested"))
        repeat(2_000) { Files.writeString(nested.resolve("$it.jpg"), "x") }
        val id = collectionAt(folder)

        MediaCollectionMaintenance.startScan(id, emptyList(), listOf("jpg"))
        MediaCollectionMaintenance.stop()

        var status = MediaCollectionMaintenance.scanStatus(id)
        for (i in 0 until 100) {
            if (status.state == ApiCollectionScanState.CANCELLED) break
            Thread.sleep(20)
            status = MediaCollectionMaintenance.scanStatus(id)
        }
        assertEquals(ApiCollectionScanState.CANCELLED, status.state)

        assertThrows(IllegalStateException::class.java) {
            MediaCollectionMaintenance.startScan(id, emptyList(), listOf("jpg"))
        }
        val restartFolder = Files.createTempDirectory("collection-restart-")
        val restartId = collectionAt(restartFolder)
        MediaCollectionMaintenance.init(store) { Paths.get("/nonexistent-ffmpeg") }
        val restarted = MediaCollectionMaintenance.startScan(restartId, emptyList(), listOf("jpg"))
        assertTrue(restarted.state == ApiCollectionScanState.RUNNING || restarted.state == ApiCollectionScanState.DONE)
    }

    @Test
    fun `scan of a missing folder is rejected`() {
        val id = collectionAt(Paths.get("/nonexistent-${UUID.randomUUID()}"))
        assertThrows(IllegalArgumentException::class.java) { MediaCollectionMaintenance.scan(id, listOf("mp4"), emptyList()) { } }
    }

    @Test
    fun `check reports missing files`(@TempDir folder: Path) {
        Files.writeString(folder.resolve("a.jpg"), "x")
        Files.writeString(folder.resolve("b.jpg"), "x")
        val id = collectionAt(folder)
        MediaCollectionMaintenance.scan(id, emptyList(), listOf("jpg")) { }
        Files.delete(folder.resolve("b.jpg"))

        val result = MediaCollectionMaintenance.check(id)!!
        assertEquals(2, result.total)
        assertEquals(1, result.ok)
        assertEquals("b.jpg", result.issues.single().location)
    }

    @Test
    fun `export lists every item as CSV`(@TempDir folder: Path) {
        Files.writeString(folder.resolve("a.jpg"), "x")
        val id = collectionAt(folder)
        MediaCollectionMaintenance.scan(id, emptyList(), listOf("jpg")) { }

        val lines = MediaCollectionMaintenance.exportCsv(id)!!.trim().lines()
        assertEquals("itemType,name,location,duration,fps", lines[0])
        assertEquals("IMAGE,a,a.jpg,,", lines[1])
    }

    @Test
    fun `delete all keeps items that are used by a template and deletes the rest`() {
        val collection = createTestCollection()
        val template = createTemplateShell("maint-del-${UUID.randomUUID()}")
        val group = addTaskTypeAndGroup(template, "T", "G")
        val task = addTask(template, group, collection, "Find the video")
        val collectionId = store.transactional {
            task.targets.first().item = collection.items.filter { it.name eq "video1" }.first()
            collection.id
        }

        val result = MediaCollectionMaintenance.deleteItems(collectionId)!!
        assertEquals(1, result.deleted, "image1 is unused and deleted")
        assertEquals(1, result.blockedCount)
        assertTrue(result.blocked.single().problem.contains("Find the video"), result.blocked.single().problem)
        assertEquals(setOf("video1"), itemNames(collectionId))

        TemplateManager.deleteTemplate(store.transactional(true) { template.id })
        val second = MediaCollectionMaintenance.deleteItems(collectionId)!!
        assertEquals(1, second.deleted)
        assertEquals(0, second.blockedCount)
        assertTrue(itemNames(collectionId).isEmpty())
        store.transactional(true) { assertTrue(DbMediaItem.filter { it.name eq "video1" }.asSequence().none { it.collection.id == collectionId }) }
    }

    @Test
    fun `unknown collection returns null`() {
        val id = UUID.randomUUID().toString()
        assertNull(MediaCollectionMaintenance.check(id))
        assertNull(MediaCollectionMaintenance.exportCsv(id))
        assertNull(MediaCollectionMaintenance.deleteItems(id))
    }
}
