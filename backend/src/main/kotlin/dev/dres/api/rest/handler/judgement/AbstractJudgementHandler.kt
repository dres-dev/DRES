package dev.dres.api.rest.handler.judgement

import dev.dres.api.rest.AccessManager
import dev.dres.api.rest.handler.AccessManagedRestHandler
import dev.dres.api.rest.handler.RestHandler
import dev.dres.api.rest.handler.userId
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.api.rest.types.users.ApiRole
import dev.dres.run.RunManager
import dev.dres.utilities.extensions.sessionId
import io.javalin.http.Context
import io.javalin.security.RouteRole
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.filter
import kotlinx.dnq.query.isEmpty

/**
 *
 * @author Ralph Gasser
 * @version 1.0
 */
abstract class AbstractJudgementHandler(protected val store: TransientEntityStore): RestHandler, AccessManagedRestHandler {
    /** */
    override val permittedRoles: Set<RouteRole> = setOf(ApiRole.JUDGE)

    /** */
    override val apiVersion = "v1"

    /**
     * Checks if [RunManager] can actually be judged by the user defined in the current [Context].
     *
     * @param ctx The [Context] to check eligibility for.
     * @param runManager The [RunManager] to check eligibility for.
     * @throws [ErrorStatusException] if user is not eligible.
     */
    protected fun checkEligibility(ctx: Context, runManager: RunManager) {
        val userId = ctx.userId()
        if (AccessManager.rolesOfSession(ctx.sessionId()).contains(ApiRole.ADMIN)) {
            return //Admins require no further check
        }
        if (runManager.template.judges.filter { it.userId eq userId }.isEmpty) {
            throw ErrorStatusException(403, "Access to specified run is denied.", ctx)
        }
    }
}