package dev.dres.data.model.competition.task.options

import dev.dres.api.rest.types.competition.tasks.options.ApiComponentOption
import dev.dres.data.model.competition.task.TaskDescription
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.XdEnumEntity
import kotlinx.dnq.XdEnumEntityType
import kotlinx.dnq.xdRequiredStringProp

/**
 * An enumeration of potential options for [TaskDescription] targets.
 *
 * @author Ralph Gasser
 * @version 2.0.0
 */
class HintOption(entity: Entity) : XdEnumEntity(entity) {
    companion object : XdEnumEntityType<HintOption>() {
        val IMAGE_ITEM by enumField { description = "IMAGE_ITEM" }                  /** An image [MediaItem]. */
        val VIDEO_ITEM_SEGMENT by enumField { description = "VIDEO_ITEM_SEGMENT" }  /** Part of a video [MediaItem]. */
        val TEXT by enumField { description = "TEXT" }                              /** A text snippet. */
        val EXTERNAL_IMAGE by enumField { description = "EXTERNAL_IMAGE" }          /** An external image that is not part of a collection. */
        val EXTERNAL_VIDEO by enumField { description = "EXTERNAL_VIDEO" }          /** An external video that is not part of a collection. */
    }

    /** Name / description of the [ScoreOption]. */
    var description by xdRequiredStringProp(unique = true)
        private set

    /**
     * Converts this [HintOption] to a RESTful API representation [ApiComponentOption].
     *
     * @return [ApiComponentOption]
     */
    fun toApi() = ApiComponentOption.values().find { it.option == this } ?: throw IllegalStateException("Option ${this.description} is not supported.")
}