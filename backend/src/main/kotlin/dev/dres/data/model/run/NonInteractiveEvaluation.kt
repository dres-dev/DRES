package dev.dres.data.model.run

import dev.dres.data.model.run.InteractiveSynchronousEvaluation.ISTaskRun
import dev.dres.data.model.template.DbEvaluationTemplate
import dev.dres.data.model.run.interfaces.EvaluationRun
import dev.dres.data.model.run.interfaces.Run
import dev.dres.data.model.run.interfaces.TaskRun
import dev.dres.data.model.submissions.DbSubmission
import dev.dres.data.model.submissions.Submission
import dev.dres.run.filter.SubmissionFilter
import kotlinx.dnq.query.*


/**
 * Represents a concrete, interactive and synchronous [Run] of a [DbEvaluationTemplate].
 *
 * [InteractiveSynchronousEvaluation]s can be started, ended and they can be used to create new [TaskRun]s and access the current [TaskRun].
 *
 * @author Luca Rossetto
 * @param 1.0.0
 */
class NonInteractiveEvaluation(evaluation: DbEvaluation) : AbstractEvaluation(evaluation) {

    init {
        require(this.evaluation.type == DbEvaluationType.NON_INTERACTIVE) { "Incompatible competition type ${this.evaluation.type}. This is a programmer's error!" }
        require(this.description.tasks.size() > 0) { "Cannot create a run from a competition that doesn't have any tasks." }
        require(this.description.teams.size() > 0) { "Cannot create a run from a competition that doesn't have any teams." }
    }

    /** List of [TaskRun]s registered for this [NonInteractiveEvaluation]. */
    override val tasks = this.evaluation.tasks.asSequence().map {
        NITaskRun(it)
    }.toList()

    /**
     * The [TaskRun] used by a [NonInteractiveEvaluation].
     */
    inner class NITaskRun(task: DbTask): AbstractNonInteractiveTask(task) {

        /** Reference to the [EvaluationRun] hosting this [NITaskRun]. */
        override val competition: EvaluationRun
            get() = this@NonInteractiveEvaluation

        /** The position of this [NITaskRun] within the [NonInteractiveEvaluation]. */
        override val position: Int
            get() = this@NonInteractiveEvaluation.tasks.indexOf(this)

        /** The [TeamTaskScorer] instance used by this [ISTaskRun]. */
        override val scorer = this.template.newScorer()
        /** */
        override val filter: SubmissionFilter
            get() = TODO("Can there be submission filters for non-interactive tasks?")

        @Synchronized
        override fun postSubmission(submission: Submission) {
            check(this@NonInteractiveEvaluation.description.teams.asSequence().filter { it == submission.team }.any()) {
                "Team ${submission.team.teamId} does not exists for evaluation run ${this@NonInteractiveEvaluation.name}. This is a programmer's error!"
            }

            /* Execute submission filters. */
            this.filter.acceptOrThrow(submission)

            val dbSubmission: DbSubmission = TODO("submission needs to be stored at this point and not earlier")

            /* Process Submission. */
            this.submissions.add(dbSubmission)

            /* TODO: Validation? */
        }
    }
}