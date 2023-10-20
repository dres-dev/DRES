package dev.dres.run.filter

import dev.dres.api.rest.types.evaluation.submission.ApiClientSubmission
import dev.dres.run.filter.basics.AbstractSubmissionFilter
import dev.dres.run.filter.basics.SubmissionFilter


/**
 * A [SubmissionFilter] that checks if a [ApiClientSubmission] contains temporal information.
 *
 * @author Luca Rossetto
 * @version 2.0.0
 */
class ValidTemporalSubmissionFilter : AbstractSubmissionFilter("Submission does include non-temporal information.") {

    /**
     * Tests the given [ApiClientSubmission] with this [SubmissionFilter] return true, if test succeeeds.
     *
     * Requires an ongoing transaction!
     *
     * @param t The [ApiClientSubmission] to check.
     * @return True on success, false otherwise.
     */
    override fun test(t: ApiClientSubmission): Boolean {
        for (answerSet in t.answerSets) {
            for (answer in answerSet.answers) {
                if (answer.mediaItemName == null) return false /* Check that either a media item or a text has been specified. */
                if (answer.start == null) return false /* Check that start timestamp is contained. */
                if (answer.end == null) return false /* Check that end timestamp is contained. */
                if (answer.start > answer.end) return false /* Check that start precedes end timestamp. */
            }
        }
        return true
    }
}