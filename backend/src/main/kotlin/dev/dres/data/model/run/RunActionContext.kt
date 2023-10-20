package dev.dres.data.model.run

import dev.dres.api.rest.AccessManager
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.api.rest.types.users.ApiRole
import dev.dres.data.model.admin.DbRole
import dev.dres.data.model.admin.UserId
import dev.dres.data.model.run.interfaces.EvaluationId
import dev.dres.data.model.template.team.TeamId
import dev.dres.run.RunManager
import dev.dres.utilities.extensions.sessionToken
import io.javalin.http.Context

/**
 * The [RunActionContext] captures and encapsulates information usually required during the interaction with a [RunManager].
 * It exposes information available to the OpenAPI facility (e.g., through session management) to the [RunManager].
 *
 * @author Luca Rossetto
 * @author Ralph Gasser
 * @version 2.0.0
 */
open class RunActionContext(val userId: UserId, var evaluationId: EvaluationId?, val roles: Set<ApiRole>) {

    /** True if the user associated with this [RunActionContext] acts as [DbRole.ADMIN]*/
    val isAdmin: Boolean
        get() = this.roles.contains(ApiRole.ADMIN)

    companion object {
        /** A static [RunActionContext] used for internal invocations by DRES. Always acts as an implicit [DbRole.ADMIN]. */
        val INTERNAL = RunActionContext("SYSTEM",null, setOf(ApiRole.ADMIN))

        /**
         * Constructs a [RunActionContext] from this [Context].
         *
         * @return ctx [RunActionContext]
         */
        fun Context.runActionContext() = RunActionContext(
            AccessManager.userIdForSession(this.sessionToken()) ?: throw ErrorStatusException(403, "Unauthorized user.", this),
            this.pathParamMap()["evaluationId"],
            AccessManager.rolesOfSession(this.sessionToken()).toSet()
        )
    }
}
