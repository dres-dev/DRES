package dres.integration

import dev.dres.run.filter.*
import kotlinx.dnq.query.asSequence
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.UUID

/**
 * Integration tests for the DB-backed submission filters (issue #489, robustness of submission endpoint).
 *
 * These filters query the live Xodus store for existing verdicts, so they cannot be tested
 * as plain unit tests and require the full entity store to be running.
 */
class DbBackedSubmissionFilterTest : AbstractDresIntegrationTest() {

    // ── Shared fixture helpers ─────────────────────────────────────────────────

    /** Creates a one-task evaluation and returns the task alongside team and user. */
    private fun createFilterFixture(suffix: String = UUID.randomUUID().toString()): Triple<
            dev.dres.data.model.run.DbTask,
            dev.dres.data.model.template.team.DbTeam,
            dev.dres.data.model.admin.DbUser
            > {
        val col = createTestCollection()
        val template = createTemplateShell("filter-$suffix", teamCount = 1)
        val group = addTaskTypeAndGroup(template, "KIS", "KIS", "KIS", "MEDIA_ITEM")
        addTask(template, group, col, "Filter-Task-$suffix")
        val dbEval = createEvaluation(template, "filter-eval-$suffix", "INTERACTIVE_SYNCHRONOUS")
        val tmplId = store.transactional(true) {
            dbEval.template.tasks.asSequence().first().id
        }
        val task = createPersistedTask(dbEval, tmplId)
        val team = store.transactional(true) { dbEval.template.teams.asSequence().first() }
        val user = createTestUser(suffix.take(8))
        return Triple(task, team, user)
    }

    // ── MaximumCorrectPerTeamFilter ────────────────────────────────────────────

    @Test
    fun `MaximumCorrectPerTeamFilter accepts first submission when no correct ones exist`() {
        val (task, team, user) = createFilterFixture()
        val submission = clientSubmission(task, team, user)
        store.transactional {
            val filter = MaximumCorrectPerTeamFilter(1)
            assertTrue(filter.test(submission))
        }
    }

    @Test
    fun `MaximumCorrectPerTeamFilter rejects when correct limit is reached`() {
        val (task, team, user) = createFilterFixture()
        createSubmission(task, team, user, "CORRECT")
        val submission = clientSubmission(task, team, user)
        store.transactional {
            val filter = MaximumCorrectPerTeamFilter(1)
            assertFalse(filter.test(submission))
        }
    }

    @Test
    fun `MaximumCorrectPerTeamFilter accepts when correct count is below limit`() {
        val (task, team, user) = createFilterFixture()
        createSubmission(task, team, user, "CORRECT")
        val submission = clientSubmission(task, team, user)
        store.transactional {
            val filter = MaximumCorrectPerTeamFilter(2)
            assertTrue(filter.test(submission))
        }
    }

    @Test
    fun `MaximumCorrectPerTeamFilter is not affected by WRONG verdicts`() {
        val (task, team, user) = createFilterFixture()
        repeat(5) { createSubmission(task, team, user, "WRONG") }
        val submission = clientSubmission(task, team, user)
        store.transactional {
            val filter = MaximumCorrectPerTeamFilter(1)
            assertTrue(filter.test(submission))
        }
    }

    // ── MaximumWrongPerTeamFilter ──────────────────────────────────────────────

    @Test
    fun `MaximumWrongPerTeamFilter accepts first submission when no wrong ones exist`() {
        val (task, team, user) = createFilterFixture()
        val submission = clientSubmission(task, team, user)
        store.transactional {
            val filter = MaximumWrongPerTeamFilter(3)
            assertTrue(filter.test(submission))
        }
    }

    @Test
    fun `MaximumWrongPerTeamFilter rejects when wrong limit is reached`() {
        val (task, team, user) = createFilterFixture()
        repeat(3) { createSubmission(task, team, user, "WRONG") }
        val submission = clientSubmission(task, team, user)
        store.transactional {
            val filter = MaximumWrongPerTeamFilter(3)
            assertFalse(filter.test(submission))
        }
    }

    @Test
    fun `MaximumWrongPerTeamFilter accepts when wrong count is below limit`() {
        val (task, team, user) = createFilterFixture()
        repeat(2) { createSubmission(task, team, user, "WRONG") }
        val submission = clientSubmission(task, team, user)
        store.transactional {
            val filter = MaximumWrongPerTeamFilter(3)
            assertTrue(filter.test(submission))
        }
    }

    @Test
    fun `MaximumWrongPerTeamFilter is not affected by CORRECT verdicts`() {
        val (task, team, user) = createFilterFixture()
        repeat(5) { createSubmission(task, team, user, "CORRECT") }
        val submission = clientSubmission(task, team, user)
        store.transactional {
            val filter = MaximumWrongPerTeamFilter(1)
            assertTrue(filter.test(submission))
        }
    }

    // ── MaximumTotalPerTeamFilter ──────────────────────────────────────────────

    @Test
    fun `MaximumTotalPerTeamFilter accepts first submission`() {
        val (task, team, user) = createFilterFixture()
        val submission = clientSubmission(task, team, user)
        store.transactional {
            val filter = MaximumTotalPerTeamFilter(5)
            assertTrue(filter.test(submission))
        }
    }

    @Test
    fun `MaximumTotalPerTeamFilter rejects when total limit is reached`() {
        val (task, team, user) = createFilterFixture()
        repeat(5) { createSubmission(task, team, user, "WRONG") }
        val submission = clientSubmission(task, team, user)
        store.transactional {
            val filter = MaximumTotalPerTeamFilter(5)
            assertFalse(filter.test(submission))
        }
    }

    @Test
    fun `MaximumTotalPerTeamFilter counts both correct and wrong submissions`() {
        val (task, team, user) = createFilterFixture()
        repeat(2) { createSubmission(task, team, user, "CORRECT") }
        repeat(2) { createSubmission(task, team, user, "WRONG") }
        val submission = clientSubmission(task, team, user)
        store.transactional {
            val filter = MaximumTotalPerTeamFilter(4)
            assertFalse(filter.test(submission))
        }
    }

    @Test
    fun `MaximumTotalPerTeamFilter accepts when total count is below limit`() {
        val (task, team, user) = createFilterFixture()
        repeat(3) { createSubmission(task, team, user, "WRONG") }
        val submission = clientSubmission(task, team, user)
        store.transactional {
            val filter = MaximumTotalPerTeamFilter(5)
            assertTrue(filter.test(submission))
        }
    }

    // ── MaximumCorrectPerTeamMemberFilter ──────────────────────────────────────

    @Test
    fun `MaximumCorrectPerTeamMemberFilter accepts first submission from member`() {
        val (task, team, user) = createFilterFixture()
        val submission = clientSubmission(task, team, user)
        store.transactional {
            val filter = MaximumCorrectPerTeamMemberFilter(1)
            assertTrue(filter.test(submission))
        }
    }

    @Test
    fun `MaximumCorrectPerTeamMemberFilter rejects when member correct limit is reached`() {
        val (task, team, user) = createFilterFixture()
        createSubmission(task, team, user, "CORRECT")
        val submission = clientSubmission(task, team, user)
        store.transactional {
            val filter = MaximumCorrectPerTeamMemberFilter(1)
            assertFalse(filter.test(submission))
        }
    }

    @Test
    fun `MaximumCorrectPerTeamMemberFilter tracks per user not per team`() {
        val (task, team, user1) = createFilterFixture()
        val user2 = createTestUser()
        createSubmission(task, team, user1, "CORRECT") // user1 at limit

        val sub2 = clientSubmission(task, team, user2)
        store.transactional {
            val filter = MaximumCorrectPerTeamMemberFilter(1)
            assertTrue(filter.test(sub2), "User2 should not be blocked by user1's correct submission")
        }
    }

    @Test
    fun `MaximumCorrectPerTeamMemberFilter allows second member correct if limit is higher`() {
        val (task, team, user) = createFilterFixture()
        createSubmission(task, team, user, "CORRECT")
        val submission = clientSubmission(task, team, user)
        store.transactional {
            val filter = MaximumCorrectPerTeamMemberFilter(2)
            assertTrue(filter.test(submission))
        }
    }

    // ── DuplicateSubmissionFilter ──────────────────────────────────────────────

    @Test
    fun `DuplicateSubmissionFilter accepts submission when no previous submissions exist`() {
        val (task, team, user) = createFilterFixture()
        val submission = clientSubmission(task, team, user)
        store.transactional {
            val filter = DuplicateSubmissionFilter()
            assertTrue(filter.test(submission))
        }
    }
}
