package dres.integration

import dev.dres.data.model.admin.DbRole
import dev.dres.data.model.admin.DbUser
import dev.dres.data.model.media.*
import dev.dres.data.model.run.*
import dev.dres.data.model.submissions.*
import dev.dres.data.model.template.DbEvaluationTemplate
import dev.dres.data.model.template.task.*
import dev.dres.data.model.template.task.options.*
import dev.dres.data.model.template.team.DbTeam
import dev.dres.data.model.template.team.DbTeamAggregator
import dev.dres.data.model.template.team.DbTeamGroup
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.XdModel
import kotlinx.dnq.query.asSequence
import kotlinx.dnq.store.container.StaticStoreContainer
import kotlinx.dnq.util.initMetaData
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID

/**
 * Base class for DRES integration tests. Initialises a real on-disk Xodus store in a
 * temporary directory, registers all entity types once, and tears everything down after the
 * test class finishes.
 *
 * Because [StaticStoreContainer] is a JVM-wide singleton we initialise it exactly once for
 * the entire test process (guarded by [storeInitialised]). Individual test classes share the
 * same store; they achieve isolation by using unique names / IDs for every entity they create.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class AbstractDresIntegrationTest {

    companion object {
        @Volatile
        private var storeInitialised = false

        private lateinit var tempDir: Path
        lateinit var store: TransientEntityStore
            private set

        @Synchronized
        fun ensureStoreInitialised() {
            if (storeInitialised) return

            tempDir = Files.createTempDirectory("dres-test-")

            XdModel.registerNodes(
                DbConfiguredOption,
                DbEvaluation,
                DbEvaluationStatus,
                DbEvaluationType,
                DbEvaluationTemplate,
                DbHint,
                DbHintOption,
                DbHintType,
                DbMediaType,
                DbMediaCollection,
                DbMediaItem,
                DbMediaItemMetaDataEntry,
                DbMediaSegment,
                DbRole,
                DbScoreOption,
                DbSubmission,
                DbSubmissionOption,
                DbTask,
                DbTaskStatus,
                DbTaskGroup,
                DbTaskType,
                DbTaskOption,
                DbTaskTemplate,
                DbTaskTemplateTarget,
                DbTargetType,
                DbTargetOption,
                DbTeam,
                DbTeamAggregator,
                DbTeamGroup,
                DbUser,
                DbAnswer,
                DbAnswerSet,
                DbAnswerType,
                DbVerdictStatus,
            )

            store = StaticStoreContainer.init(dbFolder = tempDir.toFile(), entityStoreName = "dres-test-db")
            initMetaData(XdModel.hierarchy, store)
            storeInitialised = true
        }
    }

    @BeforeAll
    fun initStore() {
        ensureStoreInitialised()
    }

    @AfterAll
    fun cleanup() {
        /* Store is shared — we intentionally do not close it here. */
    }

    // ── Fixture helpers ────────────────────────────────────────────────────────

    /** Creates a minimal [DbMediaCollection] with a single video and a single image item. */
    fun createTestCollection(nameSuffix: String = UUID.randomUUID().toString()): DbMediaCollection =
        store.transactional {
            val col = DbMediaCollection.new {
                name = "test-collection-$nameSuffix"
                path = "/media/$nameSuffix"
            }
            DbMediaItem.new {
                name = "video1"
                type = DbMediaType.VIDEO
                location = "video1.mp4"
                fps = 25f
                durationMs = 60_000L
                collection = col
            }
            DbMediaItem.new {
                name = "image1"
                type = DbMediaType.IMAGE
                location = "image1.jpg"
                collection = col
            }
            col
        }

    /**
     * Builds a [DbEvaluationTemplate] shell (no tasks yet) with [teamCount] teams.
     * All returned values are detached — callers must operate inside their own transactions.
     */
    fun createTemplateShell(
        name: String,
        teamCount: Int = 2
    ): DbEvaluationTemplate = store.transactional {
        val template = DbEvaluationTemplate.new {
            this.name = name
            this.instance = false
        }
        repeat(teamCount) { i ->
            DbTeam.new {
                id = UUID.randomUUID().toString()
                this.name = "Team ${i + 1}"
                color = "#FF0000"
                logo = null
                evaluation = template
            }
        }
        template
    }

    /**
     * Adds a [DbTaskType] + [DbTaskGroup] pair to [template].
     * [score] defaults to KIS; pass [DbScoreOption.AVS] for AVS tasks.
     */
    fun addTaskTypeAndGroup(
        template: DbEvaluationTemplate,
        typeName: String,
        groupName: String,
        score: DbScoreOption,
        target: DbTargetOption,
        durationSeconds: Long? = 300L
    ): DbTaskGroup = store.transactional {
        val taskType = DbTaskType.new {
            this.name = typeName
            this.evaluation = template
            this.duration = durationSeconds
            this.score = score
            this.target = target
        }
        DbTaskGroup.new {
            this.name = groupName
            this.type = taskType
            this.evaluation = template
        }
    }

    /**
     * Adds a [DbTaskTemplate] to [template] that points to [collection] and is in [group].
     */
    fun addTask(
        template: DbEvaluationTemplate,
        group: DbTaskGroup,
        collection: DbMediaCollection,
        taskName: String,
        durationSeconds: Long? = 300L,
        idx: Int = 0
    ): DbTaskTemplate = store.transactional {
        val task = DbTaskTemplate.new {
            this.name = taskName
            this.taskGroup = group
            this.evaluation = template
            this.collection = collection
            this.duration = durationSeconds
            this.idx = idx
        }
        /* A task must have at least one target */
        DbTaskTemplateTarget.new {
            this.type = DbTargetType.MEDIA_ITEM
            this.task = task
        }
        task
    }

    /**
     * Converts [template] to an instance (the live copy used during an evaluation),
     * then creates and returns a [DbEvaluation] of [type].
     */
    fun createEvaluation(
        template: DbEvaluationTemplate,
        evaluationName: String,
        type: DbEvaluationType
    ): DbEvaluation = store.transactional {
        val instance = template.toInstance()
        val evaluation = DbEvaluation.new {
            this.name = evaluationName
            this.type = type
            this.template = instance
        }
        if (type == DbEvaluationType.INTERACTIVE_ASYNCHRONOUS) {
            evaluation.initPermutation()
        }
        evaluation
    }
}
