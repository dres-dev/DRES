package dev.dres.run.filter

import dev.dres.data.model.submissions.Submission
import dev.dres.data.model.submissions.aspects.ItemAspect
import dev.dres.data.model.submissions.aspects.TemporalSubmissionAspect
import dev.dres.data.model.submissions.aspects.TextAspect

class DuplicateSubmissionFilter : SubmissionFilter {

    override val reason = "Duplicate submission"

    override fun test(submission: Submission): Boolean = submission.task!!.submissions.none {
    it.teamId == submission.teamId &&
    if(it is ItemAspect && submission is ItemAspect) {
        it.item == submission.item &&
                if (submission is TemporalSubmissionAspect && it is TemporalSubmissionAspect) {
                    /*(*/(submission.start <= it.start && submission.end >= it.end) /*|| */
                } else {
                    true
                }
    } else if (it is TextAspect && submission is TextAspect) {
        it.text == submission.text
    } else true
    }
}