package dev.dres.data.model.submissions

import dev.dres.api.rest.types.evaluation.ApiVerdictType
import dev.dres.data.model.admin.Role
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.XdEnumEntity
import kotlinx.dnq.XdEnumEntityType
import kotlinx.dnq.xdRequiredStringProp

/**
 * The type of [Verdict] with respect to its content
 *
 * @author Ralph Gasser & Luca Rossetto
 * @version 1.1.0
 */
class VerdictType(entity: Entity) : XdEnumEntity(entity) {
    companion object : XdEnumEntityType<VerdictType>() {
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

    /** Name / description of the [VerdictType]. */
    var description by xdRequiredStringProp(unique = true)
        private set

    /**
     * Converts this [VerdictType] to a RESTful API representation [VerdictType].
     *
     * @return [VerdictType]
     */
    fun toApi() = ApiVerdictType.values().find { it.type == this } ?: throw IllegalStateException("Verdict type ${this.description} is not supported.")
}