package dev.dres.api.rest.handler

import dev.dres.api.rest.AccessManager
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.api.rest.types.users.ApiRole
import dev.dres.data.model.admin.UserId
import dev.dres.data.model.run.EvaluationId
import dev.dres.run.RunExecutor
import dev.dres.run.RunManager
import dev.dres.run.RunManagerStatus
import dev.dres.utilities.extensions.sessionId
import io.javalin.http.Context
import kotlinx.dnq.query.filter
import kotlinx.dnq.query.flatMapDistinct
import kotlinx.dnq.query.isEmpty


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
 * Returns the active [RunManager] the used defined in the current [Context] can access.
 *
 * @return [RunManager]
 */
fun Context.activeManagerForUser(): RunManager {
    val userId = this.userId()
    val managers = AccessManager.getRunManagerForUser(userId).filter {
        it.status != RunManagerStatus.CREATED && it.status != RunManagerStatus.TERMINATED
    }
    if (managers.isEmpty()) throw ErrorStatusException(404, "There is currently no eligible competition with an active task.", this)
    if (managers.size > 1) throw ErrorStatusException(409, "More than one possible competition found: ${managers.joinToString { it.template.name }}", this)
    return managers.first()
}

/**
 * Returns the active [RunManager] the [EvaluationId] defined in the current [Context].
 *
 * @return [RunManager]
 */
fun Context.eligibleManagerForId(): RunManager {
    val userId = this.userId()
    val evaluationId = this.evaluationId()
    val manager = RunExecutor.managerForId(evaluationId) ?: throw ErrorStatusException(404, "Evaluation $evaluationId not found.", this)
    if (this.isJudge()) {
        if (manager.template.judges.filter { it.userId eq userId }.isEmpty) {
            throw ErrorStatusException(401, "Current user is not allowed to access evaluation $evaluationId as judge.", this)
        }
    }
    if (this.isParticipant()) {
        if (manager.template.teams.flatMapDistinct { it.users }.filter { it.userId eq userId }.isEmpty) {
            throw ErrorStatusException(401, "Current user is not allowed to access evaluation $evaluationId as participant.", this)
        }
    }
    return manager
}

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
