package dev.dres.data.model.submissions

import dev.dres.data.model.run.TaskId

typealias AnswerSetId = String

/**
 * An [AnswerSet] as issued by a DRES user as part of a [Submission].
 *
 * This abstraction is mainly required to enable testability of implementations.
 *
 * @author Luca Rossetto
 * @author Ralph Gasser
 * @version 2.0.0
 */
interface AnswerSet {
    /** The ID of this [AnswerSet]. */
    val id : AnswerSetId

    /** The ID of the task this [AnswerSet] belongs to. */
    val taskId: TaskId

    /** The [Submission] this [AnswerSet] belongs to. */
    val submission: Submission

    /**
     *  The [VerdictStatus] of this [AnswerSet].
     *
     *  @return The [VerdictStatus] of this [AnswerSet].
     */
    fun status(): VerdictStatus

    /**
     * Returns a [Sequence] of [Answer]s for this [AnswerSet].
     */
    fun answers() : Sequence<Answer>
}