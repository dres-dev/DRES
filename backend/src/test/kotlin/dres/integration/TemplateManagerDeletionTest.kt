package dres.integration

import dev.dres.data.model.template.task.DbTaskGroup
import dev.dres.data.model.template.task.DbTaskType
import dev.dres.data.model.template.task.options.DbConfiguredOption
import dev.dres.mgmt.TemplateManager
import kotlinx.dnq.query.asSequence
import kotlinx.dnq.query.size
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.UUID

/**
 * Integration tests for TemplateManager.updateTemplate deletion behaviour (issue #488).
 *
 * Xodus will not properly cascade deletions across relationships, and the prior deletion
 * order (task types → task groups → task templates) caused a constraint failure:
 * deleting a DbTaskType triggers OnDeletePolicy.CASCADE on DbTaskGroup.type, which in
 * turn tries to delete DbTaskGroup — but DbTaskTemplate.taskGroup has an implicit FAIL
 * policy, so the cascade is blocked while any task template still exists.
 *
 * The fix reverses the deletion order (task templates → task groups → task types).
 * These tests verify that the corrected order works end-to-end via a real Xodus store.
 */
class TemplateManagerDeletionTest : AbstractDresIntegrationTest() {

    @BeforeAll
    fun initTemplateManager() {
        TemplateManager.init(store)
    }

    // ── Task type deletion ─────────────────────────────────────────────────────

    @Test
    fun `deleting a task type removes the type, its group, and its tasks`() {
        val col = createTestCollection()
        val template = createTemplateShell("del-type-${UUID.randomUUID()}")
        val groupA = addTaskTypeAndGroup(template, "TypeA", "GroupA", "KIS", "MEDIA_ITEM")
        val groupB = addTaskTypeAndGroup(template, "TypeB", "GroupB", "AVS", "JUDGEMENT")
        val taskA  = addTask(template, groupA, col, "Task-A")
                     addTask(template, groupB, col, "Task-B")

        val taskAId = store.transactional(true) { taskA.id }
        val api = store.transactional(true) { template.toApi() }

        // Strip TypeB and everything that references it
        val stripped = api.copy(
            taskTypes  = api.taskTypes.filter  { it.name != "TypeB" },
            taskGroups = api.taskGroups.filter { it.name != "GroupB" },
            tasks      = api.tasks.filter      { it.id   == taskAId  }
        )

        TemplateManager.updateTemplate(stripped)

        store.transactional(true) {
            val typeNames  = template.taskTypes.asSequence().map  { it.name }.toList()
            val groupNames = template.taskGroups.asSequence().map { it.name }.toList()

            assertFalse(typeNames.contains("TypeB"),  "TypeB should be deleted")
            assertTrue(typeNames.contains("TypeA"),   "TypeA should remain")
            assertFalse(groupNames.contains("GroupB"), "GroupB should be deleted")
            assertTrue(groupNames.contains("GroupA"),  "GroupA should remain")
            assertEquals(1, template.tasks.size(), "Only Task-A should remain")
        }
    }

    @Test
    fun `deleting all task types removes all groups and tasks`() {
        val col = createTestCollection()
        val template = createTemplateShell("del-all-${UUID.randomUUID()}")
        val g = addTaskTypeAndGroup(template, "OnlyType", "OnlyGroup", "KIS", "MEDIA_ITEM")
        addTask(template, g, col, "Only-Task")

        val api = store.transactional(true) { template.toApi() }
        val stripped = api.copy(taskTypes = emptyList(), taskGroups = emptyList(), tasks = emptyList())

        TemplateManager.updateTemplate(stripped)

        store.transactional(true) {
            assertEquals(0, template.taskTypes.size(),  "All task types should be removed")
            assertEquals(0, template.taskGroups.size(), "All task groups should be removed")
            assertEquals(0, template.tasks.size(),      "All tasks should be removed")
        }
    }

    @Test
    fun `deleting one of several task types leaves the others intact`() {
        val col = createTestCollection()
        val template = createTemplateShell("multi-del-${UUID.randomUUID()}")
        val gKeep1  = addTaskTypeAndGroup(template, "Keep1",  "GKeep1", "KIS", "MEDIA_ITEM")
        val gKeep2  = addTaskTypeAndGroup(template, "Keep2",  "GKeep2", "KIS", "MEDIA_ITEM")
        val gRemove = addTaskTypeAndGroup(template, "Remove", "GRemove", "AVS", "JUDGEMENT")
        val tKeep1  = addTask(template, gKeep1,  col, "T-Keep1")
        val tKeep2  = addTask(template, gKeep2,  col, "T-Keep2")
                      addTask(template, gRemove, col, "T-Remove")

        val keepIds = store.transactional(true) { listOf(tKeep1.id, tKeep2.id) }
        val api     = store.transactional(true) { template.toApi() }
        val stripped = api.copy(
            taskTypes  = api.taskTypes.filter  { it.name !in listOf("Remove") },
            taskGroups = api.taskGroups.filter { it.name !in listOf("GRemove") },
            tasks      = api.tasks.filter      { keepIds.contains(it.id) }
        )

        TemplateManager.updateTemplate(stripped)

        store.transactional(true) {
            assertEquals(2, template.taskTypes.size(),  "Two task types should remain")
            assertEquals(2, template.taskGroups.size(), "Two task groups should remain")
            assertEquals(2, template.tasks.size(),      "Two tasks should remain")
            val remaining = template.taskTypes.asSequence().map { it.name }.toSet()
            assertTrue(remaining.containsAll(listOf("Keep1", "Keep2")))
        }
    }

    @Test
    fun `deleting a task type with multiple tasks removes all of them`() {
        val col = createTestCollection()
        val template = createTemplateShell("del-multi-task-${UUID.randomUUID()}")
        val gKeep   = addTaskTypeAndGroup(template, "Keep",   "GKeep",   "KIS", "MEDIA_ITEM")
        val gRemove = addTaskTypeAndGroup(template, "Remove", "GRemove", "AVS", "JUDGEMENT")
        val tKeep   = addTask(template, gKeep,   col, "T-Keep")
                      addTask(template, gRemove, col, "T-Remove-1")
                      addTask(template, gRemove, col, "T-Remove-2")
                      addTask(template, gRemove, col, "T-Remove-3")

        val tKeepId = store.transactional(true) { tKeep.id }
        val api     = store.transactional(true) { template.toApi() }
        val stripped = api.copy(
            taskTypes  = api.taskTypes.filter  { it.name != "Remove" },
            taskGroups = api.taskGroups.filter { it.name != "GRemove" },
            tasks      = api.tasks.filter      { it.id == tKeepId }
        )

        TemplateManager.updateTemplate(stripped)

        store.transactional(true) {
            assertEquals(1, template.tasks.size(), "Only T-Keep should remain")
        }
    }

    // ── DbConfiguredOption cleanup ─────────────────────────────────────────────

    @Test
    fun `deleting a task type removes its DbConfiguredOption children`() {
        val col = createTestCollection()
        val template = createTemplateShell("del-config-${UUID.randomUUID()}")
        addTaskTypeAndGroup(template, "ConfigType", "CGroup", "KIS", "MEDIA_ITEM")

        // Attach a DbConfiguredOption to the task type directly
        val configCountBefore = store.transactional {
            val type = template.taskTypes.asSequence().first()
            type.configurations.add(DbConfiguredOption.new { key = "DOMAIN.KEY"; value = "val" })
            DbConfiguredOption.all().size()
        }

        val api = store.transactional(true) { template.toApi() }
        val stripped = api.copy(taskTypes = emptyList(), taskGroups = emptyList(), tasks = emptyList())

        TemplateManager.updateTemplate(stripped)

        store.transactional(true) {
            val configCountAfter = DbConfiguredOption.all().size()
            assertTrue(
                configCountAfter < configCountBefore,
                "DbConfiguredOption count should decrease after task type deletion (was $configCountBefore, now $configCountAfter)"
            )
        }
    }

    // ── Task-group-only deletion ───────────────────────────────────────────────

    @Test
    fun `deleting a task group without deleting its type leaves the type intact`() {
        val col = createTestCollection()
        val template = createTemplateShell("del-group-only-${UUID.randomUUID()}")
        val g1 = addTaskTypeAndGroup(template, "Type1", "G1", "KIS", "MEDIA_ITEM")
                 addTaskTypeAndGroup(template, "Type2", "G2", "KIS", "MEDIA_ITEM")
        val t1 = addTask(template, g1, col, "T1")

        val t1Id = store.transactional(true) { t1.id }
        val api  = store.transactional(true) { template.toApi() }
        val stripped = api.copy(
            taskGroups = api.taskGroups.filter { it.name != "G2" },
            tasks      = api.tasks.filter { it.id == t1Id }
        )

        TemplateManager.updateTemplate(stripped)

        store.transactional(true) {
            assertEquals(2, template.taskTypes.size(),  "Both task types should remain")
            assertEquals(1, template.taskGroups.size(), "Only G1 should remain")
            assertEquals(1, template.tasks.size(),      "Only T1 should remain")
        }
    }

    // ── Idempotency ────────────────────────────────────────────────────────────

    @Test
    fun `saving the same template twice without changes is idempotent`() {
        val col = createTestCollection()
        val template = createTemplateShell("idempotent-${UUID.randomUUID()}")
        val g = addTaskTypeAndGroup(template, "KIS", "KIS-Group", "KIS", "MEDIA_ITEM")
        addTask(template, g, col, "Task-1")
        addTask(template, g, col, "Task-2")

        val api = store.transactional(true) { template.toApi() }

        TemplateManager.updateTemplate(api)
        TemplateManager.updateTemplate(api.copy(modified = store.transactional(true) { template.toApi().modified }))

        store.transactional(true) {
            assertEquals(1, template.taskTypes.size())
            assertEquals(1, template.taskGroups.size())
            assertEquals(2, template.tasks.size())
        }
    }
}
