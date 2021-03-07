package dev.dres.run.filter

import dev.dres.data.model.submissions.Submission

/**
 * An exception that is thrown, when a [Submission] is rejected by a [SubmissionFilter].
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class SubmissionRejectedException(s: Submission) : Throwable("Submission $s was rejected by filter.")