package dev.dres.data.model.admin

import dev.dres.api.rest.types.users.ApiRole
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.XdEnumEntity
import kotlinx.dnq.XdEnumEntityType
import kotlinx.dnq.xdRequiredStringProp

/**
 * The [DbRole]s currently supported by DRES.
 *
 * @author Luca Rossetto & Ralph Gasser
 * @version 2.0.0
 */
class DbRole(entity: Entity) : XdEnumEntity(entity) {
    companion object : XdEnumEntityType<DbRole>() {
        val VIEWER by enumField { description = "VIEWER" }
        val PARTICIPANT by enumField { description = "PARTICIPANT" }
        val JUDGE by enumField { description = "JUDGE" }
        val ADMIN by enumField { description = "ADMIN" }

        /**
         * Returns a list of all [DbRole] values.
         *
         * @return List of all [DbRole] values.
         */
        fun values() = listOf(VIEWER, PARTICIPANT, JUDGE, ADMIN)


    }

    /** Name / description of the [DbRole]. */
    var description by xdRequiredStringProp(unique = true)
        private set

    /**
     * Converts this [DbRole] to a RESTful API representation [ApiRole].
     *
     * @return [ApiRole]
     */
    fun toApi(): ApiRole = ApiRole.values().find { it.toDb() == this } ?: throw IllegalStateException("Role ${this.description} is not supported.")

    override fun toString() = this.description
}
