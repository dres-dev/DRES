package dev.dres.run.transformer

import dev.dres.api.rest.types.evaluation.ApiSubmission
import dev.dres.data.model.run.TaskId

/**
 * Removes all [AnswerSet]s from a [Submission] that do not match a specified [Task]
 */

class SubmissionTaskMatchFilter(private val taskId: TaskId) : SubmissionTransformer {
    override fun transform(submission: ApiSubmission): ApiSubmission =
        submission.copy(
            answers = submission.answers.filter { it.taskId == taskId }
        )

}