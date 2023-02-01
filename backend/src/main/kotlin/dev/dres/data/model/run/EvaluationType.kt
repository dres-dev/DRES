package dev.dres.data.model.run

import dev.dres.api.rest.types.evaluation.ApiRunType
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.XdEnumEntity
import kotlinx.dnq.XdEnumEntityType
import kotlinx.dnq.xdRequiredStringProp

/**
 * Enumeration of the type of [Evaluation].
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class EvaluationType(entity: Entity) : XdEnumEntity(entity) {
    companion object : XdEnumEntityType<EvaluationType>() {
        val INTERACTIVE_SYNCHRONOUS by enumField { description = "INTERACTIVE_SYNCHRONOUS" }
        val INTERACTIVE_ASYNCHRONOUS by enumField { description = "INTERACTIVE_ASYNCHRONOUS" }
        val NON_INTERACTIVE by enumField { description = "NON_INTERACTIVE" }
    }

    var description by xdRequiredStringProp(unique = true)
        private set

    /**
     * Converts this [EvaluationType] to a RESTful API representation [ApiRunType].
     *
     * @return [ApiRunType]
     */
    fun toApi() = ApiRunType.values().find { it.type == this } ?: throw IllegalStateException("Run type ${this.description} is not supported.")
}