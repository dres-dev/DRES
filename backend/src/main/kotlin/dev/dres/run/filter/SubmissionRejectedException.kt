package dev.dres.run.filter

import dev.dres.data.model.submissions.DbSubmission

/**
 * An exception that is thrown, when a [DbSubmission] is rejected by a [SubmissionFilter].
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class SubmissionRejectedException(s: DbSubmission, reason: String) : Throwable("Submission ${s.submissionId} was rejected by filter: $reason")