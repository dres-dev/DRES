package dev.dres.mgmt

import dev.dres.api.rest.types.evaluation.ApiEndedEvaluationInfo
import dev.dres.api.rest.types.evaluation.ApiVerdictCounts
import dev.dres.data.model.run.DbEvaluation
import dev.dres.data.model.run.interfaces.EvaluationId
import dev.dres.data.model.submissions.DbSubmission
import dev.dres.run.RunExecutor
import dev.dres.run.RunManager
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.asSequence
import kotlinx.dnq.query.eq
import kotlinx.dnq.query.firstOrNull
import kotlinx.dnq.query.query
import kotlinx.dnq.query.size

/** Management functions for evaluations persisted in the database. */
object EvaluationManager {

    private lateinit var store: TransientEntityStore

    fun init(store: TransientEntityStore) {
        this.store = store
    }

    /** Information about an evaluation removed from persistent storage. */
    data class DeletedEvaluation(val evaluationId: EvaluationId, val name: String, val submissions: Int)

    /** Whether a run manager is still registered with the executor. */
    fun isActive(evaluationId: EvaluationId): Boolean = RunExecutor.managers().any { it.id == evaluationId }

    /** Lists evaluations no longer registered with the executor, newest first. */
    fun listEnded(): List<ApiEndedEvaluationInfo> {
        val active = RunExecutor.managers().map { it.id }.toSet()
        return store.transactional(true) {
            DbEvaluation.all().asSequence().filter { it.evaluationId !in active }.map { evaluation ->
                ApiEndedEvaluationInfo(
                    evaluationId = evaluation.evaluationId,
                    name = evaluation.name,
                    templateName = evaluation.template.name,
                    templateDescription = evaluation.template.description,
                    type = evaluation.type.toApi(),
                    created = evaluation.created,
                    started = evaluation.started,
                    ended = evaluation.ended,
                    taskCount = evaluation.tasks.size(),
                    submissionCount = submissionsOf(evaluation).size,
                    verdicts = countVerdicts(evaluation)
                )
            }.sortedByDescending { it.ended ?: it.started ?: it.created }.toList()
        }
    }

    /**
     * Deletes a stopped evaluation, its submissions and its private template instance.
     * The original reusable template is retained.
     * @throws IllegalStateException if the evaluation is still active.
     */
    /** Counts are calculated from the same run snapshot to avoid a second Xodus read per refresh. */
    fun verdictCounts(manager: RunManager): ApiVerdictCounts = store.transactional(true) {
        DbEvaluation.query(DbEvaluation::id eq manager.id).firstOrNull()?.let(::countVerdicts)
            ?: ApiVerdictCounts(0, 0, 0, 0)
    }

    fun deleteEvaluation(evaluationId: EvaluationId): DeletedEvaluation? =
        RunExecutor.withNoActiveManager(evaluationId) {
            store.transactional {
                val evaluation = DbEvaluation.query(DbEvaluation::id eq evaluationId).firstOrNull()
                    ?: return@transactional null
                val name = evaluation.name
                val submissions = submissionsOf(evaluation)
                submissions.forEach { it.delete() }
                val template = evaluation.template
                evaluation.delete()
                if (template.instance) template.delete()
                DeletedEvaluation(evaluationId, name, submissions.size)
            }
        }

    /** Counts answer sets per verdict, returning zeroes when the evaluation is absent. */
    fun verdictCounts(evaluationId: EvaluationId): ApiVerdictCounts = store.transactional(true) {
        DbEvaluation.query(DbEvaluation::id eq evaluationId).firstOrNull()?.let(::countVerdicts)
            ?: ApiVerdictCounts(0, 0, 0, 0)
    }

    private fun countVerdicts(evaluation: DbEvaluation): ApiVerdictCounts {
        val counts = evaluation.tasks.asSequence()
            .flatMap { task -> task.answerSets.asSequence().map { it.status.description } }
            .groupingBy { it }.eachCount()
        return ApiVerdictCounts(
            correct = counts["CORRECT"] ?: 0,
            wrong = counts["WRONG"] ?: 0,
            indeterminate = counts["INDETERMINATE"] ?: 0,
            undecidable = counts["UNDECIDABLE"] ?: 0
        )
    }

    private fun submissionsOf(evaluation: DbEvaluation): List<DbSubmission> =
        evaluation.tasks.asSequence()
            .flatMap { task -> task.answerSets.asSequence().map { it.submission } }
            .distinctBy { it.id }
            .toList()
}
