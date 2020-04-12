package dres.data.model.run

import kotlinx.serialization.Serializable

/**
 * A [Submission] as received by a competition participant.
 *
 * @author Ralph Gasser & Luca Rossetto
 * @version 1.0
 */
@Serializable
data class Submission(val team: Int, val timestamp: Long, val collection: String, val item: String, val start: Long? = null, val end: Long? = null) {
    var status: SubmissionStatus = SubmissionStatus.INDETERMINATE
}