package dev.dres.data.model.submissions

import dev.dres.api.rest.types.evaluation.submission.ApiVerdictStatus
import dev.dres.data.model.admin.DbRole
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.XdEnumEntity
import kotlinx.dnq.XdEnumEntityType
import kotlinx.dnq.xdRequiredStringProp

/**
 * Status of a [DbSubmission] with respect to its validation.
 *
 * @author Luca Rossetto
 * @author Loris Sauter
 * @version 2.0.0
 */
class DbVerdictStatus(entity: Entity) : XdEnumEntity(entity) {
    companion object : XdEnumEntityType<DbVerdictStatus>() {
        /** Submission has been deemed as correct. */
        val CORRECT by enumField { description = VerdictStatus.CORRECT.name }
        /** Submission has been deemed as wrong. */
        val WRONG by enumField { description = VerdictStatus.WRONG.name }
        /** Submission has not been validated yet. */
        val INDETERMINATE by enumField { description = VerdictStatus.INDETERMINATE.name }
        /** Submission has been deemed as undecidable. The semantic of this depends on the consumer of this information */
        val UNDECIDABLE by enumField { description = VerdictStatus.UNDECIDABLE.name }

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
            VerdictStatus.CORRECT.name -> CORRECT
            VerdictStatus.WRONG.name -> WRONG
            VerdictStatus.INDETERMINATE.name -> INDETERMINATE
            VerdictStatus.UNDECIDABLE.name  -> UNDECIDABLE
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
    fun toApi() = VerdictStatus.fromDb(this).toApi()

    override fun toString(): String = this.description


}
