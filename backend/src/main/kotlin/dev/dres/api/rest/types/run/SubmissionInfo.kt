package dev.dres.api.rest.types.run

import dev.dres.api.rest.types.collection.RestMediaItem
import dev.dres.data.model.submissions.Submission
import dev.dres.data.model.submissions.SubmissionStatus
import dev.dres.data.model.submissions.aspects.ItemAspect
import dev.dres.data.model.submissions.aspects.TemporalSubmissionAspect
import dev.dres.data.model.submissions.aspects.TextAspect

/**
 * Contains information about a [Submission].
 *
 * @author Luca Rossetto & Ralph Gasser
 * @version 1.1.0
 */
data class SubmissionInfo(
    val id: String? = null,
    val teamId: String,
    val teamName: String? = null,
    val memberId: String,
    val memberName: String? = null,
    val status: SubmissionStatus,
    val timestamp: Long,
    val item: RestMediaItem? = null,
    val text: String? = null,
    val start: Long? = null,
    val end: Long? = null
) {

    /**
     * Constructor that generates a [SubmissionInfo] from a [Submission]. Contains
     * all information, that can be extracted from the [Submission] directly but does not
     * resolve advanced information, such as [teamName] and [memberName].
     *
     * @param submission The [Submission] to convert.
     */
    constructor(submission: Submission) : this(
        id = submission.uid.string,
        teamId = submission.teamId.string,
        memberId = submission.memberId.string,
        status = submission.status,
        timestamp = submission.timestamp,
        item = if (submission is ItemAspect) RestMediaItem.fromMediaItem(submission.item) else null,
        text = if (submission is TextAspect) submission.text else null,
        start = if (submission is TemporalSubmissionAspect) submission.start else null,
        end = if (submission is TemporalSubmissionAspect) submission.end else null
    )

    companion object {
        /**
         * Generates and returns a blind [SubmissionInfo] containing only [teamId], [memberId], [status] and [timestamp]
         *
         * @param submission The [Submission] to convert.
         */
        fun blind(submission: Submission): SubmissionInfo = SubmissionInfo(
            teamId = submission.teamId.string,
            memberId = submission.memberId.string,
            status = submission.status,
            timestamp = submission.timestamp
        )
    }
}
