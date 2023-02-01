package dev.dres.api.rest.types.users

import dev.dres.api.rest.types.collection.ApiMediaType
import dev.dres.data.model.admin.Role
import dev.dres.data.model.media.MediaType
import io.javalin.security.RouteRole

/**
 * An enumeration of all roles exposed through the API.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
enum class ApiRole : RouteRole {
    ANYONE, VIEWER, PARTICIPANT, JUDGE, ADMIN;

    /**
     * Converts this [ApiMediaType] to a [MediaType] representation. Requires an ongoing transaction.
     *
     * @return [MediaType]
     */
    fun toRole(): Role? = when(this) {
        ANYONE -> null
        VIEWER -> Role.VIEWER
        PARTICIPANT -> Role.PARTICIPANT
        JUDGE -> Role.JUDGE
        ADMIN -> Role.ADMIN
    }
}