package dev.dres.data.model.submissions.aspects

import dev.dres.data.model.UID

/**
 *
 * @author Luca Rossetto
 * @version 1.0.0
 */
interface OriginAspect {
    val uid: UID
    val teamId: UID
    val memberId: UID
}