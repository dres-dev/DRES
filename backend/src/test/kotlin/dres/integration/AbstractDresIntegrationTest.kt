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
import kotlinx.dnq.query.eq
import kotlinx.dnq.query.filter
import kotlinx.dnq.query.first
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
 * temporary directory, registers all entity types exactly once, and tears down nothing
 * (the store is process-wide and shared across all test classes).
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class AbstractDresIntegrationTest {

    companion object {
        @Volatile
        private var storeInitialised = false

        private lateinit var tempDir: Path
        private lateinit var _store: TransientEntityStore

        val store: TransientEntityStore
            get() = _store

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

            val s = StaticStoreContainer.init(dbFolder = tempDir.toFile(), entityStoreName = "dres-test-db")
            initMetaData(XdModel.hierarchy, s)
            _store = s

            /* Warm up all XdEnumEntity caches so they can be referenced outside transactions in tests. */
            s.transactional(true) {
                DbScoreOption.KIS; DbScoreOption.AVS; DbScoreOption.LEGACY_AVS; DbScoreOption.NOOP
                DbTargetOption.MEDIA_ITEM; DbTargetOption.MEDIA_SEGMENT
                DbTargetOption.JUDGEMENT; DbTargetOption.VOTE; DbTargetOption.TEXT
                DbHintOption.IMAGE_ITEM; DbHintOption.VIDEO_ITEM_SEGMENT; DbHintOption.TEXT
                DbHintOption.EXTERNAL_IMAGE; DbHintOption.EXTERNAL_VIDEO
                DbTargetType.MEDIA_ITEM; DbTargetType.MEDIA_ITEM_TEMPORAL_RANGE
                DbTargetType.JUDGEMENT; DbTargetType.JUDGEMENT_WITH_VOTE; DbTargetType.TEXT
                DbSubmissionOption.NO_DUPLICATES; DbSubmissionOption.LIMIT_CORRECT_PER_TEAM
                DbSubmissionOption.LIMIT_WRONG_PER_TEAM; DbSubmissionOption.LIMIT_TOTAL_PER_TEAM
                DbSubmissionOption.LIMIT_CORRECT_PER_MEMBER; DbSubmissionOption.TEMPORAL_SUBMISSION
                DbSubmissionOption.TEXTUAL_SUBMISSION; DbSubmissionOption.ITEM_SUBMISSION
                DbSubmissionOption.MINIMUM_TIME_GAP
                DbTaskOption.HIDDEN_RESULTS; DbTaskOption.MAP_TO_SEGMENT; DbTaskOption.PROLONG_ON_SUBMISSION
                DbEvaluationType.INTERACTIVE_SYNCHRONOUS; DbEvaluationType.INTERACTIVE_ASYNCHRONOUS
                DbEvaluationType.NON_INTERACTIVE
                DbEvaluationStatus.CREATED; DbEvaluationStatus.ACTIVE; DbEvaluationStatus.TERMINATED
                DbTaskStatus.CREATED; DbTaskStatus.PREPARING; DbTaskStatus.RUNNING; DbTaskStatus.ENDED
                DbMediaType.VIDEO; DbMediaType.IMAGE; DbMediaType.TEXT
                DbHintType.EMPTY; DbHintType.VIDEO; DbHintType.IMAGE; DbHintType.TEXT
                DbAnswerType.TEMPORAL; DbAnswerType.ITEM; DbAnswerType.TEXT
                DbVerdictStatus.CORRECT; DbVerdictStatus.WRONG
                DbVerdictStatus.INDETERMINATE; DbVerdictStatus.UNDECIDABLE
            }

            storeInitialised = true
        }
    }

    @BeforeAll
    fun initStore() {
        ensureStoreInitialised()
    }

    @AfterAll
    fun cleanup() {
        /* Store is shared — intentionally not closed here. */
    }

    // ── Fixture helpers ────────────────────────────────────────────────────────

    /** Creates a [DbMediaCollection] with one video and one image item. */
    fun createTestCollection(nameSuffix: String = UUID.randomUUID().toString()): DbMediaCollection {
        val s = store
        return s.transactional {
            val col = DbMediaCollection.new {
                name = "test-collection-$nameSuffix"
                path = "/media/$nameSuffix"
            }
            col.items.add(DbMediaItem.new {
                name = "video1"
                type = DbMediaType.VIDEO
                location = "video1.mp4"
                fps = 25f
                durationMs = 60_000L
            })
            col.items.add(DbMediaItem.new {
                name = "image1"
                type = DbMediaType.IMAGE
                location = "image1.jpg"
            })
            col
        }
    }

    /**
     * Creates a [DbEvaluationTemplate] shell (no tasks) with [teamCount] teams.
     */
    fun createTemplateShell(name: String, teamCount: Int = 2): DbEvaluationTemplate {
        val s = store
        return s.transactional {
            val template = DbEvaluationTemplate.new {
                this.name = name
                this.instance = false
            }
            repeat(teamCount) { i ->
                template.teams.add(DbTeam.new {
                    id = UUID.randomUUID().toString()
                    this.name = "Team ${i + 1}"
                    color = "#FF0000"
                })
            }
            template
        }
    }

    /**
     * Adds a [DbTaskType] and associated [DbTaskGroup] to [template].
     * [scoreOption] and [targetOption] are the string descriptions of the corresponding Db enum values
     * (e.g. "KIS", "AVS", "MEDIA_ITEM", "MEDIA_SEGMENT").
     */
    fun addTaskTypeAndGroup(
        template: DbEvaluationTemplate,
        typeName: String,
        groupName: String,
        scoreOption: String = "KIS",
        targetOption: String = "MEDIA_ITEM",
        durationSeconds: Long? = 300L
    ): DbTaskGroup {
        val s = store
        return s.transactional {
            val score = DbScoreOption.filter { it.description eq scoreOption }.first()
            val target = DbTargetOption.filter { it.description eq targetOption }.first()
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
    }

    /**
     * Adds a [DbTaskTemplate] to [template] within [group], backed by [collection].
     */
    fun addTask(
        template: DbEvaluationTemplate,
        group: DbTaskGroup,
        collection: DbMediaCollection,
        taskName: String,
        durationSeconds: Long? = 300L,
        idx: Int = 0
    ): DbTaskTemplate {
        val s = store
        return s.transactional {
            val task = DbTaskTemplate.new {
                this.name = taskName
                this.taskGroup = group
                this.evaluation = template
                this.collection = collection
                this.duration = durationSeconds
                this.idx = idx
            }
            task.targets.add(DbTaskTemplateTarget.new {
                this.type = DbTargetType.MEDIA_ITEM
            })
            task
        }
    }

    /**
     * Instantiates [template] and wraps it in a [DbEvaluation] of the given [typeDescription]
     * ("INTERACTIVE_SYNCHRONOUS", "INTERACTIVE_ASYNCHRONOUS", or "NON_INTERACTIVE").
     */
    fun createEvaluation(
        template: DbEvaluationTemplate,
        evaluationName: String,
        typeDescription: String
    ): DbEvaluation {
        val s = store
        return s.transactional {
            val type = DbEvaluationType.filter { it.description eq typeDescription }.first()
            val instance = template.toInstance()
            val evaluation = DbEvaluation.new {
                this.name = evaluationName
                this.type = type
                this.template = instance
            }
            if (typeDescription == "INTERACTIVE_ASYNCHRONOUS") {
                evaluation.initPermutation()
            }
            evaluation
        }
    }
}
