package dev.dres.data.model.admin

import dev.dres.api.rest.types.users.ApiRole
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.XdEnumEntity
import kotlinx.dnq.XdEnumEntityType
import kotlinx.dnq.xdRequiredStringProp

/**
 * The [Role]s currently supported by DRES.
 *
 * @author Luca Rossetto & Ralph Gasser
 * @version 2.0.0
 */
class Role(entity: Entity) : XdEnumEntity(entity) {
    companion object : XdEnumEntityType<Role>() {
        val VIEWER by enumField { description = "VIEWER" }
        val PARTICIPANT by enumField { description = "PARTICIPANT" }
        val JUDGE by enumField { description = "JUDGE" }
        val ADMIN by enumField { description = "ADMIN" }

        /**
         * Returns a list of all [Role] values.
         *
         * @return List of all [Role] values.
         */
        fun values() = listOf(VIEWER, PARTICIPANT, JUDGE, ADMIN)

        /**
         * Parses a [Role] instance from a [String].
         */
        fun parse(string: String) = when(string.uppercase()) {
            "VIEWER" -> VIEWER
            "PARTICIPANT" -> PARTICIPANT
            "JUDGE" -> JUDGE
            "ADMIN", "ADMINISTRATOR" -> ADMIN
            else -> throw IllegalArgumentException("Failed to parse role '$string'.")
        }
    }

    /** Name / description of the [Role]. */
    var description by xdRequiredStringProp(unique = true)
        private set

    /**
     * Converts this [Role] to a RESTful API representation [ApiRole].
     *
     * @return [ApiRole]
     */
    fun toApi(): ApiRole = ApiRole.values().find { it.getRole() == this } ?: throw IllegalStateException("Role ${this.description} is not supported.")
}
