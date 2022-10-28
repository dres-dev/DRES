package dev.dres.api.rest.types.users

import dev.dres.data.model.admin.Role
import dev.dres.data.model.admin.User
import dev.dres.utilities.extensions.sessionId
import io.javalin.http.Context

/**
 * A response surrounding manipulation of users.
 *
 * @author Loris Sauter
 * @version 1.0.0
 */
data class UserDetails(val id: String, val username: String, val role: Role, val sessionId: String? = null) {
    companion object {
        fun of(user: User): UserDetails = UserDetails(user.id, user.username, user.role)
        fun create(user: User, ctx: Context): UserDetails = UserDetails(user.id, user.username, user.role, ctx.sessionId())
    }
}
