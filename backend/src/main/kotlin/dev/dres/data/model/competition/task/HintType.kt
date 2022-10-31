package dev.dres.data.model.competition.task

import dev.dres.api.rest.types.competition.tasks.ApiHintType
import dev.dres.api.rest.types.task.ApiContentType
import dev.dres.data.model.competition.task.options.ScoreOption
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.XdEnumEntity
import kotlinx.dnq.XdEnumEntityType
import kotlinx.dnq.xdBooleanProp
import kotlinx.dnq.xdRequiredStringProp

class HintType(entity: Entity) : XdEnumEntity(entity) {
    companion object : XdEnumEntityType<HintType>() {
        val EMPTY by enumField { description = "EMPTY"; mimeType = ""; suffix = ""; base64 = true }
        val TEXT by enumField { description = "TEXT"; mimeType = "text/plain"; base64 = false }
        val VIDEO by enumField { description = "VIDEO"; mimeType = "video/mp4"; base64 = true }
        val IMAGE by enumField {  description = "IMAGE"; mimeType = "image/jpg"; base64 = true  }

    }

    /** Name / description of the [ScoreOption]. */
    var description by xdRequiredStringProp(unique = true)
        private set

    /** Name / description of the [ScoreOption]. */
    var mimeType by xdRequiredStringProp(unique = true)
        private set

    /** Name / description of the [ScoreOption]. */
    var suffix by xdRequiredStringProp(unique = true)

    /** Name / description of the [ScoreOption]. */
    var base64 by xdBooleanProp()

    /**
     * Converts this [HintType] to the RESTful API representation [ApiContentType].
     *
     * @return [ApiContentType] equivalent to this [HintType].
     */
    fun toApi(): ApiHintType
            = ApiHintType.values().find { it.type == this } ?: throw IllegalStateException("Hint type ${this.description} is not supported.")
}