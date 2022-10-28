package dev.dres.data.model.competition.task

import dev.dres.api.rest.types.task.ApiContentType
import dev.dres.data.model.competition.task.options.TaskScoreOption
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.XdEnumEntity
import kotlinx.dnq.XdEnumEntityType
import kotlinx.dnq.xdRequiredStringProp

/**
 *
 */
class TargetType(entity: Entity): XdEnumEntity(entity) {
    companion object : XdEnumEntityType<TargetType>() {
        val JUDGEMENT by enumField { description = "JUDGEMENT" }
        val JUDGEMENT_WITH_VOTE by enumField { description = "JUDGEMENT_WITH_VOTE" }
        val MEDIA_ITEM by enumField { description = "MEDIA_ITEM" }
        val MEDIA_ITEM_TEMPORAL_RANGE by enumField { description = "MEDIA_ITEM_TEMPORAL_RANGE" }
        val TEXT by enumField { description = "EXTERNAL_IMAGE" }
    }

    /** Name / description of the [TaskScoreOption]. */
    var description by xdRequiredStringProp(unique = true)
        private set

    /**
     * Converts this [HintType] to the RESTful API representation [ApiContentType].
     *
     * @return [ApiContentType] equivalent to this [HintType].
     */
    fun toApi() = when(this) {
        JUDGEMENT,
        JUDGEMENT_WITH_VOTE  -> ApiContentType.EMPTY
        MEDIA_ITEM -> ApiContentType.IMAGE
        MEDIA_ITEM_TEMPORAL_RANGE -> ApiContentType.VIDEO
        TEXT -> ApiContentType.TEXT
        else -> throw IllegalStateException("The target type ${this.description} is not supported.")
    }
}