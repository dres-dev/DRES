package dev.dres.run.transformer

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
     * Apply this [SubmissionTaskMatchTransformer] to the provided [DbSubmission]. Transformation happens in place.
     *
     * Requires an ongoing transaction.
     *
     * @param submission [DbSubmission] to transform.
     */
    override fun transform(submission: DbSubmission) {
        for (answerSet in submission.answerSets) {
            if (answerSet.task.id != this.taskId) {
                answerSet.delete()
            }
        }
    }
}