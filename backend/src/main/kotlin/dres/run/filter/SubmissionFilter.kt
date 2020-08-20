package dres.run.filter

import dres.data.model.run.Submission
import java.util.function.Predicate

/**
 * A [Predicate] that can be used to filter [Submission]'s prior to them being processed
 * by the [Submission] evaluation pipeline.
 *
 * @author Ralph Gasser
 * @version 1.0
 */
fun interface SubmissionFilter : Predicate<Submission> {

    override infix fun and(other: Predicate<in Submission>): SubmissionFilter = SubmissionFilter { s -> this@SubmissionFilter.test(s) && other.test(s) }

    override infix fun or(other: Predicate<in Submission>): SubmissionFilter = SubmissionFilter { s -> this@SubmissionFilter.test(s) || other.test(s) }

    operator fun not(): SubmissionFilter = SubmissionFilter { s -> !this@SubmissionFilter.test(s) }

}