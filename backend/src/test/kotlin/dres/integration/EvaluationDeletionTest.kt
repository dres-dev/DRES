package dres.integration

import dev.dres.data.model.media.DbMediaItem
import dev.dres.data.model.run.DbEvaluation
import dev.dres.data.model.submissions.DbAnswer
import dev.dres.data.model.submissions.DbAnswerSet
import dev.dres.data.model.submissions.DbAnswerType
import dev.dres.data.model.submissions.DbSubmission
import dev.dres.data.model.submissions.DbVerdictStatus
import dev.dres.data.model.template.DbEvaluationTemplate
import dev.dres.mgmt.EvaluationManager
import dev.dres.mgmt.TemplateManager
import dev.dres.run.InteractiveRunManager
import dev.dres.run.RunExecutor
import kotlinx.dnq.query.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Covers safe cleanup of a stopped evaluation and its persistent references. */
class EvaluationDeletionTest : AbstractDresIntegrationTest() {

    @BeforeAll
    fun initManagers() {
        TemplateManager.init(store)
        EvaluationManager.init(store)
    }

    private data class Fixture(
        val templateId: String,
        val instanceId: String,
        val evaluationId: String,
        val itemId: String,
        val submissionIds: List<String>,
        val answerSetIds: List<String>
    )

    private fun buildFixture(): Fixture {
        val collection = createTestCollection()
        val template = createTemplateShell("eval-delete-${UUID.randomUUID()}")
        val group = addTaskTypeAndGroup(template, "KIS", "G", "KIS", "MEDIA_ITEM")
        val taskTemplate = addTask(template, group, collection, "T1")
        val itemId = store.transactional {
            val item = collection.items.filter { it.name eq "video1" }.first()
            taskTemplate.targets.first().item = item
            item.id
        }
        val evaluation = createEvaluation(template, "eval-delete-run-${UUID.randomUUID()}", "INTERACTIVE_SYNCHRONOUS")
        val instanceTaskId = store.transactional(true) { evaluation.template.tasks.first().id }
        val task = createPersistedTask(evaluation, instanceTaskId)
        val user = createTestUser()
        val answerSetIds = ArrayList<String>()
        val submissionIds = (0 until 2).map {
            store.transactional {
                val team = evaluation.template.teams.first()
                val submission = DbSubmission.new {
                    timestamp = System.currentTimeMillis()
                    this.team = team
                    this.user = user
                }
                val answerSet = DbAnswerSet.new {
                    this.submission = submission
                    status = DbVerdictStatus.filter { it.description eq "WRONG" }.first()
                    this.task = task
                }
                DbAnswer.new {
                    this.answerSet = answerSet
                    type = DbAnswerType.filter { it.description eq "ITEM" }.first()
                    item = DbMediaItem.filter { it.id eq itemId }.first()
                }
                answerSetIds.add(answerSet.id)
                submission.id
            }
        }
        return store.transactional(true) {
            Fixture(template.id, evaluation.template.id, evaluation.id, itemId, submissionIds, answerSetIds)
        }
    }

    private fun tryDeleteItem(itemId: String): Boolean = try {
        store.transactional { DbMediaItem.filter { it.id eq itemId }.first().delete() }
        true
    } catch (_: Exception) {
        false
    }

    @Test
    fun `deleting a stopped evaluation removes submissions and its template copy only`() {
        val fixture = buildFixture()

        val deleted = EvaluationManager.deleteEvaluation(fixture.evaluationId)

        assertNotNull(deleted)
        assertEquals(2, deleted!!.submissions)
        store.transactional(true) {
            assertTrue(DbEvaluation.filter { it.id eq fixture.evaluationId }.isEmpty)
            assertTrue(DbEvaluationTemplate.filter { it.id eq fixture.instanceId }.isEmpty)
            assertFalse(DbEvaluationTemplate.filter { it.id eq fixture.templateId }.isEmpty)
            fixture.submissionIds.forEach { id -> assertTrue(DbSubmission.filter { it.id eq id }.isEmpty) }
            fixture.answerSetIds.forEach { id -> assertTrue(DbAnswerSet.filter { it.id eq id }.isEmpty) }
        }
    }

    @Test
    fun `listed stopped evaluations include metadata and disappear after deletion`() {
        val fixture = buildFixture()

        val listed = EvaluationManager.listEnded().find { it.evaluationId == fixture.evaluationId }
        assertNotNull(listed)
        assertEquals(2, listed!!.submissionCount)
        assertEquals(1, listed.taskCount)
        assertEquals(0, listed.verdicts.correct)
        assertEquals(2, listed.verdicts.wrong)
        assertEquals(0, listed.verdicts.indeterminate)
        assertEquals(0, listed.verdicts.undecidable)
        assertEquals(2, EvaluationManager.verdictCounts(fixture.evaluationId).total)
        assertTrue(listed.name.startsWith("eval-delete-run-"))

        EvaluationManager.deleteEvaluation(fixture.evaluationId)
        assertNull(EvaluationManager.listEnded().find { it.evaluationId == fixture.evaluationId })
    }

    @Test
    fun `media item remains referenced until both evaluation copy and original template are removed`() {
        val fixture = buildFixture()

        assertFalse(tryDeleteItem(fixture.itemId))
        EvaluationManager.deleteEvaluation(fixture.evaluationId)
        assertFalse(tryDeleteItem(fixture.itemId))
        assertNotNull(TemplateManager.deleteTemplate(fixture.templateId))
        assertTrue(tryDeleteItem(fixture.itemId))
    }

    @Test
    fun `deleting an unknown evaluation returns null`() {
        assertNull(EvaluationManager.deleteEvaluation(UUID.randomUUID().toString()))
    }

    @Test
    fun `an active evaluation is not listed or deleted`() {
        val fixture = buildFixture()
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        val manager = java.lang.reflect.Proxy.newProxyInstance(
            InteractiveRunManager::class.java.classLoader,
            arrayOf(InteractiveRunManager::class.java)
        ) { _, method, _ ->
            when (method.name) {
                "getId" -> fixture.evaluationId
                "run" -> {
                    started.countDown()
                    release.await(10, TimeUnit.SECONDS)
                    null
                }
                else -> throw UnsupportedOperationException(method.name)
            }
        } as InteractiveRunManager
        RunExecutor.schedule(manager)
        try {
            assertTrue(started.await(5, TimeUnit.SECONDS))
            assertNull(EvaluationManager.listEnded().find { it.evaluationId == fixture.evaluationId })
            assertThrows(IllegalStateException::class.java) { EvaluationManager.deleteEvaluation(fixture.evaluationId) }
            store.transactional(true) {
                assertFalse(DbEvaluation.filter { it.id eq fixture.evaluationId }.isEmpty)
                assertFalse(DbEvaluationTemplate.filter { it.id eq fixture.instanceId }.isEmpty)
            }
        } finally {
            release.countDown()
        }
    }
}
