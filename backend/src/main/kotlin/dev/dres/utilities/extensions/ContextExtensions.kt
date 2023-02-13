package dev.dres.utilities.extensions

import dev.dres.api.rest.AccessManager
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.api.rest.types.users.ApiRole
import dev.dres.api.rest.util.MimeTypeHelper
import dev.dres.data.model.admin.UserId
import dev.dres.data.model.run.interfaces.EvaluationId
import dev.dres.run.RunExecutor
import dev.dres.run.RunManager
import dev.dres.run.RunManagerStatus
import io.javalin.http.Context
import kotlinx.dnq.query.filter
import kotlinx.dnq.query.flatMapDistinct
import kotlinx.dnq.query.isEmpty
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.random.Random

fun Context.errorResponse(status: Int, errorMessage: String) {
    this.status(status)
    this.json(ErrorStatus(errorMessage))
}

fun Context.errorResponse(error: ErrorStatusException) {
    this.status(error.statusCode)
    this.json(error.errorStatus)
}

fun Context.streamFile(file: File) {
    if (!file.exists()){
        this.errorResponse(404, "'${file.name}' not found")
        return
    }
    val mimeType = MimeTypeHelper.mimeType(file)
    this.contentType(mimeType) //needs to be set, probably a bug in Javalin
    this.writeSeekableStream(file.inputStream(), mimeType)
}

fun Context.streamFile(path: Path) {
    if (!Files.exists(path)){
        this.errorResponse(404, "File $path not found!")
        return
    }
    val mimeType = MimeTypeHelper.mimeType(path.toFile())
    this.contentType(mimeType)
    this.writeSeekableStream(Files.newInputStream(path, StandardOpenOption.READ), mimeType)
}

fun Context.sendFile(path: Path) {
    if (!Files.exists(path)){
        this.errorResponse(404, "File $path not found!")
        return
    }
    val mimeType = MimeTypeHelper.mimeType(path.toFile())
    this.contentType(mimeType)
    this.result(Files.newInputStream(path, StandardOpenOption.READ))
}

fun Context.sendFile(file: File) {
    if (!file.exists()){
        this.errorResponse(404, "'${file.name}' not found")
        return
    }
    val mimeType = MimeTypeHelper.mimeType(file)
    this.contentType(mimeType)
    this.result(file.inputStream())
}

fun Context.sessionToken(): String? = this.attribute<String>("session")

fun Context.getOrCreateSessionToken(): String {
    val attributeId = this.attribute<String>("session")
    if (attributeId != null) {
        return attributeId
    }

    val random = Random(System.nanoTime())
    val id = List(AccessManager.SESSION_TOKEN_LENGTH) { AccessManager.SESSION_TOKEN_CHAR_POOL.random(random) }.joinToString("")

    this.attribute("session", id)

    return id

}

/**
 * Returns the [UserId] for the current [Context].
 *
 * @return [UserId]
 */
fun Context.userId(): UserId = AccessManager.userIdForSession(this.sessionToken())
    ?: throw ErrorStatusException(401, "No user registered for session ${this.sessionToken()}.", this)

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

    if (this.isAdmin()) {
        return manager
    }

    throw ErrorStatusException(401, "Current user is not allowed to access evaluation $evaluationId as participant.", this)
}
/**
 * Checks uf user associated with current [Context] has [ApiRole.PARTICIPANT].
 *
 * @return True if current user has [ApiRole.PARTICIPANT]
 */
fun Context.isAdmin(): Boolean {
    val roles = AccessManager.rolesOfSession(this.sessionToken())
    return roles.contains(ApiRole.ADMIN)
}

/**
 * Checks uf user associated with current [Context] has [ApiRole.PARTICIPANT].
 *
 * @return True if current user has [ApiRole.PARTICIPANT]
 */
fun Context.isParticipant(): Boolean {
    val roles = AccessManager.rolesOfSession(this.sessionToken())
    return roles.contains(ApiRole.PARTICIPANT) && !roles.contains(ApiRole.ADMIN)
}

/**
 * Checks uf user associated with current [Context] has [ApiRole.JUDGE].
 *
 * @return True if current user has [ApiRole.JUDGE]
 */
fun Context.isJudge(): Boolean {
    val roles = AccessManager.rolesOfSession(this.sessionToken())
    return roles.contains(ApiRole.JUDGE) && !roles.contains(ApiRole.ADMIN)
}
