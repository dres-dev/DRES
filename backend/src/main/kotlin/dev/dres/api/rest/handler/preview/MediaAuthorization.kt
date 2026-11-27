package dev.dres.api.rest.handler.preview

import dev.dres.api.rest.AccessManager
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.api.rest.types.users.ApiRole
import dev.dres.data.model.media.CollectionId
import dev.dres.run.InteractiveRunManager
import dev.dres.run.RunExecutor
import dev.dres.run.RunManagerStatus
import dev.dres.utilities.extensions.realIP
import dev.dres.utilities.extensions.sessionToken
import io.javalin.http.Context
import org.slf4j.LoggerFactory

/**
 * Authorises access to *media bytes*: whole originals, cached previews and external files.
 *
 * The viewer endpoints (`evaluation/{id}/...`) answer "may this user see the state of this run?". The
 * media endpoints answer a sharper question: "may this user receive these bytes, here and now?". The
 * role alone is not an answer, because every authenticated participant and judge holds
 * [ApiRole.VIEWER] as well; without a run-level check a participant whose team is not even taking part
 * could walk `media/{itemId}` or `preview/{itemId}/{timestamp}` and reconstruct any media item.
 *
 * The rule implemented here mirrors the run's own viewer policy (see
 * `GetTaskHintHandler.executeGatekeeper`), as decided for this codebase:
 *
 *  - `ADMIN` may always fetch media.
 *  - `JUDGE` is treated exactly like a viewer of the runs it judges.
 *  - `PARTICIPANT` is treated exactly like a viewer *iff* the run it belongs to sets
 *    `participantCanView`; otherwise all its media access is denied.
 *  - `VIEWER` may fetch the media of the runs it observes.
 *  - Only [RunManagerStatus.ACTIVE] runs grant anything.
 *
 * A media item is reachable while it belongs to the collection of a task template of at least one such
 * run. That is the finest scoping the media endpoints can express without threading a task id through
 * every URL; the per-task, single-payload bound remains the job of
 * [MediaGrant][dev.dres.api.rest.handler.preview.MediaGrant], which is the path viewers normally take.
 *
 * All functions are stateless derivations of the request [Context]: they read the session maps and the
 * snapshot a [InteractiveRunManager] holds, so no database transaction is required.
 */
object MediaAuthorization {

    private val logger = LoggerFactory.getLogger("dev.dres.media")

    /**
     * Authorises a request for media of the collection [collectionId], i.e. an item original or a preview
     * of one of its items.
     *
     * @param ctx The request [Context], used for the session and for error reporting.
     * @param collectionId The collection the requested media belongs to.
     * @param subject Short description used in the denial log (item id or route), never a session token.
     * @throws ErrorStatusException 401 without a session, 403 when the session may not receive this media.
     */
    fun enforceMediaAccess(ctx: Context, collectionId: CollectionId?, subject: String) {
        val session = ctx.sessionToken()
            ?: throw ErrorStatusException(401, "Access to media requires an authenticated session.", ctx)
        if (isAdmin(session)) {
            return
        }

        val runs = eligibleRuns(session)
        if (runs.isEmpty()) {
            throw denied(ctx, session, subject, "no active run grants media access to this session")
        }

        val reachable = collectionId != null && runs.any { run ->
            run.template.tasks.any { it.collectionId == collectionId }
        }
        if (!reachable) {
            throw denied(ctx, session, subject, "media is not part of any run this session may observe")
        }
    }

    /**
     * Authorises a request for the external media file [file].
     *
     * External files are not part of a collection, so the check is reference based: the caller must be
     * able to observe at least one active run whose task templates reference the file from a hint.
     *
     * @param ctx The request [Context].
     * @param file The resolved file name inside `DRES.EXTERNAL_ROOT`.
     * @throws ErrorStatusException 401 without a session, 403 when no eligible run references the file.
     */
    fun enforceExternalMediaAccess(ctx: Context, file: String) {
        val session = ctx.sessionToken()
            ?: throw ErrorStatusException(401, "Access to media requires an authenticated session.", ctx)
        if (isAdmin(session)) {
            return
        }

        val runs = eligibleRuns(session)
        if (runs.isEmpty()) {
            throw denied(ctx, session, file, "no active run grants media access to this session")
        }

        val referenced = runs.any { run ->
            run.template.tasks.any { task -> task.hints.any { it.path == file } }
        }
        if (!referenced) {
            throw denied(ctx, session, file, "external media is not referenced by any run this session may observe")
        }
    }

    /**
     * Returns the runs through which [session] may reach media, already filtered by its roles and by
     * `participantCanView`. Only [RunManagerStatus.ACTIVE] runs are considered.
     *
     * @param session The session to resolve.
     * @return List of eligible [InteractiveRunManager]s, possibly empty.
     */
    private fun eligibleRuns(session: String): List<InteractiveRunManager> {
        val roles = AccessManager.rolesOfSession(session)
        val userId = AccessManager.userIdForSession(session) ?: return emptyList()

        return RunExecutor.managers().filterIsInstance<InteractiveRunManager>().filter { run ->
            if (run.status != RunManagerStatus.ACTIVE) {
                return@filter false
            }
            /* Viewers and judges must be registered for the run; participants must belong to a team. */
            val isJudge = roles.contains(ApiRole.JUDGE) && run.template.judges.contains(userId)
            val isViewer = roles.contains(ApiRole.VIEWER) && run.template.viewers.contains(userId)
            val isParticipant = roles.contains(ApiRole.PARTICIPANT) && run.template.hasParticipant(userId)

            when {
                /* A judge counts as a viewer. */
                isJudge || isViewer -> true
                /* A participant counts as a viewer only when the run allows participants to view. */
                isParticipant -> run.runProperties.participantCanView
                else -> false
            }
        }
    }

    private fun isAdmin(session: String): Boolean =
        AccessManager.rolesOfSession(session).contains(ApiRole.ADMIN)

    /**
     * Logs a denial and builds the exception to throw. The log line records who asked for what but never
     * the session token itself.
     */
    private fun denied(ctx: Context, session: String, subject: String, reason: String): ErrorStatusException {
        logger.warn(
            "Denied media access to {} ({}) for user {} from {}: {}",
            ctx.path(),
            subject,
            AccessManager.userIdForSession(session),
            ctx.realIP(),
            reason
        )
        return ErrorStatusException(403, "Access to this media is not permitted in the current context.", ctx)
    }
}
