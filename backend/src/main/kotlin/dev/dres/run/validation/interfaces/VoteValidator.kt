package dev.dres.run.validation.interfaces

import dev.dres.data.model.submissions.DbAnswerSet
import dev.dres.data.model.submissions.DbVerdictStatus

/**
 * A [JudgementValidator] that can hand-off undecidable verdict to a public vote.
 *
 * This kind of [AnswerSetValidator]  is inherently asynchronous.
 *
 * @author Luca Rossetto
 * @author Ralph Gasser
 * @version 2.0.0
 */
interface VoteValidator : JudgementValidator {
    /** Indicates that this validator is currently active and accepting votes. */
    val isActive: Boolean

    /** Current distribution of votes */
    val voteCount: Map<String, Int>

    /**
     * Places a verdict for the currently active vote.
     *
     * @param verdict The [DbVerdictStatus] of the vote.
     */
    fun vote(verdict: DbVerdictStatus)

    /**
     * Returns the [DbAnswerSet] this [VoteValidator] is currently accepting votes for.
     *
     * @return [DbAnswerSet] or null, if no vote is ongoing.
     */
    fun current() : DbAnswerSet?
}