package dres.data.model.run

import kotlinx.serialization.Serializable

@Serializable
sealed class Submission(open val team: Int, open val timestamp: Long, open val name: String )

class AvsSubmission(team: Int, timestamp: Long, name: String): Submission(team, timestamp, name)

class KisSubmission(team: Int, timestamp: Long, name: String, val start: Long, val end: Long): Submission(team, timestamp, name)