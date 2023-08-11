package dev.dres.api.rest.handler.evaluation.client

import dev.dres.api.rest.handler.AccessManagedRestHandler
import dev.dres.api.rest.handler.RestHandler
import dev.dres.utilities.extensions.userId
import dev.dres.api.rest.types.users.ApiRole
import dev.dres.run.RunExecutor
import dev.dres.run.RunManager
import io.javalin.http.Context
import io.javalin.security.RouteRole
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.*

/**
 *
 * @author Ralph Gasser
 * @version 1.0
 */
abstract class AbstractEvaluationClientHandler : RestHandler, AccessManagedRestHandler {
    /** By default [AbstractEvaluationClientHandler] can only be used by [ApiRole.VIEWER]. */
    override val permittedRoles: Set<RouteRole> = setOf(ApiRole.VIEWER)

    /** All [AbstractEvaluationClientHandler]s are part of the v1 API. */
    override val apiVersion = "v2"

    /**
     * Returns the [RunManager] associated with the current [Context].
     *
     * @param ctx The request [Context]
     * @return [RunManager] or null
     */
    fun getRelevantManagers(ctx: Context): List<RunManager> =
        RunExecutor.managers().filter { m -> m.template.teams.any { t -> t.users.any { u -> u.id == ctx.userId() } } }
}
