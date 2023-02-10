package dev.dres.data.model.submissions

import dev.dres.api.rest.types.evaluation.ApiAnswerType
import dev.dres.data.model.admin.DbRole
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.XdEnumEntity
import kotlinx.dnq.XdEnumEntityType
import kotlinx.dnq.xdRequiredStringProp

/**
 * The type of [DbAnswerSet] with respect to its content
 *
 * @author Ralph Gasser & Luca Rossetto
 * @version 1.1.0
 */
class DbAnswerType(entity: Entity) : XdEnumEntity(entity) {
    companion object : XdEnumEntityType<DbAnswerType>() {
        val ITEM by enumField { description = AnswerType.ITEM.name }
        val TEMPORAL by enumField { description = AnswerType.TEMPORAL.name }
        val TEXT by enumField { description = AnswerType.TEXT.name }

        /**
         * Returns a list of all [DbRole] values.
         *
         * @return List of all [DbRole] values.
         */
        fun values() = listOf(ITEM, TEMPORAL, TEXT)

        /**
         * Parses a [DbRole] instance from a [String].
         */
        fun parse(string: String) = when (string.uppercase()) {
            AnswerType.ITEM.name -> ITEM
            AnswerType.TEMPORAL.name -> TEMPORAL
            AnswerType.TEXT.name -> TEXT
            else -> throw IllegalArgumentException("Failed to parse submission type '$string'.")
        }
    }

    /** Name / description of the [DbAnswerType]. */
    var description by xdRequiredStringProp(unique = true)
        private set

    /**
     * Converts this [DbAnswerType] to a RESTful API representation [DbAnswerType].
     *
     * @return [DbAnswerType]
     */
    fun toApi() = AnswerType.fromDb(this).toApi()

}