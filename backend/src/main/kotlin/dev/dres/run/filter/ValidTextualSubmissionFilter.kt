package dev.dres.run.filter

import dev.dres.api.rest.types.evaluation.submission.ApiClientSubmission
import dev.dres.data.model.submissions.DbSubmission
import dev.dres.run.filter.basics.AbstractSubmissionFilter
import dev.dres.run.filter.basics.SubmissionFilter

/**
 * A [SubmissionFilter} that checks if a [DbSubmission] contains text information.
 *
 * @author Luca Rossetto
 * @version 1.1.0
 */
class ValidTextualSubmissionFilter : AbstractSubmissionFilter("Submission does not include textual information (or is an empty submission)") {

    /**
     * Tests the given [ApiClientSubmission] with this [SubmissionFilter] return true, if test succeeds.
     *
     * Requires an ongoing transaction!
     *
     * @param t The [ApiClientSubmission] to check.
     * @return True on success, false otherwise.
     */
    override fun test(t: ApiClientSubmission): Boolean {
        for (answerSet in t.answerSets) {
            for (answer in answerSet.answers) {
                if (answer.text == null) return false /* Check that start precedes end timestamp. */
            }
        }
        return true
    }
}
