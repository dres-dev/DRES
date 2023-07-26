package dev.dres.run.filter

import dev.dres.api.rest.types.evaluation.submission.ApiClientSubmission
import dev.dres.run.filter.basics.AbstractSubmissionFilter
import dev.dres.run.filter.basics.SubmissionFilter

/**
 * A [SubmissionFilter] that filters for pure media item submissions (no temporal information, no text).
 *
 * @author Luca Rossetto
 * @version 2.0.0
 */
class ValidItemSubmissionFilter: AbstractSubmissionFilter("Submission does include temporal information, but whole item was expected.") {

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
                if (answer.itemName == null) return false /* Check that a media item has been specified. */
                if (answer.start != null) return false /* Check no start timestamp is contained. */
                if (answer.end != null) return false /* Check no end timestamp is contained. */
                if (answer.text != null) return false /* Check no text is contained. */
            }
        }
        return true
    }
}
