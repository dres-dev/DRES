package dev.dres.api.rest.types.users

import dev.dres.data.model.admin.Role
import io.javalin.security.RouteRole

/**
 * An enumeration of all roles exposed through the API.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
enum class ApiRole(val role: Role?) : RouteRole {
    ANYONE(null),
    VIEWER(Role.VIEWER),
    PARTICIPANT(Role.PARTICIPANT),
    JUDGE(Role.JUDGE),
    ADMIN(Role.ADMIN)
}