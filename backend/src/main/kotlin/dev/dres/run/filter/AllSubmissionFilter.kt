package dev.dres.run.filter

import dev.dres.data.model.submissions.DbSubmission


/**
 * A [SubmissionFilter] that lets all [DbSubmission] pass.
 *
 * @author Ralph Gasser
 * @version 1.0
 */
object AllSubmissionFilter : SubmissionFilter {
    override val reason = "" //will never be relevant

    override fun test(t: DbSubmission): Boolean = true
}