package dev.dres.api.rest.handler.evaluation.admin

import dev.dres.api.rest.AccessManager
import dev.dres.api.rest.handler.AccessManagedRestHandler
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.api.rest.types.users.ApiRole
import dev.dres.data.model.run.interfaces.EvaluationId
import dev.dres.run.InteractiveAsynchronousRunManager
import dev.dres.run.InteractiveRunManager
import dev.dres.run.RunExecutor
import dev.dres.run.RunManager
import dev.dres.utilities.extensions.sessionToken
import io.javalin.http.Context
import io.javalin.security.RouteRole
import jetbrains.exodus.database.TransientEntityStore

/**
 *
 * @author Ralph Gasser
 * @version 1.0
 */
abstract class AbstractEvaluationAdminHandler : AccessManagedRestHandler {

    /** By default [AbstractEvaluationAdminHandler] can only be used by [ApiRole.ADMIN]. */
    override val permittedRoles: Set<RouteRole> = setOf(ApiRole.ADMIN)

   /** All [AbstractEvaluationAdminHandler]s are part of the v1 API. */
    override val apiVersion = "v2"

    /**
     * Obtains the [InteractiveRunManager] for the given [EvaluationId].
     *
     * @param evaluationId The [EvaluationId] identifying the [InteractiveRunManager].
     * @return [InteractiveRunManager] or null.
     */
    protected fun getManager(evaluationId: EvaluationId): InteractiveRunManager? {
        val run = RunExecutor.managerForId(evaluationId)
        if (run != null && run is InteractiveRunManager) {
            return run
        }
        return null
    }

    /**
     * Ensures that only [ApiRole.ADMIN] are able to modify the state of [InteractiveRunManager]
     */
    fun synchronousAdminCheck(manager: RunManager, ctx: Context) {
        if (manager is InteractiveAsynchronousRunManager) return
        if (!AccessManager.rolesOfSession(ctx.sessionToken()).contains(ApiRole.ADMIN)) {
            throw ErrorStatusException(403, "Access Denied.", ctx);
        }
    }
}
