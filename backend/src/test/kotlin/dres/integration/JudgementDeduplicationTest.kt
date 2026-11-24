package dres.integration

import dev.dres.DRES
import dev.dres.api.rest.types.evaluation.submission.ApiVerdictStatus
import dev.dres.api.rest.types.template.tasks.ApiTaskTemplate
import dev.dres.api.rest.types.template.tasks.ApiTaskType
import dev.dres.api.rest.types.template.tasks.options.ApiScoreOption
import dev.dres.api.rest.types.template.tasks.options.ApiTargetOption
import dev.dres.data.model.submissions.DbAnswer
import dev.dres.data.model.submissions.DbAnswerSet
import dev.dres.data.model.submissions.DbAnswerType
import dev.dres.data.model.submissions.DbSubmission
import dev.dres.data.model.submissions.DbVerdictStatus
import dev.dres.data.model.run.DbTask
import dev.dres.data.model.run.InteractiveSynchronousEvaluation
import dev.dres.run.validation.judged.BasicJudgementValidator
import kotlinx.dnq.query.first
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.util.UUID

class JudgementDeduplicationTest : AbstractDresIntegrationTest() {

    @Test
    fun `identical answer ranges share one judgement and verdict`() {
        DRES.AUDIT_LOG_ROOT = Files.createTempDirectory("dres-judgement-audit-")

        val collection = createTestCollection()
        val sourceTemplate = createTemplateShell("judgement-dedup-${UUID.randomUUID()}", teamCount = 1)
        val group = addTaskTypeAndGroup(sourceTemplate, "JUDGEMENT", "Judgement", targetOption = "JUDGEMENT")
        val sourceTaskTemplate = addTask(sourceTemplate, group, collection, "Judgement task")
        val evaluation = createEvaluation(sourceTemplate, "judgement-dedup-run-${UUID.randomUUID()}", "INTERACTIVE_SYNCHRONOUS")
        val eval = store.transactional { InteractiveSynchronousEvaluation(store, evaluation) }
        val taskTemplateId = store.transactional(true) { eval.template.tasks.first().id!! }
        val task = createPersistedTask(evaluation, taskTemplateId)
        val correctStatus = store.transactional(true) { DbVerdictStatus.CORRECT }
        val team = store.transactional(true) { evaluation.template.teams.first() }
        val user = createTestUser()
        val mediaItem = store.transactional(true) { collection.items.first() }
        val validator = BasicJudgementValidator(
            taskTemplate = ApiTaskTemplate(
                id = null,
                name = "Judgement task",
                taskGroup = "Judgement",
                taskType = "JUDGEMENT",
                duration = 300L,
                collectionId = "collection",
                targets = emptyList(),
                hints = emptyList(),
                comment = null
            ),
            store = store,
            taskType = ApiTaskType(
                name = "JUDGEMENT",
                duration = 300L,
                targetOption = ApiTargetOption.JUDGEMENT,
                hintOptions = emptyList(),
                submissionOptions = emptyList(),
                taskOptions = emptyList(),
                scoreOption = ApiScoreOption.KIS,
                configuration = emptyMap()
            )
        )

        val answerSets = store.transactional {
            val submission = DbSubmission.new {
                id = UUID.randomUUID().toString()
                timestamp = System.currentTimeMillis()
                this.team = team
                this.user = user
            }
            (1..3).map {
                val answerSet = DbAnswerSet.new {
                    this.submission = submission
                    status = DbVerdictStatus.INDETERMINATE
                    this.task = task
                }
                DbAnswer.new {
                    this.answerSet = answerSet
                    type = DbAnswerType.TEMPORAL
                    item = mediaItem
                    start = 10_000L
                    end = 20_000L
                }
                answerSet
            }
        }

        // Revalidating an answer set (for example during task reinitialization) must not duplicate it.
        store.transactional {
            validator.validate(answerSets[0])
            validator.validate(answerSets[0])
            validator.validate(answerSets[1])
        }
        assertEquals(1, validator.pending)

        val request = validator.next()
        assertNotNull(request)
        assertEquals(1, validator.pending)

        // New matching submissions arriving while the range is being judged join its verdict fan-out.
        validator.validate(answerSets[2])
        validator.judge(request!!.first, ApiVerdictStatus.CORRECT)
        assertEquals(0, validator.pending)
        assertEquals(
            listOf(correctStatus, correctStatus, correctStatus),
            store.transactional(true) { answerSets.map { it.status } }
        )

        // A later identical submission uses the verdict cache rather than opening another judgement.
        val later = store.transactional {
            val submission = DbSubmission.new {
                id = UUID.randomUUID().toString()
                timestamp = System.currentTimeMillis()
                this.team = team
                this.user = user
            }
            DbAnswerSet.new {
                this.submission = submission
                status = DbVerdictStatus.INDETERMINATE
                this.task = task
            }.also { answerSet ->
                DbAnswer.new {
                    this.answerSet = answerSet
                    type = DbAnswerType.TEMPORAL
                    item = mediaItem
                    start = 10_000L
                    end = 20_000L
                }
            }
        }
        validator.validate(later)
        assertEquals(correctStatus, store.transactional(true) { later.status })
        assertEquals(0, validator.pending)
        assertEquals(null, validator.next())
    }
}
