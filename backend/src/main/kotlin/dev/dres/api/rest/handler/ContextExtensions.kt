package dev.dres.api.rest.handler

import dev.dres.api.rest.AccessManager
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.api.rest.types.users.ApiRole
import dev.dres.data.model.admin.UserId
import dev.dres.data.model.run.EvaluationId
import dev.dres.utilities.extensions.sessionId
import io.javalin.http.Context


/**
 * Returns the [UserId] for the current [Context].
 *
 * @return [UserId]
 */
fun Context.userId(): UserId = AccessManager.userIdForSession(this.sessionId())
    ?: throw ErrorStatusException(401, "No user registered for session ${this.sessionId()}.", this)

/**
 * Returns the [EvaluationId] associated with the current [Context]
 *
 * @return [EvaluationId]
 */
fun Context.evaluationId(): EvaluationId
    = this.pathParamMap()["evaluationId"] ?: throw ErrorStatusException(400, "Parameter 'evaluationId' is missing!'", this)

/**
 * Checks uf user associated with current [Context] has [ApiRole.PARTICIPANT].
 *
 * @return True if current user has [ApiRole.PARTICIPANT]
 */
fun Context.isParticipant(): Boolean {
    val roles = AccessManager.rolesOfSession(this.sessionId())
    return roles.contains(ApiRole.PARTICIPANT) && !roles.contains(ApiRole.ADMIN)
}

/**
 * Checks uf user associated with current [Context] has [ApiRole.JUDGE].
 *
 * @return True if current user has [ApiRole.JUDGE]
 */
fun Context.isJudge(): Boolean {
    val roles = AccessManager.rolesOfSession(this.sessionId())
    return roles.contains(ApiRole.JUDGE) && !roles.contains(ApiRole.ADMIN)
}
