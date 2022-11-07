package dev.dres.run.score.interfaces

import dev.dres.data.model.submissions.Submission
import dev.dres.data.model.submissions.Verdict

/**
 * A [TaskScorer] implementation that can update scores incrementally on a [Submission] by [Submission] basis.
 *
 * @author Luca Rossetto & Ralph Gasser
 * @version 1.1.0
 */
interface IncrementalSubmissionTaskScorer: TaskScorer {
    /**
     * Updates this [IncrementalSubmissionTaskScorer]'s score just using a single submission.
     *
     * @param submission The [Submission] to update this [IncrementalSubmissionTaskScorer] with.
     */
    fun update(submission: Submission)
}