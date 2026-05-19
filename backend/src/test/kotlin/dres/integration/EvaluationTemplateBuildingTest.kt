package dres.integration

import dev.dres.data.model.template.DbEvaluationTemplate
import dev.dres.data.model.run.DbEvaluation
import dev.dres.data.model.run.DbEvaluationType
import dev.dres.data.model.run.InteractiveSynchronousEvaluation
import dev.dres.data.model.template.task.DbTaskGroup
import dev.dres.data.model.template.task.DbTaskTemplate
import dev.dres.data.model.template.task.DbTaskTemplateTarget
import dev.dres.data.model.template.task.DbTargetType
import dev.dres.data.model.template.task.options.DbScoreOption
import dev.dres.data.model.template.task.options.DbTargetOption
import dev.dres.data.model.template.team.DbTeam
import kotlinx.dnq.query.asSequence
import kotlinx.dnq.query.size
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.UUID

/**
 * Integration tests for Evaluation Template Building (issue #489).
 *
 * Covers VBS-style, LSC-style, perpetual task, and exotic / edge-case templates,
 * as well as attempts to stress the builder with invalid inputs.
 */
class EvaluationTemplateBuildingTest : AbstractDresIntegrationTest() {

    // ── VBS-style template ─────────────────────────────────────────────────────

    @Test
    fun `VBS KIS task type can be created with temporal target and KIS scorer`() {
        val col = createTestCollection()
        val template = createTemplateShell("vbs-kis-${UUID.randomUUID()}")
        val group = addTaskTypeAndGroup(
            template, "KIS", "KIS-Group",
            scoreOption = "KIS",
            targetOption = "MEDIA_SEGMENT",
            durationSeconds = 300L
        )
        val task = addTask(template, group, col, "KIS Task 1")

        store.transactional(true) {
            assertEquals("KIS", group.type.name)
            assertEquals("KIS", group.type.score.description)
            assertEquals("MEDIA_SEGMENT", group.type.target.description)
            assertEquals(300L, group.type.duration)
            assertEquals(300L, task.duration)
        }
    }

    @Test
    fun `VBS AVS task type can be created with media-item target and AVS scorer`() {
        val col = createTestCollection()
        val template = createTemplateShell("vbs-avs-${UUID.randomUUID()}")
        val group = addTaskTypeAndGroup(
            template, "AVS", "AVS-Group",
            scoreOption = "AVS",
            targetOption = "JUDGEMENT",
            durationSeconds = 300L
        )
        addTask(template, group, col, "AVS Task 1")

        store.transactional(true) {
            assertEquals("AVS", group.type.score.description)
            assertEquals(1, template.tasks.size())
        }
    }

    @Test
    fun `VBS-style template with both KIS and AVS task groups can be instantiated`() {
        val col = createTestCollection()
        val name = "vbs-full-${UUID.randomUUID()}"
        val template = createTemplateShell(name, teamCount = 3)
        val kisGroup = addTaskTypeAndGroup(template, "KIS", "KIS", "KIS", "MEDIA_SEGMENT")
        val avsGroup = addTaskTypeAndGroup(template, "AVS", "AVS", "AVS", "JUDGEMENT")
        repeat(3) { addTask(template, kisGroup, col, "KIS-$it", idx = it) }
        repeat(2) { addTask(template, avsGroup, col, "AVS-$it", idx = it + 3) }

        store.transactional {
            assertEquals(5, template.tasks.size())
            assertEquals(3, template.teams.size())
            val instance = template.toInstance()
            assertTrue(instance.instance)
            assertEquals(5, instance.tasks.size())
            assertEquals(3, instance.teams.size())
        }
    }

    @Test
    fun `VBS-style template toApi round-trips correctly`() {
        val col = createTestCollection()
        val name = "vbs-api-${UUID.randomUUID()}"
        val template = createTemplateShell(name, teamCount = 2)
        val group = addTaskTypeAndGroup(template, "KIS", "KIS", "KIS", "MEDIA_SEGMENT")
        addTask(template, group, col, "Task A")

        store.transactional(true) {
            val api = template.toApi()
            assertEquals(name, api.name)
            assertEquals(1, api.taskTypes.size)
            assertEquals(1, api.taskGroups.size)
            assertEquals(2, api.teams.size)
            assertEquals(1, api.tasks.size)
        }
    }

    // ── LSC-style template ─────────────────────────────────────────────────────

    @Test
    fun `LSC-style KIS template uses MEDIA_ITEM target and KIS scorer`() {
        val col = createTestCollection()
        val template = createTemplateShell("lsc-kis-${UUID.randomUUID()}", teamCount = 10)
        val group = addTaskTypeAndGroup(
            template, "LKIS", "LKIS-Group",
            scoreOption = "KIS",
            targetOption = "MEDIA_ITEM",
            durationSeconds = 420L
        )
        repeat(5) { addTask(template, group, col, "LKIS-Task-$it", idx = it) }

        store.transactional(true) {
            assertEquals("MEDIA_ITEM", group.type.target.description)
            assertEquals(10, template.teams.size())
            assertEquals(5, template.tasks.size())
        }
    }

    @Test
    fun `LSC-style template can be instantiated and instance is separate from template`() {
        val col = createTestCollection()
        val name = "lsc-instance-${UUID.randomUUID()}"
        val template = createTemplateShell(name, teamCount = 4)
        val group = addTaskTypeAndGroup(template, "LKIS", "LKIS", "KIS", "MEDIA_ITEM")
        addTask(template, group, col, "LKIS-1")

        store.transactional {
            val instance = template.toInstance()
            assertNotEquals(template.id, instance.id)
            assertTrue(instance.instance)
            assertFalse(template.instance)
        }
    }

    // ── Perpetual tasks ────────────────────────────────────────────────────────

    @Test
    fun `task with null duration represents a perpetual task`() {
        val col = createTestCollection()
        val template = createTemplateShell("perpetual-${UUID.randomUUID()}")
        val group = addTaskTypeAndGroup(
            template, "Perpetual-KIS", "P-KIS",
            scoreOption = "KIS",
            targetOption = "MEDIA_SEGMENT",
            durationSeconds = null
        )
        val task = addTask(template, group, col, "Perpetual Task", durationSeconds = null)

        store.transactional(true) {
            assertNull(group.type.duration, "Task type duration should be null for perpetual task")
            assertNull(task.duration, "Task template duration should be null for perpetual task")
        }
    }

    @Test
    fun `template with mix of timed and perpetual tasks is valid`() {
        val col = createTestCollection()
        val template = createTemplateShell("mixed-perpetual-${UUID.randomUUID()}")
        val timedGroup = addTaskTypeAndGroup(template, "KIS", "KIS", "KIS", "MEDIA_SEGMENT", durationSeconds = 300L)
        val perpetualGroup = addTaskTypeAndGroup(template, "P-KIS", "P-KIS", "KIS", "MEDIA_SEGMENT", durationSeconds = null)
        val timedTask = addTask(template, timedGroup, col, "Timed-1", durationSeconds = 300L)
        val perpetualTask = addTask(template, perpetualGroup, col, "Perpetual-1", durationSeconds = null)

        store.transactional(true) {
            assertEquals(300L, timedTask.duration)
            assertNull(perpetualTask.duration)
            assertEquals(2, template.tasks.size())
        }
    }

    @Test
    fun `perpetual template can be instantiated`() {
        val col = createTestCollection()
        val template = createTemplateShell("perpetual-instance-${UUID.randomUUID()}", teamCount = 2)
        val group = addTaskTypeAndGroup(template, "P-KIS", "P-KIS", "KIS", "MEDIA_SEGMENT", durationSeconds = null)
        addTask(template, group, col, "Perpetual Task", durationSeconds = null)

        store.transactional {
            val instance = template.toInstance()
            assertTrue(instance.instance)
            val instanceTask = instance.tasks.asSequence().first()
            assertNull(instanceTask.duration, "Perpetual task duration must be preserved in instance")
        }
    }

    // ── Exotic templates ───────────────────────────────────────────────────────

    @Test
    fun `template with many teams (20) can be created and instantiated`() {
        val col = createTestCollection()
        val template = createTemplateShell("large-team-${UUID.randomUUID()}", teamCount = 20)
        val group = addTaskTypeAndGroup(template, "KIS", "KIS", "KIS", "MEDIA_SEGMENT")
        addTask(template, group, col, "Task-1")

        store.transactional {
            assertEquals(20, template.teams.size())
            val instance = template.toInstance()
            assertEquals(20, instance.teams.size())
        }
    }

    @Test
    fun `template with many tasks (50) can be created and instantiated`() {
        val col = createTestCollection()
        val template = createTemplateShell("many-tasks-${UUID.randomUUID()}")
        val group = addTaskTypeAndGroup(template, "KIS", "KIS", "KIS", "MEDIA_SEGMENT")
        repeat(50) { i -> addTask(template, group, col, "Task-$i", idx = i) }

        store.transactional {
            assertEquals(50, template.tasks.size())
            val instance = template.toInstance()
            assertEquals(50, instance.tasks.size())
        }
    }

    @Test
    fun `template with multiple task groups and types can be instantiated`() {
        val col = createTestCollection()
        val template = createTemplateShell("multi-group-${UUID.randomUUID()}", teamCount = 2)
        val g1 = addTaskTypeAndGroup(template, "KIS", "KIS", "KIS", "MEDIA_SEGMENT")
        val g2 = addTaskTypeAndGroup(template, "AVS", "AVS", "AVS", "JUDGEMENT")
        val g3 = addTaskTypeAndGroup(template, "LKIS", "LKIS", "KIS", "MEDIA_ITEM")
        addTask(template, g1, col, "KIS-1")
        addTask(template, g2, col, "AVS-1")
        addTask(template, g3, col, "LKIS-1")

        store.transactional {
            assertEquals(3, template.taskTypes.size())
            assertEquals(3, template.taskGroups.size())
            val instance = template.toInstance()
            assertEquals(3, instance.taskTypes.size())
            assertEquals(3, instance.taskGroups.size())
            assertEquals(3, instance.tasks.size())
        }
    }

    @Test
    fun `template with LEGACY_AVS scorer can be created`() {
        val col = createTestCollection()
        val template = createTemplateShell("legacy-avs-${UUID.randomUUID()}")
        val group = addTaskTypeAndGroup(template, "LEGACY_AVS", "L-AVS", "LEGACY_AVS", "JUDGEMENT")
        addTask(template, group, col, "L-AVS-1")

        store.transactional(true) {
            assertEquals("LEGACY_AVS", group.type.score.description)
        }
    }

    @Test
    fun `template with NOOP scorer can be created`() {
        val col = createTestCollection()
        val template = createTemplateShell("noop-${UUID.randomUUID()}")
        val group = addTaskTypeAndGroup(template, "NOOP", "NOOP", "NOOP", "MEDIA_ITEM")
        addTask(template, group, col, "NOOP-1")

        store.transactional(true) {
            assertEquals("NOOP", group.type.score.description)
        }
    }

    // ── Breaking the builder ───────────────────────────────────────────────────

    @Test
    fun `instantiating a template that is already an instance throws`() {
        val col = createTestCollection()
        val template = createTemplateShell("already-instance-${UUID.randomUUID()}")
        val group = addTaskTypeAndGroup(template, "KIS", "KIS", "KIS", "MEDIA_SEGMENT")
        addTask(template, group, col, "T1")

        store.transactional {
            val instance = template.toInstance()
            assertThrows(IllegalStateException::class.java) {
                instance.toInstance()
            }
        }
    }

    @Test
    fun `instantiated template with no tasks has empty task list`() {
        val template = createTemplateShell("no-tasks-${UUID.randomUUID()}")
        addTaskTypeAndGroup(template, "KIS", "KIS", "KIS", "MEDIA_SEGMENT")

        store.transactional(true) {
            val instance = template.toInstance()
            assertEquals(0, instance.tasks.size(), "Instance from no-task template must have 0 tasks")
        }
    }

    @Test
    fun `instantiated template with no teams has empty team list`() {
        val col = createTestCollection()
        val template = store.transactional { DbEvaluationTemplate.new { name = "no-teams-${UUID.randomUUID()}"; instance = false } }
        val group = addTaskTypeAndGroup(template, "KIS", "KIS", "KIS", "MEDIA_SEGMENT")
        addTask(template, group, col, "T1")

        store.transactional(true) {
            val instance = template.toInstance()
            assertEquals(0, instance.teams.size(), "Instance from no-team template must have 0 teams")
        }
    }

    @Test
    fun `task sort order is preserved through toInstance`() {
        val col = createTestCollection()
        val template = createTemplateShell("sort-order-${UUID.randomUUID()}")
        val group = addTaskTypeAndGroup(template, "KIS", "KIS", "KIS", "MEDIA_SEGMENT")
        val names = listOf("Zeta", "Alpha", "Gamma", "Beta")
        names.forEachIndexed { i, n -> addTask(template, group, col, n, idx = i) }

        store.transactional {
            val instance = template.toInstance()
            val instanceNames = instance.tasks.asSequence().sortedBy { it.idx }.map { it.name }.toList()
            assertEquals(names, instanceNames)
        }
    }

    @Test
    fun `template name is required — blank name throws`() {
        assertThrows(Exception::class.java) {
            store.transactional {
                DbEvaluationTemplate.new { name = "  "; instance = false }
            }
        }
    }
}
