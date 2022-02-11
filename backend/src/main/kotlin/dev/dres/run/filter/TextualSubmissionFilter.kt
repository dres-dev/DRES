package dev.dres.run.filter

import dev.dres.data.model.submissions.Submission
import dev.dres.data.model.submissions.aspects.TextAspect

class TextualSubmissionFilter : SubmissionFilter {

    override val reason = "Submission does not include textual information (or is an emppty submission)"

    override fun test(submission: Submission): Boolean = submission is TextAspect && submission.text.isNotEmpty()
}
