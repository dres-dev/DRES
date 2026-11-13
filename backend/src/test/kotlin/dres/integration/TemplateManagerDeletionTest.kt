package dres.integration

import dev.dres.data.model.template.task.DbTaskGroup
import dev.dres.data.model.template.task.DbTaskType
import dev.dres.data.model.template.task.options.DbConfiguredOption
import dev.dres.data.model.template.task.options.Parameters
import dev.dres.api.rest.types.template.tasks.options.ApiTaskOption
import dev.dres.data.model.template.team.DbTeamGroup
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

    @Test
    fun `template export can be imported with new IDs and independent team group memberships`() {
        val collection = createTestCollection()
        val source = createTemplateShell("round-trip-${UUID.randomUUID()}", teamCount = 2)
        val group = addTaskTypeAndGroup(source, "KIS", "KIS", "KIS", "MEDIA_ITEM")
        addTask(source, group, collection, "Task-1")
        val sourceIds = store.transactional {
            val sourceTeams = source.teams.asSequence().toList()
            val sourceGroup = DbTeamGroup.new {
                id = UUID.randomUUID().toString()
                name = "all-teams"
                defaultAggregator = dev.dres.data.model.template.team.DbTeamAggregator.MEAN
            }
            sourceTeams.forEach { sourceGroup.teams.add(it) }
            source.teamGroups.add(sourceGroup)
            Triple(source.id, sourceTeams.map { it.id }.toSet(), sourceGroup.id)
        }
        val exported = TemplateManager.getTemplate(sourceIds.first)!!

        val imported = TemplateManager.importTemplate(exported, "round-trip-copy-${UUID.randomUUID()}")

        assertNotEquals(sourceIds.first, imported.id)
        assertNotEquals(exported.tasks.single().id, imported.tasks.single().id)
        assertNotEquals(sourceIds.third, imported.teamGroups.single().id)
        assertEquals("Task-1", imported.tasks.single().name)
        assertEquals(setOf("Team 1", "Team 2"), imported.teamGroups.single().teams.mapNotNull { it.name }.toSet())
        assertTrue(imported.teamGroups.single().teams.all { team -> team.id in imported.teams.mapNotNull { it.id } })
    }

    @Test
    fun `legacy submission option configuration is normalized when template is saved`() {
        val template = createTemplateShell("legacy-config-${UUID.randomUUID()}", teamCount = 0)
        val group = addTaskTypeAndGroup(template, "KIS", "KIS", "KIS", "MEDIA_ITEM")
        addTask(template, group, createTestCollection(), "Task-1")
        val current = store.transactional(true) { template.toApi() }
        val legacy = current.copy(taskTypes = current.taskTypes.map { type ->
            type.copy(
                taskOptions = type.taskOptions + dev.dres.api.rest.types.template.tasks.options.ApiTaskOption.PROLONG_ON_SUBMISSION,
                configuration = mapOf(
                    Parameters.LEGACY_PROLONG_ON_SUBMISSION_LIMIT_PARAM to "1500",
                    Parameters.PROLONG_ON_SUBMISSION_LIMIT_PARAM to "2500",
                    Parameters.LEGACY_PROLONG_ON_SUBMISSION_BY_PARAM to "30",
                    Parameters.LEGACY_PROLONG_ON_SUBMISSION_CORRECT_PARAM to "true"
                )
            )
        })

        TemplateManager.updateTemplate(legacy)

        val saved = TemplateManager.getTemplate(current.id)!!.taskTypes.single()
        assertEquals(
            mapOf(
                Parameters.PROLONG_ON_SUBMISSION_LIMIT_PARAM to "2500",
                Parameters.PROLONG_ON_SUBMISSION_BY_PARAM to "30",
                Parameters.PROLONG_ON_SUBMISSION_CORRECT_PARAM to "true"
            ),
            saved.configuration
        )
        assertNull(Parameters.value(saved.configuration, Parameters.LEGACY_PROLONG_ON_SUBMISSION_LIMIT_PARAM))
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
