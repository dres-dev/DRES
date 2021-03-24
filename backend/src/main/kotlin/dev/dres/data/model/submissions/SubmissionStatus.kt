package dev.dres.data.model.submissions

/**
 * Status of a [Submission] with respect to its validation.
 *
 * @author Luca Rossetto
 * @version 1.0
 */
enum class SubmissionStatus {
    CORRECT,        /** Submission has been deemed as correct. */
    WRONG,          /** Submission has been deemed as wrong. */
    INDETERMINATE,  /** Submission has not been validated yet. */
    UNDECIDABLE
}