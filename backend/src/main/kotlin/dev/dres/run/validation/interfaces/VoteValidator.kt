package dev.dres.run.validation.interfaces

import dev.dres.data.model.submissions.AnswerSet
import dev.dres.data.model.submissions.VerdictStatus

/**
 *
 */
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
    fun vote(status: VerdictStatus)

    /**
     *
     */
    fun nextSubmissionToVoteOn() : AnswerSet?
}