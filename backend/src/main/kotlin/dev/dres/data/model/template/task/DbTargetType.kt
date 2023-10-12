package dev.dres.data.model.template.task

import dev.dres.api.rest.types.template.tasks.ApiTargetType
import dev.dres.data.model.template.task.options.DbScoreOption
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.XdEnumEntity
import kotlinx.dnq.XdEnumEntityType
import kotlinx.dnq.xdRequiredStringProp

/**
 * The type of target for a
 */
class DbTargetType(entity: Entity): XdEnumEntity(entity) {
    companion object : XdEnumEntityType<DbTargetType>() {
        val JUDGEMENT by enumField { description = "JUDGEMENT" }
        val JUDGEMENT_WITH_VOTE by enumField { description = "JUDGEMENT_WITH_VOTE" }
        val MEDIA_ITEM by enumField { description = "MEDIA_ITEM" }
        val MEDIA_ITEM_TEMPORAL_RANGE by enumField { description = "MEDIA_ITEM_TEMPORAL_RANGE" }
        val TEXT by enumField { description = "EXTERNAL_IMAGE" }
    }

    /** Name / description of the [DbScoreOption]. */
    var description by xdRequiredStringProp(unique = true)
        private set

    /**
     * Converts this [DbTargetType] to a RESTful API representation [ApiTargetType].
     *
     * @return [ApiTargetType]
     */
    fun toApi(): ApiTargetType
        = ApiTargetType.values().find { it.toDb() == this } ?: throw IllegalStateException("Target type ${this.description} is not supported.")

    override fun toString(): String = this.description
}
