package dres.run.filter

import dres.data.model.run.Submission

/**
 * A [SubmissionFilter] that lets all [Submission] pass.
 *
 * @author Ralph Gasser
 * @version 1.0
 */
object AllSubmissionFilter : SubmissionFilter {
    override fun test(t: Submission): Boolean = true
}