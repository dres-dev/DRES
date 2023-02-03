package dev.dres.api.rest.types.users

import dev.dres.api.rest.types.collection.ApiMediaType
import dev.dres.data.model.admin.DbRole
import dev.dres.data.model.media.DbMediaType
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
     * Converts this [ApiMediaType] to a [DbMediaType] representation. Requires an ongoing transaction.
     *
     * @return [DbMediaType]
     */
    fun toDb(): DbRole? = when(this) {
        ANYONE -> null
        VIEWER -> DbRole.VIEWER
        PARTICIPANT -> DbRole.PARTICIPANT
        JUDGE -> DbRole.JUDGE
        ADMIN -> DbRole.ADMIN
    }
}