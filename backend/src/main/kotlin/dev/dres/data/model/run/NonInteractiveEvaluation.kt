package dev.dres.data.model.run

import dev.dres.data.model.admin.UserId
import dev.dres.data.model.template.EvaluationTemplate
import dev.dres.data.model.template.team.TeamId
import dev.dres.data.model.run.interfaces.EvaluationRun
import dev.dres.data.model.run.interfaces.Run
import dev.dres.data.model.run.interfaces.TaskRun
import dev.dres.data.model.submissions.batch.ResultBatch
import kotlinx.dnq.query.asSequence
import kotlinx.dnq.query.size


/**
 * Represents a concrete, interactive and synchronous [Run] of a [EvaluationTemplate].
 *
 * [InteractiveSynchronousEvaluation]s can be started, ended and they can be used to create new [TaskRun]s and access the current [TaskRun].
 *
 * @author Luca Rossetto
 * @param 1.0.0
 */
class NonInteractiveEvaluation(evaluation: Evaluation) : AbstractEvaluation(evaluation) {

    init {
        require(this.evaluation.type == RunType.NON_INTERACTIVE) { "Incompatible competition type ${this.evaluation.type}. This is a programmer's error!" }
        require(this.description.tasks.size() > 0) { "Cannot create a run from a competition that doesn't have any tasks." }
        require(this.description.teams.size() > 0) { "Cannot create a run from a competition that doesn't have any teams." }
    }

    /** List of [TaskRun]s registered for this [NonInteractiveEvaluation]. */
    override val tasks: List<TaskRun> = this.evaluation.tasks.asSequence().map {
        NITaskRun(it)
    }.toList()

    /**
     * The [TaskRun] used by a [NonInteractiveEvaluation].
     */
    inner class NITaskRun(task: Task): AbstractNonInteractiveTask(task) {

        /** Reference to the [EvaluationRun] hosting this [NITaskRun]. */
        override val competition: EvaluationRun
            get() = this@NonInteractiveEvaluation

        /** The position of this [NITaskRun] within the [NonInteractiveEvaluation]. */
        override val position: Int
            get() = this@NonInteractiveEvaluation.tasks.indexOf(this)


        internal val submissions: MutableMap<Pair<TeamId, String>, ResultBatch<*>> = mutableMapOf()

        @Transient
        override val scorer: ResultBatchTaskScorer = template.newScorer() as? ResultBatchTaskScorer
            ?: throw IllegalArgumentException("specified scorer is not of type ResultBatchTaskScorer")

        @Synchronized
        override fun addSubmissionBatch(teamId: TeamId, memberId: UserId, batches: List<ResultBatch<*>>) {
            batches.forEach { resultBatch ->
                submissions[teamId to resultBatch.name] = resultBatch
            }
        }
    }
}