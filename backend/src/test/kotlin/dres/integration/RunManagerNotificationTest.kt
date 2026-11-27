package dres.integration

import dev.dres.DRES
import dev.dres.api.rest.types.evaluation.ApiTaskStatus
import dev.dres.api.rest.types.users.ApiRole
import dev.dres.data.model.config.Config
import dev.dres.data.model.run.InteractiveAsynchronousEvaluation
import dev.dres.data.model.run.InteractiveSynchronousEvaluation
import dev.dres.data.model.run.RunActionContext
import dev.dres.mgmt.EvaluationManager
import dev.dres.mgmt.TemplateManager
import dev.dres.run.InteractiveAsynchronousRunManager
import dev.dres.run.InteractiveSynchronousRunManager
import dev.dres.run.RunExecutor
import dev.dres.run.eventstream.EventStreamProcessor
import dev.dres.run.eventstream.StreamEvent
import dev.dres.run.eventstream.StreamEventHandler
import dev.dres.run.eventstream.TaskEndEvent
import dev.dres.run.eventstream.TaskStartEvent
import kotlinx.dnq.query.asSequence
import kotlinx.dnq.query.first
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Tests that a task transition is *reported*, not just persisted.
 *
 * `AuditLogger.taskEnd` / `AuditLogger.taskStart` are the only producers of `TaskEndEvent` /
 * `TaskStartEvent`, and `RunExecutor.eventToMessage` turns those into the `TASK_END` / `TASK_START`
 * WebSocket messages. Any code path that ends or starts a task without going through the audit logger is
 * therefore invisible to every connected viewer - which is exactly what happened for asynchronous aborts
 * and for tasks that start or end by themselves.
 */
class RunManagerNotificationTest : AbstractDresIntegrationTest() {

    /** Captures the events the managers emit, so tests can assert on what observers would receive. */
    private object Capture : StreamEventHandler {
        val events = ConcurrentLinkedQueue<StreamEvent>()

        override fun handleStreamEvent(event: StreamEvent) {
            this.events.add(event)
        }

        fun clear() = this.events.clear()

        inline fun <reified T : StreamEvent> ofRun(runId: String): List<T> =
            this.events.filterIsInstance<T>().filter { it.runIdOf() == runId }

        /** Every concrete event in this package exposes `runId`; the sealed base class does not. */
        private fun StreamEvent.runIdOf(): String? = when (this) {
            is TaskStartEvent -> this.runId
            is TaskEndEvent -> this.runId
            else -> null
        }

        fun <T> await(timeoutMs: Long = 10_000, condition: () -> T?): T? {
            val deadline = System.currentTimeMillis() + timeoutMs
            while (System.currentTimeMillis() < deadline) {
                condition()?.let { return it }
                Thread.sleep(25)
            }
            return condition()
        }
    }

    // ── Fixtures ─────────────────────────────────────────────────────────────

    private fun buildAsyncManager(durationSeconds: Long?): Pair<InteractiveAsynchronousRunManager, RunActionContext> {
        TemplateManager.init(store)
        EvaluationManager.init(store)
        val col = createTestCollection()
        val template = createTemplateShell("notify-async-${UUID.randomUUID()}", teamCount = 1)
        val group = addTaskTypeAndGroup(template, "N-KIS", "N-KIS", "KIS", "MEDIA_ITEM", durationSeconds = durationSeconds)
        repeat(2) { i -> addTask(template, group, col, "Task-$i", durationSeconds = durationSeconds, idx = i) }

        val user = createTestUser()
        val userId = store.transactional {
            template.teams.first().users.add(user)
            user.id!!
        }
        val dbEval = createEvaluation(template, "notify-async-run-${UUID.randomUUID()}", "INTERACTIVE_ASYNCHRONOUS")
        val evaluation = store.transactional { InteractiveAsynchronousEvaluation(store, dbEval) }
        val manager = store.transactional(true) { InteractiveAsynchronousRunManager(evaluation, store) }
        return manager to RunActionContext(userId, null, setOf(ApiRole.PARTICIPANT))
    }

    private fun buildSyncManager(durationSeconds: Long?): InteractiveSynchronousRunManager {
        TemplateManager.init(store)
        EvaluationManager.init(store)
        val col = createTestCollection()
        val template = createTemplateShell("notify-sync-${UUID.randomUUID()}", teamCount = 1)
        val group = addTaskTypeAndGroup(template, "N-KIS", "N-KIS", "KIS", "MEDIA_ITEM", durationSeconds = durationSeconds)
        addTask(template, group, col, "Task-0", durationSeconds = durationSeconds, idx = 0)
        val dbEval = createEvaluation(template, "notify-sync-run-${UUID.randomUUID()}", "INTERACTIVE_SYNCHRONOUS")
        val evaluation = store.transactional { InteractiveSynchronousEvaluation(store, dbEval) }
        return store.transactional(true) { InteractiveSynchronousRunManager(evaluation, store) }
    }

    // ── Tests ────────────────────────────────────────────────────────────────

    /**
     * Regression: `abortTask` ended the task run and only wrote a log line, so a stopped task kept being
     * displayed as running by every observer.
     */
    @Test
    fun `an aborted async task is reported to observers`() {
        val (manager, rac) = this.buildAsyncManager(durationSeconds = 300L)
        manager.start(RunActionContext.INTERNAL)
        manager.startTask(rac)
        store.transactional { manager.currentTask(rac)!!.start() }
        val taskId = store.transactional(true) { manager.currentTask(rac)!!.taskId }
        Capture.clear()

        manager.abortTask(rac)

        assertEquals(ApiTaskStatus.ENDED, store.transactional(true) { manager.currentTask(rac)?.status })
        val end = Capture.await { Capture.ofRun<TaskEndEvent>(manager.id).firstOrNull { it.taskId == taskId } }
        assertTrue(end != null, "An aborted task must emit a TaskEndEvent, otherwise no viewer learns about it")
    }

    /** The end of a timed async task by itself must be reported just like an aborted one. */
    @Test
    fun `a self-ending async task is reported to observers`() {
        /* A duration of zero makes the task fall due on the first tick of the run loop. */
        val (manager, rac) = this.buildAsyncManager(durationSeconds = 0L)
        manager.start(RunActionContext.INTERNAL)
        manager.startTask(rac)
        Capture.clear()
        RunExecutor.schedule(manager)
        try {
            val taskId = store.transactional(true) { manager.currentTask(rac)?.taskId }
            val end = Capture.await { Capture.ofRun<TaskEndEvent>(manager.id).firstOrNull { it.taskId == taskId } }
            assertTrue(end != null, "A task ending by timeout must emit a TaskEndEvent")
        } finally {
            runCatching { manager.end(RunActionContext.INTERNAL) }
        }
    }

    /**
     * Regression: the audit entry for a self-starting async task carried the team id in the task-id field, so
     * the resulting `TaskStartEvent` named a task nobody could match to a template.
     */
    @Test
    fun `a self-starting async task is reported with its own task id`() {
        val (manager, rac) = this.buildAsyncManager(durationSeconds = 300L)
        manager.start(RunActionContext.INTERNAL)
        /* startTask opens its own transaction; wrapping it in another one detaches the created run. */
        val taskId = manager.startTask(rac)
        val teamId = store.transactional(true) { manager.currentTask(rac)!!.teamId }
        assertNotEquals(taskId, teamId, "The fixture must use different ids for task runs and teams")
        Capture.clear()
        try {
            RunExecutor.schedule(manager)

            val start = Capture.await { Capture.ofRun<TaskStartEvent>(manager.id).firstOrNull() }
            assertTrue(start != null, "A task that starts by itself must emit a TaskStartEvent")
            assertEquals(taskId, start!!.taskId, "The event must name the task run, not the team")
        } finally {
            runCatching { manager.end(RunActionContext.INTERNAL) }
        }
    }

    /**
     * Regression: `timeLeft` compared `currentTaskRun?.duration == null`, which is also true when there is no
     * task run at all, so "nothing is running" was reported as "perpetual".
     */
    @Test
    fun `time left is minus one when no task run exists`() {
        val (async, rac) = this.buildAsyncManager(durationSeconds = 300L)
        async.start(RunActionContext.INTERNAL)
        assertEquals(
            -1L,
            async.timeLeft(rac),
            "Without a task run there is no perpetual task - the documented answer is -1L"
        )

        val sync = this.buildSyncManager(durationSeconds = 300L)
        sync.start(RunActionContext.INTERNAL)
        assertEquals(-1L, sync.timeLeft(RunActionContext.INTERNAL))
    }

    /** A genuine perpetual task still reports no deadline (null), which the -1L change must not shadow. */
    @Test
    fun `time left is null for a running perpetual task`() {
        val (manager, rac) = this.buildAsyncManager(durationSeconds = null)
        manager.start(RunActionContext.INTERNAL)
        manager.startTask(rac)
        store.transactional { manager.currentTask(rac)!!.start() }

        assertEquals(null, manager.timeLeft(rac))
    }

    companion object {
        init {
            val events = Files.createTempDirectory("dres-notify-events-")
            val audit = Files.createTempDirectory("dres-notify-audit-")
            DRES.CONFIG = Config(eventsLocation = events, auditLocation = audit)
            DRES.AUDIT_LOG_ROOT = audit
            /* The events only reach the capture handler once the processor thread runs. */
            EventStreamProcessor.register(Capture)
            EventStreamProcessor.init()
        }
    }
}
