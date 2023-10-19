package dev.dres.data.model.run

import dev.dres.api.rest.types.evaluation.ApiEvaluationType
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.XdEnumEntity
import kotlinx.dnq.XdEnumEntityType
import kotlinx.dnq.xdRequiredStringProp

/**
 * Enumeration of the type of [DbEvaluation].
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class DbEvaluationType(entity: Entity) : XdEnumEntity(entity) {
    companion object : XdEnumEntityType<DbEvaluationType>() {
        val INTERACTIVE_SYNCHRONOUS by enumField { description = "INTERACTIVE_SYNCHRONOUS" }
        val INTERACTIVE_ASYNCHRONOUS by enumField { description = "INTERACTIVE_ASYNCHRONOUS" }
        val NON_INTERACTIVE by enumField { description = "NON_INTERACTIVE" }
    }

    var description by xdRequiredStringProp(unique = true)
        private set

    /**
     * Converts this [DbEvaluationType] to a RESTful API representation [ApiEvaluationType].
     *
     * @return [ApiEvaluationType]
     */
    fun toApi() = ApiEvaluationType.values().find { it.toDb() == this } ?: throw IllegalStateException("Evaluation type ${this.description} is not supported.")
}