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
interface SubmissionFilter : Predicate<Submission>