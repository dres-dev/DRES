package dev.dres.run.filter

import dev.dres.api.rest.types.evaluation.submission.ApiClientSubmission
import dev.dres.data.model.submissions.Submission

/**
 * An exception that is thrown, when a [Submission] is rejected by a [SubmissionFilter].
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class SubmissionRejectedException(s: ApiClientSubmission, reason: String) : Throwable("Submission ${s.submissionId} was rejected by filter: $reason")