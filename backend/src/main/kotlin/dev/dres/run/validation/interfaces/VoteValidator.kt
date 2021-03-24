package dev.dres.run.validation.interfaces

import dev.dres.data.model.submissions.Submission
import dev.dres.data.model.submissions.SubmissionStatus

interface VoteValidator : JudgementValidator {

    /**
     * Indicates that this validator is currently active and accepting votes.
     */
    val isActive: Boolean

    /**
     * Current distribution of votes
     */
    val voteCount: Map<String, Int>

    /**
     * Places a verdict for the currently active Submission
     */
    fun vote(verdict: SubmissionStatus)

    fun nextSubmissionToVoteOn() : Submission?

}