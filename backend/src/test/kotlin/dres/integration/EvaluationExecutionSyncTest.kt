package dres.integration

import dev.dres.api.rest.types.evaluation.ApiTaskStatus
import dev.dres.data.model.run.DbEvaluationType
import dev.dres.data.model.run.DbTask
import dev.dres.data.model.run.DbTaskStatus
import dev.dres.data.model.run.InteractiveSynchronousEvaluation
import dev.dres.data.model.template.task.DbTaskTemplate
import dev.dres.data.model.template.task.options.DbScoreOption
import dev.dres.data.model.template.task.options.DbTargetOption
import kotlinx.dnq.query.eq
import kotlinx.dnq.query.filter
import kotlinx.dnq.query.first
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.UUID

/**
 * Integration tests for SYNC (Interactive Synchronous) evaluation execution (issue #489).
 *
 * Tests the state machine of [InteractiveSynchronousEvaluation] — start, task lifecycle,
 * navigation (next / previous / goTo), and ending the evaluation.
 */
class EvaluationExecutionSyncTest : AbstractDresIntegrationTest() {

    /**
     * Creates a DbTask (committed), then creates an ISTaskRun in a separate transaction.
     * Mirrors the two-phase approach used by InteractiveSynchronousRunManager.startTask().
     */
    private fun createISTaskRun(
        eval: InteractiveSynchronousEvaluation,
        templateId: String,
        prepare: Boolean = false
    ): InteractiveSynchronousEvaluation.ISTaskRun {
        val dbTask = store.transactional {
            DbTask.new {
                status = DbTaskStatus.CREATED
                evaluation = eval.dbEvaluation
                this.template = DbTaskTemplate.filter { it.id eq templateId }.first()
            }
        }
        return store.transactional {
            eval.ISTaskRun(dbTask).also { if (prepare) it.prepare() }
        }
    }

    private fun buildSyncEvaluation(taskCount: Int = 3, teamCount: Int = 2): InteractiveSynchronousEvaluation {
        val col = createTestCollection()
        val template = createTemplateShell("sync-${UUID.randomUUID()}", teamCount = teamCount)
        val group = addTaskTypeAndGroup(template, "KIS", "KIS", "KIS", "MEDIA_ITEM")
        repeat(taskCount) { i -> addTask(template, group, col, "Task-$i", idx = i) }
        val dbEval = createEvaluation(template, "sync-run-${UUID.randomUUID()}", "INTERACTIVE_SYNCHRONOUS")
        return store.transactional { InteractiveSynchronousEvaluation(store, dbEval) }
    }

    // ── Lifecycle ──────────────────────────────────────────────────────────────

    @Test
    fun `fresh evaluation has not started and has not ended`() {
        val eval = buildSyncEvaluation()
        assertFalse(eval.hasStarted)
        assertFalse(eval.hasEnded)
        assertFalse(eval.isRunning)
    }

    @Test
    fun `start sets hasStarted to true`() {
        val eval = buildSyncEvaluation()
        store.transactional { eval.start() }
        assertTrue(eval.hasStarted)
        assertTrue(eval.isRunning)
        assertFalse(eval.hasEnded)
    }

    @Test
    fun `starting an already-started evaluation throws`() {
        val eval = buildSyncEvaluation()
        store.transactional { eval.start() }
        assertThrows(IllegalStateException::class.java) {
            store.transactional { eval.start() }
        }
    }

    @Test
    fun `end after start sets hasEnded to true`() {
        val eval = buildSyncEvaluation()
        store.transactional { eval.start() }
        store.transactional { eval.end() }
        assertTrue(eval.hasEnded)
        assertFalse(eval.isRunning)
    }

    @Test
    fun `end without prior start still ends the evaluation`() {
        val eval = buildSyncEvaluation()
        store.transactional { eval.end() }
        assertTrue(eval.hasEnded)
    }

    // ── Task index navigation ──────────────────────────────────────────────────

    @Test
    fun `templateIndex starts at zero`() {
        val eval = buildSyncEvaluation(taskCount = 3)
        assertEquals(0, eval.templateIndex)
    }

    @Test
    fun `goTo moves the template index`() {
        val eval = buildSyncEvaluation(taskCount = 3)
        eval.goTo(2)
        assertEquals(2, eval.templateIndex)
    }

    @Test
    fun `goTo below zero throws IndexOutOfBoundsException`() {
        val eval = buildSyncEvaluation(taskCount = 3)
        assertThrows(IndexOutOfBoundsException::class.java) { eval.goTo(-1) }
    }

    @Test
    fun `goTo beyond task list throws IndexOutOfBoundsException`() {
        val eval = buildSyncEvaluation(taskCount = 3)
        assertThrows(IndexOutOfBoundsException::class.java) { eval.goTo(3) }
    }

    @Test
    fun `getCurrentTaskTemplate returns task at current index`() {
        val eval = buildSyncEvaluation(taskCount = 3)
        val first = store.transactional(true) { eval.getCurrentTaskTemplate() }
        eval.goTo(2)
        val third = store.transactional(true) { eval.getCurrentTaskTemplate() }
        assertNotEquals(first.id, third.id)
    }

    @Test
    fun `goTo boundary — last index is accessible`() {
        val eval = buildSyncEvaluation(taskCount = 5)
        assertDoesNotThrow { eval.goTo(4) }
        assertEquals(4, eval.templateIndex)
    }

    // ── Task run lifecycle ─────────────────────────────────────────────────────

    @Test
    fun `no task runs exist on a fresh evaluation`() {
        val eval = buildSyncEvaluation()
        assertTrue(eval.taskRuns.isEmpty())
        assertNull(eval.currentTaskRun)
    }

    @Test
    fun `creating an ISTaskRun adds it to taskRuns`() {
        val eval = buildSyncEvaluation()
        store.transactional { eval.start() }
        val tmplId = store.transactional(true) { eval.getCurrentTaskTemplate().id!! }

        createISTaskRun(eval, tmplId)

        assertEquals(1, eval.taskRuns.size)
    }

    @Test
    fun `ISTaskRun can be prepared and then ended`() {
        val eval = buildSyncEvaluation()
        store.transactional { eval.start() }
        val tmplId = store.transactional(true) { eval.getCurrentTaskTemplate().id!! }

        val taskRun = createISTaskRun(eval, tmplId, prepare = true)

        assertEquals(ApiTaskStatus.PREPARING, taskRun.status)

        store.transactional { taskRun.start() }
        assertEquals(ApiTaskStatus.RUNNING, taskRun.status)

        store.transactional { taskRun.end() }
        assertEquals(ApiTaskStatus.ENDED, taskRun.status)
        assertTrue(taskRun.hasEnded)
    }

    @Test
    fun `creating second ISTaskRun while first is running throws`() {
        val eval = buildSyncEvaluation(taskCount = 2)
        store.transactional { eval.start() }
        val tmplId0 = store.transactional(true) { eval.getCurrentTaskTemplate().id!! }

        createISTaskRun(eval, tmplId0) // first run — not ended

        eval.goTo(1)
        val tmplId1 = store.transactional(true) { eval.getCurrentTaskTemplate().id!! }

        // Pre-create DbTask so the transaction in assertThrows only contains ISTaskRun creation
        val dbTask2 = store.transactional {
            DbTask.new {
                status = DbTaskStatus.CREATED
                evaluation = eval.dbEvaluation
                this.template = DbTaskTemplate.filter { it.id eq tmplId1 }.first()
            }
        }
        assertThrows(IllegalStateException::class.java) {
            store.transactional { eval.ISTaskRun(dbTask2) }
        }
    }

    @Test
    fun `task run can be repeated if previous one has ended`() {
        val eval = buildSyncEvaluation(taskCount = 1)
        store.transactional { eval.start() }
        val tmplId = store.transactional(true) { eval.getCurrentTaskTemplate().id!! }

        val firstRun = createISTaskRun(eval, tmplId)
        store.transactional { firstRun.end() }
        assertTrue(firstRun.hasEnded)

        val secondRun = createISTaskRun(eval, tmplId)
        assertEquals(2, eval.taskRuns.size)
        assertFalse(secondRun.hasEnded)
    }

    // ── Scoreboard ─────────────────────────────────────────────────────────────

    @Test
    fun `scoreboards are created for each task group`() {
        val eval = buildSyncEvaluation()
        assertEquals(1, eval.scoreboards.size)
        assertEquals("KIS", eval.scoreboards.first().name)
    }

    @Test
    fun `scoreboard covers all teams`() {
        val eval = buildSyncEvaluation(teamCount = 3)
        val scoreboard = eval.scoreboards.first()
        val scores = scoreboard.scores()
        assertEquals(3, scores.size)
    }

    // ── Perpetual task in sync evaluation ─────────────────────────────────────

    @Test
    fun `sync evaluation with perpetual task has null duration on task run`() {
        val col = createTestCollection()
        val template = createTemplateShell("sync-perpetual-${UUID.randomUUID()}")
        val group = addTaskTypeAndGroup(template, "P-KIS", "P-KIS", "KIS", "MEDIA_ITEM", durationSeconds = null)
        addTask(template, group, col, "Perpetual-Task", durationSeconds = null)
        val dbEval = createEvaluation(template, "sync-perpetual-run-${UUID.randomUUID()}", "INTERACTIVE_SYNCHRONOUS")
        val eval = store.transactional { InteractiveSynchronousEvaluation(store, dbEval) }

        store.transactional { eval.start() }
        val tmplId = store.transactional(true) { eval.getCurrentTaskTemplate().id!! }

        val taskRun = createISTaskRun(eval, tmplId)

        assertNull(taskRun.duration, "Perpetual task run must have null duration")
    }
}
