package dev.dres.data.model.run

import dev.dres.api.rest.types.evaluation.ApiEvaluationStatus
import dev.dres.api.rest.types.evaluation.ApiEvaluationType
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.XdEnumEntity
import kotlinx.dnq.XdEnumEntityType
import kotlinx.dnq.xdRequiredStringProp


/**
 * Enumeration of the status of a [DbEvaluation].
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class DbEvaluationStatus(entity: Entity): XdEnumEntity(entity) {
    companion object : XdEnumEntityType<DbEvaluationStatus>() {
        val CREATED by enumField { description = "CREATED" }
        val ACTIVE by enumField { description = "ACTIVE" }
        val TERMINATED by enumField { description = "TERMINATED" }
    }

    var description by xdRequiredStringProp(unique = true)
        private set

    /**
     * Converts this [DbEvaluationType] to a RESTful API representation [ApiEvaluationType].
     *
     * @return [ApiEvaluationType]
     */
    fun toApi() = ApiEvaluationStatus.values().find { it.toDb() == this } ?: throw IllegalStateException("Evaluation status ${this.description} is not supported.")
}