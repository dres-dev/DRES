package dres.integration

import dev.dres.data.model.run.DbEvaluationType
import dev.dres.data.model.run.InteractiveAsynchronousEvaluation
import dev.dres.data.model.template.task.options.DbScoreOption
import dev.dres.data.model.template.task.options.DbTargetOption
import kotlinx.dnq.query.asSequence
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.UUID

/**
 * Integration tests for ASYNC (Interactive Asynchronous) evaluation execution (issue #489).
 *
 * Tests per-team task navigation and independent task sequences in
 * [InteractiveAsynchronousEvaluation].
 */
class EvaluationExecutionAsyncTest : AbstractDresIntegrationTest() {

    private fun buildAsyncEvaluation(
        taskCount: Int = 4,
        teamCount: Int = 3
    ): Pair<InteractiveAsynchronousEvaluation, List<String>> {
        val col = createTestCollection()
        val template = createTemplateShell("async-${UUID.randomUUID()}", teamCount = teamCount)
        val group = addTaskTypeAndGroup(template, "KIS", "KIS", "KIS", "MEDIA_ITEM")
        repeat(taskCount) { i -> addTask(template, group, col, "Task-$i", idx = i) }
        val dbEval = createEvaluation(template, "async-run-${UUID.randomUUID()}", "INTERACTIVE_ASYNCHRONOUS")
        val eval = store.transactional { InteractiveAsynchronousEvaluation(store, dbEval) }
        val teamIds = store.transactional(true) { dbEval.template.teams.asSequence().map { it.teamId }.toList() }
        return eval to teamIds
    }

    // ── Lifecycle ──────────────────────────────────────────────────────────────

    @Test
    fun `fresh async evaluation has not started`() {
        val (eval, _) = buildAsyncEvaluation()
        assertFalse(eval.hasStarted)
        assertFalse(eval.hasEnded)
    }

    @Test
    fun `start sets hasStarted`() {
        val (eval, _) = buildAsyncEvaluation()
        store.transactional { eval.start() }
        assertTrue(eval.hasStarted)
        assertTrue(eval.isRunning)
    }

    @Test
    fun `end terminates the evaluation`() {
        val (eval, _) = buildAsyncEvaluation()
        store.transactional { eval.start() }
        store.transactional { eval.end() }
        assertTrue(eval.hasEnded)
        assertFalse(eval.isRunning)
    }

    // ── Per-team navigation ────────────────────────────────────────────────────

    @Test
    fun `each team starts at task index 0`() {
        val (eval, teamIds) = buildAsyncEvaluation(taskCount = 4, teamCount = 3)
        for (teamId in teamIds) {
            val template = store.transactional(true) { eval.currentTaskTemplate(teamId) }
            assertNotNull(template)
        }
    }

    @Test
    fun `goTo advances a team to the specified index`() {
        val (eval, teamIds) = buildAsyncEvaluation(taskCount = 4)
        val teamId = teamIds.first()
        val initial = store.transactional(true) { eval.currentTaskTemplate(teamId) }
        eval.goTo(teamId, 2)
        val afterGoTo = store.transactional(true) { eval.currentTaskTemplate(teamId) }
        assertNotEquals(initial.id, afterGoTo.id)
    }

    @Test
    fun `goTo for one team does not affect another team's position`() {
        val (eval, teamIds) = buildAsyncEvaluation(taskCount = 4, teamCount = 2)
        val team0 = teamIds[0]
        val team1 = teamIds[1]

        val team1Initial = store.transactional(true) { eval.currentTaskTemplate(team1) }
        eval.goTo(team0, 3) // advance team0 to last task
        val team1After = store.transactional(true) { eval.currentTaskTemplate(team1) }

        assertEquals(team1Initial.id, team1After.id, "Team 1 position must not change when Team 0 navigates")
    }

    @Test
    fun `different teams can be at different positions simultaneously`() {
        val (eval, teamIds) = buildAsyncEvaluation(taskCount = 4, teamCount = 3)
        eval.goTo(teamIds[0], 0)
        eval.goTo(teamIds[1], 1)
        eval.goTo(teamIds[2], 2)

        val t0 = store.transactional(true) { eval.currentTaskTemplate(teamIds[0]) }
        val t1 = store.transactional(true) { eval.currentTaskTemplate(teamIds[1]) }
        val t2 = store.transactional(true) { eval.currentTaskTemplate(teamIds[2]) }

        assertNotEquals(t0.id, t1.id)
        assertNotEquals(t1.id, t2.id)
        assertNotEquals(t0.id, t2.id)
    }

    @Test
    fun `unknown team ID throws`() {
        val (eval, _) = buildAsyncEvaluation()
        assertThrows(Exception::class.java) {
            store.transactional(true) { eval.currentTaskTemplate("unknown-team-xyz") }
        }
    }

    // ── Scoreboards ────────────────────────────────────────────────────────────

    @Test
    fun `scoreboards are initialised for each task group`() {
        val (eval, _) = buildAsyncEvaluation()
        assertEquals(1, eval.scoreboards.size)
    }

    @Test
    fun `scoreboard covers all teams`() {
        val (eval, teamIds) = buildAsyncEvaluation(teamCount = 3)
        val scores = eval.scoreboards.first().scores()
        assertEquals(3, scores.size)
    }

    // ── Permutation ────────────────────────────────────────────────────────────

    @Test
    fun `each team has a complete permutation covering all tasks`() {
        val taskCount = 5
        val (eval, teamIds) = buildAsyncEvaluation(taskCount = taskCount, teamCount = 3)

        val visitedIds = mutableSetOf<String?>()
        for (teamId in teamIds) {
            visitedIds.clear()
            for (i in 0 until taskCount) {
                eval.goTo(teamId, i)
                val tmpl = store.transactional(true) { eval.currentTaskTemplate(teamId) }
                visitedIds.add(tmpl.id)
            }
            assertEquals(taskCount, visitedIds.size, "Team $teamId must visit each task exactly once")
        }
    }

    @Test
    fun `async evaluation with perpetual tasks initialises correctly`() {
        val col = createTestCollection()
        val template = createTemplateShell("async-perpetual-${UUID.randomUUID()}", teamCount = 2)
        val group = addTaskTypeAndGroup(template, "P-KIS", "P-KIS", "KIS", "MEDIA_ITEM", durationSeconds = null)
        repeat(3) { i -> addTask(template, group, col, "Perpetual-$i", durationSeconds = null, idx = i) }
        val dbEval = createEvaluation(template, "async-perpetual-run-${UUID.randomUUID()}", "INTERACTIVE_ASYNCHRONOUS")
        val eval = store.transactional { InteractiveAsynchronousEvaluation(store, dbEval) }
        val teamIds = store.transactional(true) { dbEval.template.teams.asSequence().map { it.teamId }.toList() }

        store.transactional { eval.start() }
        for (teamId in teamIds) {
            val tmpl = store.transactional(true) { eval.currentTaskTemplate(teamId) }
            assertNotNull(tmpl)
            assertNull(tmpl.duration, "Perpetual task must have null duration in async evaluation")
        }
    }
}
