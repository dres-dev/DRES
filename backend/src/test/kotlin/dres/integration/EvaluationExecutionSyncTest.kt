package dres.integration

import dev.dres.api.rest.types.evaluation.ApiTaskStatus
import dev.dres.data.model.run.DbEvaluationType
import dev.dres.data.model.run.DbTask
import dev.dres.data.model.run.DbTaskStatus
import dev.dres.data.model.run.InteractiveSynchronousEvaluation
import dev.dres.data.model.template.task.options.DbScoreOption
import dev.dres.data.model.template.task.options.DbTargetOption
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

    private fun buildSyncEvaluation(taskCount: Int = 3, teamCount: Int = 2): InteractiveSynchronousEvaluation {
        val col = createTestCollection()
        val template = createTemplateShell("sync-${UUID.randomUUID()}", teamCount = teamCount)
        val group = addTaskTypeAndGroup(template, "KIS", "KIS", DbScoreOption.KIS, DbTargetOption.MEDIA_ITEM)
        repeat(taskCount) { i -> addTask(template, group, col, "Task-$i", idx = i) }
        val dbEval = createEvaluation(template, "sync-run-${UUID.randomUUID()}", DbEvaluationType.INTERACTIVE_SYNCHRONOUS)
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
        val template = store.transactional(true) { eval.getCurrentTaskTemplate() }

        store.transactional {
            val dbTask = DbTask.new {
                status = DbTaskStatus.CREATED
                evaluation = eval.dbEvaluation
                this.template = dev.dres.data.model.template.task.DbTaskTemplate.filter { it.id eq template.id!! }.first()
            }
            eval.ISTaskRun(dbTask)
        }

        assertEquals(1, eval.taskRuns.size)
    }

    @Test
    fun `ISTaskRun can be prepared and then ended`() {
        val eval = buildSyncEvaluation()
        store.transactional { eval.start() }
        val template = store.transactional(true) { eval.getCurrentTaskTemplate() }

        val taskRun = store.transactional {
            val dbTask = DbTask.new {
                status = DbTaskStatus.CREATED
                evaluation = eval.dbEvaluation
                this.template = dev.dres.data.model.template.task.DbTaskTemplate.filter { it.id eq template.id!! }.first()
            }
            eval.ISTaskRun(dbTask).also { it.prepare() }
        }

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
        val template0 = store.transactional(true) { eval.getCurrentTaskTemplate() }

        store.transactional {
            val dbTask = DbTask.new {
                status = DbTaskStatus.CREATED
                evaluation = eval.dbEvaluation
                this.template = dev.dres.data.model.template.task.DbTaskTemplate.filter { it.id eq template0.id!! }.first()
            }
            eval.ISTaskRun(dbTask)
        }

        // Navigate to second task
        eval.goTo(1)
        val template1 = store.transactional(true) { eval.getCurrentTaskTemplate() }

        assertThrows(IllegalStateException::class.java) {
            store.transactional {
                val dbTask2 = DbTask.new {
                    status = DbTaskStatus.CREATED
                    evaluation = eval.dbEvaluation
                    this.template = dev.dres.data.model.template.task.DbTaskTemplate.filter { it.id eq template1.id!! }.first()
                }
                eval.ISTaskRun(dbTask2)
            }
        }
    }

    @Test
    fun `task run can be repeated if previous one has ended`() {
        val eval = buildSyncEvaluation(taskCount = 1)
        store.transactional { eval.start() }
        val template = store.transactional(true) { eval.getCurrentTaskTemplate() }

        val firstRun = store.transactional {
            val dbTask = DbTask.new {
                status = DbTaskStatus.CREATED
                evaluation = eval.dbEvaluation
                this.template = dev.dres.data.model.template.task.DbTaskTemplate.filter { it.id eq template.id!! }.first()
            }
            eval.ISTaskRun(dbTask).also { it.end() }
        }
        assertTrue(firstRun.hasEnded)

        val secondRun = store.transactional {
            val dbTask2 = DbTask.new {
                status = DbTaskStatus.CREATED
                evaluation = eval.dbEvaluation
                this.template = dev.dres.data.model.template.task.DbTaskTemplate.filter { it.id eq template.id!! }.first()
            }
            eval.ISTaskRun(dbTask2)
        }
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
        val group = addTaskTypeAndGroup(template, "P-KIS", "P-KIS", DbScoreOption.KIS, DbTargetOption.MEDIA_ITEM, durationSeconds = null)
        addTask(template, group, col, "Perpetual-Task", durationSeconds = null)
        val dbEval = createEvaluation(template, "sync-perpetual-run-${UUID.randomUUID()}", DbEvaluationType.INTERACTIVE_SYNCHRONOUS)
        val eval = store.transactional { InteractiveSynchronousEvaluation(store, dbEval) }

        store.transactional { eval.start() }
        val tmpl = store.transactional(true) { eval.getCurrentTaskTemplate() }

        val taskRun = store.transactional {
            val dbTask = DbTask.new {
                status = DbTaskStatus.CREATED
                evaluation = eval.dbEvaluation
                this.template = dev.dres.data.model.template.task.DbTaskTemplate.filter { it.id eq tmpl.id!! }.first()
            }
            eval.ISTaskRun(dbTask)
        }

        assertNull(taskRun.duration, "Perpetual task run must have null duration")
    }
}
