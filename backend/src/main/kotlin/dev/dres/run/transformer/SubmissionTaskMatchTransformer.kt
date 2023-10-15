package dev.dres.run.transformer

import dev.dres.api.rest.types.evaluation.submission.ApiClientSubmission
import dev.dres.data.model.run.TaskId
import dev.dres.data.model.submissions.DbSubmission
import dev.dres.run.transformer.basics.SubmissionTransformer
import kotlinx.dnq.query.iterator

/**
 * A [SubmissionTransformer] that removes all [AnswerSet]s from a [Submission] that do not match a specified [TaskId]
 *
 * @author Luca Rossetto
 */
class SubmissionTaskMatchTransformer(private val taskId: TaskId) : SubmissionTransformer {
    /**
     * Apply this [SubmissionTaskMatchTransformer] to the provided [ApiClientSubmission].
     *
     * Requires an ongoing transaction.
     *
     * @param submission [ApiClientSubmission] to transform.
     */
    override fun transform(submission: ApiClientSubmission): ApiClientSubmission {

        return submission.copy(
            answerSets = submission.answerSets.filter { it.taskId == this.taskId }
        )

    }
}