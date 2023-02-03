package dev.dres.data.model.submissions

import dev.dres.api.rest.types.evaluation.ApiVerdictStatus
import dev.dres.data.model.admin.DbRole
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.XdEnumEntity
import kotlinx.dnq.XdEnumEntityType
import kotlinx.dnq.xdRequiredStringProp

/**
 * Status of a [DbSubmission] with respect to its validation.
 *
 * @author Luca Rossetto
 * @version 2.0.0
 */
class DbVerdictStatus(entity: Entity) : XdEnumEntity(entity) {
    companion object : XdEnumEntityType<DbVerdictStatus>() {
        val CORRECT by enumField { description = "CORRECT" }                 /** Submission has been deemed as correct. */
        val WRONG by enumField { description = "WRONG" }
        val INDETERMINATE by enumField { description = "INDETERMINATE" }     /** Submission has been deemed as wrong. */
        val UNDECIDABLE by enumField { description = "UNDECIDABLE" }        /** Submission has not been validated yet. */

        /**
         * Returns a list of all [DbRole] values.
         *
         * @return List of all [DbRole] values.
         */
        fun values() = listOf(CORRECT, WRONG, INDETERMINATE, UNDECIDABLE)

        /**
         * Parses a [DbRole] instance from a [String].
         */
        fun parse(string: String) = when (string.uppercase()) {
            "CORRECT" -> CORRECT
            "WRONG" -> WRONG
            "INDETERMINATE" -> INDETERMINATE
            "UNDECIDABLE" -> UNDECIDABLE
            else -> throw IllegalArgumentException("Failed to parse submission status '$string'.")
        }
    }

    /** Name / description of the [DbAnswerType]. */
    var description by xdRequiredStringProp(unique = true)
        private set

    /**
     * Converts this [DbVerdictStatus] to a RESTful API representation [ApiVerdictStatus].
     *
     * @return [ApiVerdictStatus]
     */
    fun toApi() = ApiVerdictStatus.values().find { it.toDb() == this } ?: throw IllegalStateException("Verdict status ${this.description} is not supported.")

    override fun toString(): String = this.description
}