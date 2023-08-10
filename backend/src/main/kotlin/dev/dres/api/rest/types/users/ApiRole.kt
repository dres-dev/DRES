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

    companion object {
        /**
         * Parses a [DbRole] instance from a [String].
         */
        fun parse(string: String) = when (string.uppercase()) {
            "VIEWER" -> VIEWER
            "PARTICIPANT" -> PARTICIPANT
            "JUDGE" -> JUDGE
            "ADMIN", "ADMINISTRATOR" -> ADMIN
            else -> throw IllegalArgumentException("Failed to parse role '$string'.")
        }
    }

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