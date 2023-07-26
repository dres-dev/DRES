package dev.dres.run.filter

import dev.dres.api.rest.types.evaluation.submission.ApiClientAnswerSet
import dev.dres.api.rest.types.evaluation.submission.ApiClientSubmission
import dev.dres.data.model.run.DbTask
import dev.dres.data.model.submissions.AnswerSet
import dev.dres.data.model.submissions.DbAnswerSet
import dev.dres.data.model.submissions.DbSubmission
import dev.dres.run.filter.basics.AbstractSubmissionFilter
import dev.dres.run.filter.basics.SubmissionFilter
import kotlinx.dnq.query.*


/**
 * A [SubmissionFilter] that filters duplicate [DbSubmission]s in terms of content.
 *
 * @author Luca Rossetto
 * @version 1.1.0
 */
class DuplicateSubmissionFilter : AbstractSubmissionFilter("Duplicate submission received.") {


    /**
     * Tests the given [ApiClientSubmission] with this [SubmissionFilter] return true, if test succeeds.
     *
     * Requires an ongoing transaction!
     *
     * @param t The [ApiClientSubmission] to check.
     * @return True on success, false otherwise.
     */
    override fun test(t: ApiClientSubmission): Boolean {
        require(t.teamId != null) { "Submission ${t.submissionId} is not associated with a team. This is a programmer's error!" }
        for ((taskId, answerSets) in t.answerSets.groupBy { it.taskId }) {
            val existingAnswerSets = DbAnswerSet.filter { (it.taskId eq taskId) and (it.submission.team.id eq t.teamId!!) }
            for (answerSet in existingAnswerSets) {
                if (answerSets.any { isEquivalent(answerSet, it) }) {
                    return false
                }
            }
        }
        return true
    }

    /**
     * Checks a [DbAnswerSet] and an [ApiClientAnswerSet] for equivalence.
     *
     * @param a The [DbAnswerSet].
     * @param b The [ApiClientAnswerSet]
     * @return True, if compared sets are equivalents and thus duplicates.
     */
    private fun isEquivalent(a: DbAnswerSet, b: ApiClientAnswerSet): Boolean {
        for (answer in a.answers) {
            if (b.answers.find { it.text == answer.text && it.start == answer.start && it.end == answer.end && it.itemName == answer.item?.name } == null) {
                return false
            }
        }
       return true
    }
}