package dev.dres.data.model.submissions

import dev.dres.data.model.PersistentEntity
import dev.dres.data.model.admin.User
import dev.dres.data.model.template.team.Team
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.simple.min
import kotlinx.dnq.xdLink1
import kotlinx.dnq.xdRequiredLongProp

/**
 * A [BatchedSubmission] as submitted by a competition participant and received by DRES.
 *
 * @author Luca Rossetto
 * @version 2.0.0
 */
class BatchedSubmission(entity: Entity) : PersistentEntity(entity) {
    /** The [SubmissionId] of this [User]. */
    var submissionId: SubmissionId
        get() = this.id
        set(value) { this.id = value }

    /** The timestamp of this [Submission]. */
    var timestamp by xdRequiredLongProp { min(0L) }

    /** The [SubmissionStatus] of this [Submission]. */
    var status by xdLink1(SubmissionStatus)

    /** The [Team] that submitted this [Submission] */
    var team by xdLink1(Team)

    /** The [User] that submitted this [Submission] */
    var user by xdLink1(User)
}