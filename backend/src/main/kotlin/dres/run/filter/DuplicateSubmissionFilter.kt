package dres.run.filter

import dres.data.model.run.Submission

class DuplicateSubmissionFilter : SubmissionFilter {
    override fun test(submission: Submission): Boolean = submission.taskRun!!.submissions.none {

        it.item == submission.item &&

        //contains a previous submission...
        ((submission.start!! <= it.start!! && submission.end!! >= it.end!!) ||


        //or is contained by a previous submission
        (it.start <= submission.start && it.end!! >= submission.end!!))
    }
}