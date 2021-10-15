package dev.dres.run.filter

import dev.dres.data.model.submissions.Submission


/**
 * A [SubmissionFilter] that lets all [Submission] pass.
 *
 * @author Ralph Gasser
 * @version 1.0
 */
object AllSubmissionFilter : SubmissionFilter {
    override val reason = "" //will never be relevant

    override fun test(t: Submission): Boolean = true
}