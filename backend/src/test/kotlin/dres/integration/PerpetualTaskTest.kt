package dres.integration

import dev.dres.DRES
import dev.dres.api.rest.types.ViewerInfo
import dev.dres.api.rest.types.evaluation.ApiTaskStatus
import dev.dres.api.rest.types.evaluation.submission.ApiAnswer
import dev.dres.api.rest.types.evaluation.submission.ApiAnswerSet
import dev.dres.api.rest.types.evaluation.submission.ApiAnswerType
import dev.dres.api.rest.types.evaluation.submission.ApiSubmission
import dev.dres.api.rest.types.evaluation.submission.ApiVerdictStatus
import dev.dres.api.rest.types.users.ApiRole
import dev.dres.data.model.config.Config
import dev.dres.data.model.run.InteractiveAsynchronousEvaluation
import dev.dres.data.model.run.InteractiveSynchronousEvaluation
import dev.dres.data.model.run.RunActionContext
import dev.dres.data.model.template.team.DbTeam
import dev.dres.data.model.template.team.TeamId
import dev.dres.mgmt.EvaluationManager
import dev.dres.mgmt.TemplateManager
import dev.dres.run.InteractiveAsynchronousRunManager
import dev.dres.run.InteractiveSynchronousRunManager
import dev.dres.run.RunManagerStatus
import dev.dres.run.score.Scoreable
import dev.dres.run.score.scorer.KisTaskScorer
import dev.dres.run.updatables.EndOnSubmitUpdatable
import dev.dres.run.updatables.Phase
import kotlinx.dnq.query.eq
import kotlinx.dnq.query.filter
import kotlinx.dnq.query.first
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.util.UUID

/**
 * Tests for perpetually running tasks (`duration == null`), the feature that exists mainly for testing and
 * demonstration runs.
 *
 * A perpetual task has no deadline, so it can only end by an explicit abort or by reaching a configured
 * `LIMIT_CORRECT_PER_TEAM` submission limit. Both paths used to carry duration guards written for timed tasks,
 * which silently made a perpetual task impossible to finish and a running perpetual asynchronous task
 * impossible to skip.
 */
class PerpetualTaskTest : AbstractDresIntegrationTest() {

    // ── Fixtures ─────────────────────────────────────────────────────────────

    /**
     * Builds a synchronous run manager for a perpetual or timed task.
     *
     * [configure] runs before the manager is constructed, because a [dev.dres.run.RunManager] snapshots the
     * template: options added afterwards are invisible to its updatables.
     */
    private fun buildSyncManager(
        durationSeconds: Long?,
        teamCount: Int = 1,
        configure: (dev.dres.data.model.template.task.DbTaskGroup) -> Unit = {}
    ): InteractiveSynchronousRunManager {
        TemplateManager.init(store)
        EvaluationManager.init(store)
        val col = createTestCollection()
        val template = createTemplateShell("perp-sync-${UUID.randomUUID()}", teamCount = teamCount)
        val group = addTaskTypeAndGroup(template, "P-KIS", "P-KIS", "KIS", "MEDIA_ITEM", durationSeconds = durationSeconds)
        addTask(template, group, col, "Perpetual", durationSeconds = durationSeconds)
        configure(group)
        val dbEval = createEvaluation(template, "perp-sync-run-${UUID.randomUUID()}", "INTERACTIVE_SYNCHRONOUS")
        val evaluation = store.transactional { InteractiveSynchronousEvaluation(store, dbEval) }
        return store.transactional(true) { InteractiveSynchronousRunManager(evaluation, store) }
    }

    /**
     * Builds an asynchronous run manager plus a participant context whose user is a member of the first team.
     *
     * [InteractiveAsynchronousRunManager] resolves the acting team from the team's user list, and it snapshots
     * that list when it is constructed. The membership therefore has to be created before the manager exists.
     */
    private fun buildAsyncManager(
        durationSeconds: Long?,
        teamCount: Int = 1,
        taskCount: Int = 2
    ): Pair<InteractiveAsynchronousRunManager, RunActionContext> {
        TemplateManager.init(store)
        EvaluationManager.init(store)
        val col = createTestCollection()
        val template = createTemplateShell("perp-async-${UUID.randomUUID()}", teamCount = teamCount)
        val group = addTaskTypeAndGroup(template, "P-KIS", "P-KIS", "KIS", "MEDIA_ITEM", durationSeconds = durationSeconds)
        repeat(taskCount) { i -> addTask(template, group, col, "Perpetual-$i", durationSeconds = durationSeconds, idx = i) }

        /* Membership must exist before the manager snapshots the template. */
        val user = createTestUser()
        val userId = store.transactional {
            template.teams.first().users.add(user)
            user.id!!
        }

        val dbEval = createEvaluation(template, "perp-async-run-${UUID.randomUUID()}", "INTERACTIVE_ASYNCHRONOUS")
        val evaluation = store.transactional { InteractiveAsynchronousEvaluation(store, dbEval) }
        val manager = store.transactional(true) { InteractiveAsynchronousRunManager(evaluation, store) }
        return manager to RunActionContext(userId, null, setOf(ApiRole.PARTICIPANT))
    }

    // ── End on submit ────────────────────────────────────────────────────────

    /**
     * Regression: [EndOnSubmitUpdatable] wrapped its entire body in `duration != null`, so a perpetual task
     * could never be ended by reaching the correct-submission limit - the only mechanism it has.
     */
    @Test
    fun `end on submit ends a perpetual task once the limit is reached`() {
        val manager = buildSyncManager(durationSeconds = null) { group ->
            configureSubmissionOption(group, "LIMIT_CORRECT_PER_TEAM", mapOf("limit" to "1"))
        }

        manager.start(RunActionContext.INTERNAL)
        manager.startTask(RunActionContext.INTERNAL)
        val taskTemplateId = store.transactional(true) { manager.currentTaskTemplate(RunActionContext.INTERNAL).id!! }
        manager.viewerReady(taskTemplateId, RunActionContext.INTERNAL, ViewerInfo("perp-viewer", "localhost"))
        /* Without the run loop, the PREPARING -> RUNNING transition has to be driven explicitly. */
        store.transactional { manager.currentTask(RunActionContext.INTERNAL)!!.start() }

        val task = store.transactional(true) { manager.currentTask(RunActionContext.INTERNAL) }
        assertNotNull(task)
        assertNull(task!!.duration, "Fixture must describe a perpetual task")
        assertEquals(ApiTaskStatus.RUNNING, task.status, "Task must be running for the updatable to act")
        assertNull(manager.timeLeft(RunActionContext.INTERNAL), "A perpetual task must report no remaining time")

        /* Register one correct submission for the only team, which reaches the configured limit. */
        val dbTaskId = store.transactional(true) { task.taskId }
        val dbTask = store.transactional(true) { dev.dres.data.model.run.DbTask.filter { it.id eq dbTaskId }.first() }
        val team = store.transactional(true) { manager.evaluation.dbEvaluation.template.teams.first() }
        val user = createTestUser()
        createSubmission(dbTask, team, user, "CORRECT")

        val updatable = EndOnSubmitUpdatable(manager)
        assertEquals(Phase.MAIN, updatable.phase)
        assertTrue(
            updatable.shouldBeUpdated(RunManagerStatus.ACTIVE, ApiTaskStatus.RUNNING),
            "EndOnSubmitUpdatable must consider a running perpetual task"
        )
        store.transactional { updatable.update(RunManagerStatus.ACTIVE, ApiTaskStatus.RUNNING) }

        assertEquals(
            ApiTaskStatus.ENDED,
            store.transactional(true) { manager.currentTask(RunActionContext.INTERNAL)?.status },
            "Reaching the submission limit must end a perpetual task"
        )
    }

    /** A perpetual task without submissions must keep running, so the guard does not end it prematurely. */
    @Test
    fun `end on submit keeps a perpetual task without submissions running`() {
        val manager = buildSyncManager(durationSeconds = null) { group ->
            configureSubmissionOption(group, "LIMIT_CORRECT_PER_TEAM", mapOf("limit" to "1"))
        }

        manager.start(RunActionContext.INTERNAL)
        manager.startTask(RunActionContext.INTERNAL)
        store.transactional { manager.currentTask(RunActionContext.INTERNAL)!!.start() }

        store.transactional { EndOnSubmitUpdatable(manager).update(RunManagerStatus.ACTIVE, ApiTaskStatus.RUNNING) }

        assertNotEquals(
            ApiTaskStatus.ENDED,
            store.transactional(true) { manager.currentTask(RunActionContext.INTERNAL)?.status },
            "A perpetual task without submissions must keep running"
        )
    }

    // ── Scoring ──────────────────────────────────────────────────────────────

    /**
     * A perpetual task has no time fraction; [KisTaskScorer] must award the full speed bonus instead of
     * failing on the missing duration.
     */
    @Test
    fun `kis scorer treats a perpetual task as solved at full speed`() {
        val started = System.currentTimeMillis() - 60_000
        val teamId = "team-perpetual"
        val scoreable = object : Scoreable {
            override val taskId: String = "perpetual-${UUID.randomUUID()}"
            override val teams: List<TeamId> = listOf(teamId)
            override val duration: Long? = null
            override val started: Long? = started
            override val ended: Long? = null
        }
        val submission = ApiSubmission(
            submissionId = UUID.randomUUID().toString(),
            teamId = teamId,
            memberId = "member",
            teamName = "team",
            memberName = "member",
            timestamp = started + 1_000,
            answers = listOf(
                ApiAnswerSet(
                    UUID.randomUUID().toString(),
                    ApiVerdictStatus.CORRECT,
                    scoreable.taskId,
                    listOf(ApiAnswer(ApiAnswerType.TEXT, null, "answer"))
                )
            )
        )

        val scores = KisTaskScorer(
            scoreable,
            maxPointsPerTask = 100.0,
            maxPointsAtTaskEnd = 50.0,
            penaltyPerWrongSubmission = 10.0,
            store = null
        ).calculateScores(sequenceOf(submission))

        assertEquals(100.0, scores[teamId], "Perpetual tasks must award the full speed bonus")
    }

    // ── Asynchronous skip ────────────────────────────────────────────────────

    /**
     * Regression: `InteractiveAsynchronousRunManager.goTo` rejected every move while a task was running, which
     * turned a running perpetual task into a dead end. Skipping must end it instead.
     */
    @Test
    fun `async perpetual task can be skipped by its team`() {
        val (manager, rac) = buildAsyncManager(durationSeconds = null)
        /* `start` only acts for admins, so the run is started on behalf of the system. */
        manager.start(RunActionContext.INTERNAL)
        manager.startTask(rac)
        /* Without the run loop, the PREPARING -> RUNNING transition has to be driven explicitly. */
        store.transactional { manager.currentTask(rac)!!.start() }

        assertEquals(ApiTaskStatus.RUNNING, store.transactional(true) { manager.currentTask(rac)?.status })
        val runningTaskId = store.transactional(true) { manager.currentTask(rac)?.taskId }
        val firstTemplateId = store.transactional(true) { manager.currentTaskTemplate(rac).id }
        assertNull(manager.timeLeft(rac), "A perpetual task must not report a remaining time")

        assertTrue(manager.next(rac), "A running perpetual task must be skippable")

        val skipped = store.transactional(true) { manager.tasks(rac).firstOrNull { it.taskId == runningTaskId } }
        assertEquals(ApiTaskStatus.ENDED, skipped?.status, "Skipping must end the perpetual task run")
        assertNotEquals(
            firstTemplateId,
            store.transactional(true) { manager.currentTaskTemplate(rac).id },
            "Skipping must select the next task template"
        )
    }

    /** A timed asynchronous task must still refuse to be skipped while it runs. */
    @Test
    fun `async timed task cannot be skipped while running`() {
        val (manager, rac) = buildAsyncManager(durationSeconds = 300L)
        manager.start(RunActionContext.INTERNAL)
        manager.startTask(rac)
        /* The task must actually be running, otherwise skipping is not blocked but simply unnecessary. */
        store.transactional { manager.currentTask(rac)!!.start() }

        assertThrows(IllegalArgumentException::class.java) { manager.next(rac) }
        assertEquals(ApiTaskStatus.RUNNING, store.transactional(true) { manager.currentTask(rac)?.status })
    }

    /** Adjusting the duration of a perpetual task is meaningless and must fail with a helpful message. */
    @Test
    fun `async perpetual task has no adjustable duration`() {
        val (manager, rac) = buildAsyncManager(durationSeconds = null)
        /* `start` only acts for admins, so the run is started on behalf of the system. */
        manager.start(RunActionContext.INTERNAL)
        manager.startTask(rac)
        /* Without the run loop, the PREPARING -> RUNNING transition has to be driven explicitly. */
        store.transactional { manager.currentTask(rac)!!.start() }

        val error = assertThrows(IllegalStateException::class.java) { manager.adjustDuration(rac, 10) }
        assertTrue(
            error.message!!.contains("perpetual", ignoreCase = true),
            "The rejection must explain the task is perpetual: ${error.message}"
        )
    }

    companion object {
        init {
            /* AuditLogger writes through EventStreamProcessor, which resolves DRES.CONFIG on first touch. */
            val events = Files.createTempDirectory("dres-perpetual-events-")
            val audit = Files.createTempDirectory("dres-perpetual-audit-")
            DRES.CONFIG = Config(eventsLocation = events, auditLocation = audit)
            DRES.AUDIT_LOG_ROOT = audit
        }
    }
}
