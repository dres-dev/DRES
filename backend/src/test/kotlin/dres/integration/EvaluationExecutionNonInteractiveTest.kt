package dres.integration

import dev.dres.data.model.run.DbEvaluation
import dev.dres.data.model.run.DbEvaluationType
import dev.dres.data.model.run.DbTask
import dev.dres.data.model.run.DbTaskStatus
import dev.dres.data.model.run.NonInteractiveEvaluation
import dev.dres.data.model.template.task.DbTaskTemplate
import kotlinx.dnq.query.asSequence
import kotlinx.dnq.query.eq
import kotlinx.dnq.query.filter
import kotlinx.dnq.query.first
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.UUID

/**
 * Integration tests for NON_INTERACTIVE evaluation execution (issue #489).
 *
 * [NonInteractiveEvaluation] is the batch/LSC mode: all task runs are pre-created from
 * the template at construction time rather than started on-demand.
 */
class EvaluationExecutionNonInteractiveTest : AbstractDresIntegrationTest() {

    /**
     * Builds a NON_INTERACTIVE evaluation with [taskCount] pre-created DbTask entries.
     * Uses AVS scorer since KIS is explicitly disallowed in non-interactive mode.
     */
    private fun buildNonInteractiveEvaluation(
        taskCount: Int = 3,
        teamCount: Int = 2
    ): NonInteractiveEvaluation {
        val col = createTestCollection()
        val template = createTemplateShell("ni-${UUID.randomUUID()}", teamCount = teamCount)
        val group = addTaskTypeAndGroup(template, "AVS", "AVS", "AVS", "JUDGEMENT")
        repeat(taskCount) { i -> addTask(template, group, col, "Task-$i", idx = i) }

        // Transaction 1: create evaluation instance + DbTask entries
        val dbEval = store.transactional {
            val type = DbEvaluationType.filter { it.description eq "NON_INTERACTIVE" }.first()
            val instance = template.toInstance()
            val evaluation = DbEvaluation.new {
                this.name = "ni-run-${UUID.randomUUID()}"
                this.type = type
                this.template = instance
            }
            // Pre-create DbTask for every task template in the instance
            instance.tasks.asSequence().forEach { tmpl ->
                DbTask.new {
                    status = DbTaskStatus.CREATED
                    this.evaluation = evaluation
                    this.template = tmpl
                }
            }
            evaluation
        }

        // Transaction 2: build the evaluation object (reads existing tasks)
        return store.transactional { NonInteractiveEvaluation(store, dbEval) }
    }

    // ── Lifecycle ──────────────────────────────────────────────────────────────

    @Test
    fun `fresh non-interactive evaluation has not started`() {
        val eval = buildNonInteractiveEvaluation()
        assertFalse(eval.hasStarted)
        assertFalse(eval.hasEnded)
        assertFalse(eval.isRunning)
    }

    @Test
    fun `start sets hasStarted`() {
        val eval = buildNonInteractiveEvaluation()
        store.transactional { eval.start() }
        assertTrue(eval.hasStarted)
        assertTrue(eval.isRunning)
    }

    @Test
    fun `end after start terminates the evaluation`() {
        val eval = buildNonInteractiveEvaluation()
        store.transactional { eval.start() }
        store.transactional { eval.end() }
        assertTrue(eval.hasEnded)
        assertFalse(eval.isRunning)
    }

    @Test
    fun `starting twice throws`() {
        val eval = buildNonInteractiveEvaluation()
        store.transactional { eval.start() }
        assertThrows(IllegalStateException::class.java) {
            store.transactional { eval.start() }
        }
    }

    // ── Task runs ──────────────────────────────────────────────────────────────

    @Test
    fun `task runs are pre-created for all task templates`() {
        val taskCount = 4
        val eval = buildNonInteractiveEvaluation(taskCount = taskCount)
        assertEquals(taskCount, eval.taskRuns.size)
    }

    @Test
    fun `each NITaskRun has duration zero`() {
        val eval = buildNonInteractiveEvaluation(taskCount = 2)
        for (run in eval.taskRuns) {
            assertEquals(0L, run.duration, "NITaskRun duration must always be 0")
        }
    }

    @Test
    fun `each NITaskRun covers all teams`() {
        val teamCount = 3
        val eval = buildNonInteractiveEvaluation(teamCount = teamCount)
        for (run in eval.taskRuns) {
            assertEquals(teamCount, run.teams.size)
        }
    }

    @Test
    fun `NITaskRun positions are sequential from zero`() {
        val taskCount = 4
        val eval = buildNonInteractiveEvaluation(taskCount = taskCount)
        val positions = eval.taskRuns.map { it.position }
        assertEquals((0 until taskCount).toList(), positions.sorted())
    }

    @Test
    fun `task run has not started and not ended initially`() {
        val eval = buildNonInteractiveEvaluation(taskCount = 1)
        val run = eval.taskRuns.first()
        assertFalse(run.hasStarted)
        assertFalse(run.hasEnded)
    }

    @Test
    fun `NITaskRun can be started and ended`() {
        val eval = buildNonInteractiveEvaluation(taskCount = 1)
        val run = eval.taskRuns.first()
        store.transactional { run.start() }
        assertTrue(run.hasStarted)
        store.transactional { run.end() }
        assertTrue(run.hasEnded)
    }

    // ── Scoreboards ────────────────────────────────────────────────────────────

    @Test
    fun `scoreboards are created for each task group`() {
        val eval = buildNonInteractiveEvaluation()
        assertEquals(1, eval.scoreboards.size)
        assertEquals("AVS", eval.scoreboards.first().name)
    }

    @Test
    fun `scoreboard tracks all teams`() {
        val teamCount = 3
        val eval = buildNonInteractiveEvaluation(teamCount = teamCount)
        assertEquals(teamCount, eval.scoreboards.first().scores().size)
    }

    // ── KIS scorer rejected ────────────────────────────────────────────────────

    @Test
    fun `non-interactive evaluation with KIS scorer throws at construction`() {
        val col = createTestCollection()
        val template = createTemplateShell("ni-kis-${UUID.randomUUID()}")
        val group = addTaskTypeAndGroup(template, "KIS", "KIS", "KIS", "MEDIA_SEGMENT")
        addTask(template, group, col, "KIS-Task")

        val dbEval = store.transactional {
            val type = DbEvaluationType.filter { it.description eq "NON_INTERACTIVE" }.first()
            val instance = template.toInstance()
            val evaluation = DbEvaluation.new {
                this.name = "ni-kis-run-${UUID.randomUUID()}"
                this.type = type
                this.template = instance
            }
            instance.tasks.asSequence().forEach { tmpl ->
                DbTask.new {
                    status = DbTaskStatus.CREATED
                    this.evaluation = evaluation
                    this.template = tmpl
                }
            }
            evaluation
        }

        assertThrows(IllegalStateException::class.java) {
            store.transactional { NonInteractiveEvaluation(store, dbEval) }
        }
    }

    // ── Exotic: many tasks ─────────────────────────────────────────────────────

    @Test
    fun `non-interactive evaluation with 20 tasks creates 20 task runs`() {
        val eval = buildNonInteractiveEvaluation(taskCount = 20, teamCount = 2)
        assertEquals(20, eval.taskRuns.size)
    }

    @Test
    fun `non-interactive evaluation with LEGACY_AVS scorer constructs correctly`() {
        val col = createTestCollection()
        val template = createTemplateShell("ni-lavs-${UUID.randomUUID()}")
        val group = addTaskTypeAndGroup(template, "LAVS", "LAVS", "LEGACY_AVS", "JUDGEMENT")
        addTask(template, group, col, "LAVS-Task")

        val dbEval = store.transactional {
            val type = DbEvaluationType.filter { it.description eq "NON_INTERACTIVE" }.first()
            val instance = template.toInstance()
            val evaluation = DbEvaluation.new {
                this.name = "ni-lavs-run-${UUID.randomUUID()}"
                this.type = type
                this.template = instance
            }
            instance.tasks.asSequence().forEach { tmpl ->
                DbTask.new {
                    status = DbTaskStatus.CREATED
                    this.evaluation = evaluation
                    this.template = tmpl
                }
            }
            evaluation
        }

        val eval = store.transactional { NonInteractiveEvaluation(store, dbEval) }
        assertEquals(1, eval.taskRuns.size)
    }
}
