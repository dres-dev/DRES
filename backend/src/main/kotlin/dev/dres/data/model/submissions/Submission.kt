package dev.dres.data.model.submissions

import dev.dres.data.model.admin.UserId
import dev.dres.data.model.template.team.TeamId

typealias SubmissionId = String

/**
 * A [Submission] as issued by a DRES user.
 * Essentially a proposed solution to a certain task. The task's reference is linked via the answer sets.
 *
 * This abstraction is mainly required to enable testability of implementations.
 *
 * @author Luca Rossetto
 * @author Ralph Gasser
 * @author Loris Sauter
 * @version 2.0.0
 */
interface Submission {
    /** The ID of this [Submission]. */
    val submissionId: SubmissionId

    /** The ID of the user who issued this [Submission]. */
    val memberId: UserId

    /** The ID of the team, the user belongs to. */
    val teamId: TeamId

    /** The timestamp of this [Submission]. */
    val timestamp: Long

    /**
     * Returns a [Sequence] of [AnswerSet]s.
     */
    fun answerSets(): Sequence<AnswerSet>
}
