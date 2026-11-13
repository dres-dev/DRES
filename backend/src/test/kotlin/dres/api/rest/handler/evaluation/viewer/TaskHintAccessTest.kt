package dres.api.rest.handler.evaluation.viewer

import dev.dres.api.rest.handler.evaluation.viewer.isTaskHintAccessAllowed
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TaskHintAccessTest {

    @Test
    fun `participant cannot access hint before task run exists`() {
        assertFalse(isTaskHintAccessAllowed(isParticipant = true, isJudge = false, taskRunExists = false))
    }

    @Test
    fun `judge cannot access hint before task run exists`() {
        assertFalse(isTaskHintAccessAllowed(isParticipant = false, isJudge = true, taskRunExists = false))
    }

    @Test
    fun `participants and judges can access hint once task run exists`() {
        assertTrue(isTaskHintAccessAllowed(isParticipant = true, isJudge = false, taskRunExists = true))
        assertTrue(isTaskHintAccessAllowed(isParticipant = false, isJudge = true, taskRunExists = true))
    }

    @Test
    fun `other roles retain existing hint access behavior`() {
        assertTrue(isTaskHintAccessAllowed(isParticipant = false, isJudge = false, taskRunExists = false))
    }
}
