package dres.run

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import dev.dres.api.rest.types.evaluation.submission.ApiClientSubmission
import dev.dres.api.rest.types.evaluation.websocket.ServerMessage
import dev.dres.api.rest.types.evaluation.websocket.ServerMessageType
import dev.dres.api.rest.types.template.ApiEvaluationTemplate
import dev.dres.api.rest.types.template.tasks.ApiTaskTemplate
import dev.dres.run.RunExecutor
import dev.dres.run.eventstream.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

/**
 * Unit tests for the WebSocket event handling in [RunExecutor].
 *
 * Covers:
 *  1. [RunExecutor.eventToMessage] — asserts the correct [ServerMessageType] and
 *     field values are produced for every handled [StreamEvent] subtype.
 *  2. [ServerMessage] JSON serialisation — round-trip and field correctness.
 *  3. Smoke tests — [RunExecutor.handleStreamEvent] does not throw for any event.
 */
class RunExecutorWebSocketTest {

    private val mapper = jacksonObjectMapper()

    private val runId  = "eval-001"
    private val taskId = "task-001"

    // ── eventToMessage mapping ────────────────────────────────────────────────

    @Test
    fun `RunStartEvent maps to COMPETITION_START`() {
        val msg = RunExecutor.eventToMessage(RunStartEvent(runId, minimalTemplate()))
        assertNotNull(msg)
        assertEquals(ServerMessageType.COMPETITION_START, msg!!.type)
        assertEquals(runId, msg.evaluationId)
        assertNull(msg.taskId)
    }

    @Test
    fun `RunEndEvent maps to COMPETITION_END`() {
        val msg = RunExecutor.eventToMessage(RunEndEvent(runId))
        assertNotNull(msg)
        assertEquals(ServerMessageType.COMPETITION_END, msg!!.type)
        assertEquals(runId, msg.evaluationId)
        assertNull(msg.taskId)
    }

    @Test
    fun `TaskStartEvent maps to TASK_START with correct taskId`() {
        val msg = RunExecutor.eventToMessage(TaskStartEvent(runId, taskId, minimalTaskTemplate()))
        assertNotNull(msg)
        assertEquals(ServerMessageType.TASK_START, msg!!.type)
        assertEquals(runId, msg.evaluationId)
        assertEquals(taskId, msg.taskId)
    }

    @Test
    fun `TaskEndEvent maps to TASK_END with correct taskId`() {
        val msg = RunExecutor.eventToMessage(TaskEndEvent(runId, taskId))
        assertNotNull(msg)
        assertEquals(ServerMessageType.TASK_END, msg!!.type)
        assertEquals(runId, msg.evaluationId)
        assertEquals(taskId, msg.taskId)
    }

    @Test
    fun `ScoreUpdateEvent maps to COMPETITION_UPDATE`() {
        val msg = RunExecutor.eventToMessage(ScoreUpdateEvent(runId, "scoreboard", emptyMap()))
        assertNotNull(msg)
        assertEquals(ServerMessageType.COMPETITION_UPDATE, msg!!.type)
        assertEquals(runId, msg.evaluationId)
    }

    @Test
    fun `SubmissionEvent maps to TASK_UPDATED`() {
        val msg = RunExecutor.eventToMessage(
            SubmissionEvent("session-1", runId, ApiClientSubmission(answerSets = emptyList()))
        )
        assertNotNull(msg)
        assertEquals(ServerMessageType.TASK_UPDATED, msg!!.type)
        assertEquals(runId, msg.evaluationId)
    }

    @Test
    fun `SubmissionEvent carries no full overview, only a scoped teamOverview`() {
        // No run manager is registered for runId in this test, and the fixture submission has no
        // teamId, so both end up null here -- this asserts the *shape* (overview unset, teamOverview
        // the only possible diff carrier) rather than relying on a live run manager.
        val msg = RunExecutor.eventToMessage(
            SubmissionEvent("session-1", runId, ApiClientSubmission(answerSets = emptyList()))
        )
        assertNotNull(msg)
        assertNull(msg!!.overview)
        assertNull(msg.teamOverview)
    }

    @Test
    fun `unhandled event type maps to null`() {
        val msg = RunExecutor.eventToMessage(InvalidRequestEvent("session-1", runId, "bad data"))
        assertNull(msg)
    }

    @Test
    fun `every handled event type carries the correct evaluationId`() {
        val events: List<StreamEvent> = listOf(
            RunStartEvent(runId, minimalTemplate()),
            RunEndEvent(runId),
            TaskStartEvent(runId, taskId, minimalTaskTemplate()),
            TaskEndEvent(runId, taskId),
            ScoreUpdateEvent(runId, "board", emptyMap()),
            SubmissionEvent("s", runId, ApiClientSubmission(answerSets = emptyList()))
        )
        events.forEach { event ->
            val msg = RunExecutor.eventToMessage(event)
            assertNotNull(msg, "Expected non-null message for ${event::class.simpleName}")
            assertEquals(runId, msg!!.evaluationId, "Wrong evaluationId for ${event::class.simpleName}")
        }
    }

    // ── ServerMessage JSON serialisation ──────────────────────────────────────

    @Test
    fun `ServerMessage round-trips through JSON`() {
        val original = ServerMessage(runId, ServerMessageType.TASK_START, taskId)
        val decoded: ServerMessage = mapper.readValue(mapper.writeValueAsString(original))

        assertEquals(original.evaluationId, decoded.evaluationId)
        assertEquals(original.type,         decoded.type)
        assertEquals(original.taskId,       decoded.taskId)
    }

    @Test
    fun `ServerMessage JSON contains evaluationId, type and timestamp`() {
        val tree = mapper.readTree(
            mapper.writeValueAsString(ServerMessage(runId, ServerMessageType.COMPETITION_END))
        )
        assertEquals(runId,             tree["evaluationId"].asText())
        assertEquals("COMPETITION_END", tree["type"].asText())
        assertNotNull(tree["timestamp"])
    }

    @Test
    fun `ServerMessage without taskId serialises taskId as null`() {
        val tree = mapper.readTree(
            mapper.writeValueAsString(ServerMessage(runId, ServerMessageType.COMPETITION_START))
        )
        assertTrue(tree["taskId"].isNull)
    }

    @Test
    fun `ServerMessage with taskId serialises taskId correctly`() {
        val tree = mapper.readTree(
            mapper.writeValueAsString(ServerMessage(runId, ServerMessageType.TASK_START, taskId))
        )
        assertEquals(taskId, tree["taskId"].asText())
    }

    @Test
    fun `all ServerMessageTypes serialise without error`() {
        ServerMessageType.entries.forEach { type ->
            assertDoesNotThrow("Failed for type $type") {
                mapper.writeValueAsString(ServerMessage(runId, type))
            }
        }
    }

    // ── handleStreamEvent smoke tests ─────────────────────────────────────────

    @Test
    fun `handleStreamEvent does not throw for any event type`() {
        val events: List<StreamEvent> = listOf(
            RunStartEvent(runId, minimalTemplate()),
            RunEndEvent(runId),
            TaskStartEvent(runId, taskId, minimalTaskTemplate()),
            TaskEndEvent(runId, taskId),
            ScoreUpdateEvent(runId, "board", emptyMap()),
            SubmissionEvent("s", runId, ApiClientSubmission(answerSets = emptyList())),
            InvalidRequestEvent("s", runId, "bad data")
        )
        events.forEach { event ->
            assertDoesNotThrow("handleStreamEvent threw for ${event::class.simpleName}") {
                RunExecutor.handleStreamEvent(event)
            }
        }
    }

    // ── Fixture helpers ───────────────────────────────────────────────────────

    private fun minimalTemplate() = ApiEvaluationTemplate(
        id          = runId,
        name        = "Test Evaluation",
        description = null,
        created     = null,
        modified    = null,
        taskTypes   = emptyList(),
        taskGroups  = emptyList(),
        tasks       = emptyList(),
        teams       = emptyList(),
        teamGroups  = emptyList(),
        judges      = emptyList(),
        viewers     = emptyList()
    )

    private fun minimalTaskTemplate() = ApiTaskTemplate(
        id           = taskId,
        name         = "Task 1",
        taskGroup    = "group-1",
        taskType     = "KIS",
        duration     = 300L,
        collectionId = "col-1",
        targets      = emptyList(),
        hints        = emptyList(),
        comment      = null
    )
}
