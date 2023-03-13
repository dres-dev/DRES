package dev.dres.run.filter

import dev.dres.api.rest.types.evaluation.ApiSubmission
import dev.dres.data.model.submissions.DbSubmission
import dev.dres.data.model.submissions.Submission


/**
 * A [SubmissionFilter] that lets all [DbSubmission] pass.
 *
 * @author Ralph Gasser
 * @version 1.0
 */
object AllSubmissionFilter : SubmissionFilter {
    override val reason = "" //will never be relevant

    override fun test(t: ApiSubmission): Boolean = true
}
