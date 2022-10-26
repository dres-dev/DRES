package dev.dres.data.model.admin

import dev.dres.api.rest.RestApiRole
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
        val JUDGE by enumField { description = "JDUGE" }
        val ADMIN by enumField { description = "ADMIN" }

        /**
         * Generates and returns the [Role] that corresponds to the given [RestApiRole].
         *
         * @param role [RestApiRole]
         */
        fun fromRestRole(role: RestApiRole): Role = when(role) {
            RestApiRole.ANYONE,
            RestApiRole.VIEWER -> VIEWER
            RestApiRole.PARTICIPANT -> PARTICIPANT
            RestApiRole.JUDGE -> JUDGE
            RestApiRole.ADMIN -> ADMIN
        }
    }

    /** Name / description of the [Role]. */
    var description by xdRequiredStringProp(unique = true)
        private set
}
