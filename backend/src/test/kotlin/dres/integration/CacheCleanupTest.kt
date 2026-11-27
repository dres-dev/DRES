package dres.integration

import dev.dres.DRES
import dev.dres.data.model.config.Config
import dev.dres.data.model.media.DbMediaItem
import dev.dres.data.model.run.DbEvaluation
import dev.dres.data.model.run.DbEvaluationStatus
import dev.dres.data.model.template.task.DbHint
import dev.dres.data.model.template.task.DbHintType
import dev.dres.data.model.template.task.DbTargetType
import dev.dres.data.model.template.task.DbTaskTemplateTarget
import dev.dres.mgmt.cache.CacheManager
import kotlinx.dnq.query.asSequence
import kotlinx.dnq.query.eq
import kotlinx.dnq.query.filter
import kotlinx.dnq.query.first
import kotlinx.dnq.query.toList
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.FileTime
import java.util.UUID

/**
 * `cleanup()` protects the previews of active runs and deletes the rest. It built those names without the
 * file extension the renderer actually uses, so the blacklist never matched anything and every preview older
 * than five minutes was deleted - including previews of runs that were still going, which then had to be
 * re-rendered mid-task.
 */
class CacheCleanupTest : AbstractDresIntegrationTest() {

    companion object {
        private lateinit var cacheRoot: Path

        @BeforeAll
        @JvmStatic
        fun initCache() {
            cacheRoot = Files.createTempDirectory("dres-cleanup-cache-")
            DRES.CONFIG = Config(
                eventsLocation = Files.createTempDirectory("dres-cleanup-events-"),
                auditLocation = Files.createTempDirectory("dres-cleanup-audit-")
            )
            /* CacheConfig.ffmpegPath() resolves against DRES.DATA_ROOT, which CacheManager's constructor reads. */
            DRES.DATA_ROOT = Files.createTempDirectory("dres-cleanup-data-")
        }
    }

    /** Ages [path] past cleanup's five-minute threshold. */
    private fun makeStale(path: Path) {
        Files.writeString(path, "preview")
        Files.setLastModifiedTime(path, FileTime.fromMillis(System.currentTimeMillis() - 3_600_000L))
    }

    private fun cacheManager(): CacheManager {
        DRES.CONFIG = DRES.CONFIG.copy(cache = DRES.CONFIG.cache.copy(cachePath = cacheRoot))
        return CacheManager(DRES.CONFIG, store)
    }

    /**
     * A hint and a target of an ACTIVE run must survive cleanup; an unrelated stale file must not.
     */
    @Test
    fun `cleanup keeps previews of active runs and deletes the rest`() {
        val collection = createTestCollection()
        val template = createTemplateShell("cleanup-template-${UUID.randomUUID()}")
        val group = addTaskTypeAndGroup(template, "cleanup-type-${UUID.randomUUID()}".take(30), "cleanup-group")
        val task = addTask(template, group, collection, "cleanup-task")

        /*
         * Hints and targets are added to the TEMPLATE before the evaluation is created: an evaluation works on
         * a copy of the template taken at creation time (`DbEvaluationTemplate.toInstance()`), so anything
         * added afterwards would never reach the run that cleanup inspects.
         */
        val ids = store.transactional {
            val items = DbMediaItem.all().asSequence().take(2).toList()
            val activeItem = items[0]
            val targetItem = items[1]

            task.hints.add(DbHint.new {
                this.type = DbHintType.filter { it.description eq "VIDEO" }.first()
                this.task = task
                this.item = activeItem
                this.start = 0L
                this.end = 10_000L
                this.temporalRangeStart = 0L
                this.temporalRangeEnd = 10_000L
            })

            task.targets.add(DbTaskTemplateTarget.new {
                this.type = DbTargetType.MEDIA_ITEM_TEMPORAL_RANGE
                this.task = task
                this.item = targetItem
                this.start = 0L
                this.end = 5_000L
            })

            activeItem.mediaItemId!! to targetItem.mediaItemId!!
        }
        val activeItemId = ids.first
        val targetItemId = ids.second

        val evaluation = createEvaluation(template, "cleanup-eval-${UUID.randomUUID()}", "INTERACTIVE_SYNCHRONOUS")
        store.transactional {
            evaluation.status = DbEvaluationStatus.filter { it.description eq "ACTIVE" }.first()
        }

        val hintedPreview = cacheRoot.resolve("${activeItemId}_0-10000.mp4")
        val targetedPreview = cacheRoot.resolve("${targetItemId}_0-5000.mp4")
        val staleUnrelated = cacheRoot.resolve("not-referenced-anywhere_0-1000.mp4")
        listOf(hintedPreview, targetedPreview, staleUnrelated).forEach { makeStale(it) }

        cacheManager().cleanup()

        assertTrue(Files.exists(hintedPreview), "The preview of an active run's hint was deleted.")
        assertTrue(Files.exists(targetedPreview), "The preview of an active run's target was deleted.")
        assertFalse(Files.exists(staleUnrelated), "A preview nothing references should have been deleted.")
    }

    /**
     * A preview of a run that is no longer active is fair game: the point of the blacklist is to protect
     * active runs, not every file a finished run once produced.
     */
    @Test
    fun `cleanup deletes previews of a finished run`() {
        val collection = createTestCollection()
        val template = createTemplateShell("finished-template-${UUID.randomUUID()}")
        val group = addTaskTypeAndGroup(template, "finished-type-${UUID.randomUUID()}".take(30), "finished-group")
        val task = addTask(template, group, collection, "finished-task")

        /* Added before the evaluation copies the template; see the test above. */
        val itemId = store.transactional {
            val item = DbMediaItem.all().asSequence().first()
            task.hints.add(DbHint.new {
                this.type = DbHintType.filter { it.description eq "VIDEO" }.first()
                this.task = task
                this.item = item
                this.start = 0L
                this.end = 2_000L
                this.temporalRangeStart = 0L
                this.temporalRangeEnd = 2_000L
            })
            item.mediaItemId!!
        }

        val evaluation = createEvaluation(template, "finished-eval-${UUID.randomUUID()}", "INTERACTIVE_SYNCHRONOUS")
        store.transactional {
            evaluation.status = DbEvaluationStatus.filter { it.description eq "TERMINATED" }.first()
        }

        val preview = cacheRoot.resolve("${itemId}_0-2000.mp4")
        makeStale(preview)

        cacheManager().cleanup()

        assertFalse(Files.exists(preview), "A finished run's preview should be reclaimable.")
    }
}
