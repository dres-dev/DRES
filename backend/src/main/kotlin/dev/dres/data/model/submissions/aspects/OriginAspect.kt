package dev.dres.data.model.submissions.aspects

import dev.dres.data.model.UID
import dev.dres.data.model.competition.team.TeamId

/**
 *
 * @author Luca Rossetto
 * @version 1.0.0
 */
interface OriginAspect {
    val uid: UID
    val teamId: TeamId
    val memberId: UID
}