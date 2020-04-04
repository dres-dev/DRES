package dres.data.model.run

import kotlinx.serialization.Serializable

@Serializable
sealed class Submission(open val team: Int, open val timestamp: Long, val collection: String, open val item: String, val type: SubmissionType, var status: SubmissionStatus = SubmissionStatus.INDETERMINATE)


class LSCSubmission(team: Int, timestamp: Long, collection: String, item: String): Submission(team, timestamp, collection, item, SubmissionType.LSC)

class VBSSubmission(team: Int, timestamp: Long, collection: String, item: String, val start: Long, val end: Long): Submission(team, timestamp, collection, item, SubmissionType.VBS)



enum class SubmissionStatus {
    CORRECT, WRONG, INDETERMINATE, UNDECIDABLE
}