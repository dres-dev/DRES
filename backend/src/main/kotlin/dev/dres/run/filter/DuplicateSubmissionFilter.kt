package dev.dres.run.filter

import dev.dres.data.model.run.Submission
import dev.dres.data.model.run.TemporalSubmissionAspect

class DuplicateSubmissionFilter : SubmissionFilter {
    override fun test(submission: Submission): Boolean = submission.taskRun!!.submissions.none {

        it.team == submission.team &&
        it.item == submission.item &&

         if(submission is TemporalSubmissionAspect && it is TemporalSubmissionAspect) {
             //contains a previous submission...
             /*(*/(submission.start <= it.start && submission.end >= it.end) /*|| */
         } else {
            true
         }

        //or is contained by a previous submission
        /*(it.start <= submission.start && it.end!! >= submission.end!!))*/
    }
}