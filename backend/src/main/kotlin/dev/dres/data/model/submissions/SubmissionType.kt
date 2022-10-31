package dev.dres.data.model.submissions

import dev.dres.data.model.admin.Role
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.XdEnumEntity
import kotlinx.dnq.XdEnumEntityType
import kotlinx.dnq.xdRequiredStringProp

/**
 *
 */
class SubmissionType(entity: Entity) : XdEnumEntity(entity) {
    companion object : XdEnumEntityType<SubmissionType>() {
        val ITEM by enumField { description = "ITEM" }
        val TEMPORAL by enumField { description = "TEMPORAL" }
        val TEXT by enumField { description = "TEXT" }

        /**
         * Returns a list of all [Role] values.
         *
         * @return List of all [Role] values.
         */
        fun values() = listOf(ITEM, TEMPORAL, TEXT)

        /**
         * Parses a [Role] instance from a [String].
         */
        fun parse(string: String) = when (string.uppercase()) {
            "ITEM" -> ITEM
            "TEMPORAL" -> TEMPORAL
            "TEXT" -> TEXT
            else -> throw IllegalArgumentException("Failed to parse submission type '$string'.")
        }
    }

    /** Name / description of the [SubmissionType]. */
    var description by xdRequiredStringProp(unique = true)
        private set
}