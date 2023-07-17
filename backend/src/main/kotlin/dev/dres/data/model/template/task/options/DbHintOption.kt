package dev.dres.data.model.template.task.options

import dev.dres.api.rest.types.template.tasks.options.ApiHintOption
import dev.dres.data.model.template.task.DbTaskTemplate
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.XdEnumEntity
import kotlinx.dnq.XdEnumEntityType
import kotlinx.dnq.xdRequiredStringProp

/**
 * An enumeration of potential options for [DbTaskTemplate] targets.
 *
 * @author Ralph Gasser
 * @version 2.0.0
 */
class DbHintOption(entity: Entity) : XdEnumEntity(entity) {
    companion object : XdEnumEntityType<DbHintOption>() {
        val IMAGE_ITEM by enumField { description = "IMAGE_ITEM" }                  /** An image [MediaItem]. */
        val VIDEO_ITEM_SEGMENT by enumField { description = "VIDEO_ITEM_SEGMENT" }  /** Part of a video [MediaItem]. */
        val TEXT by enumField { description = "TEXT" }                              /** A text snippet. */
        val EXTERNAL_IMAGE by enumField { description = "EXTERNAL_IMAGE" }          /** An external image that is not part of a collection. */
        val EXTERNAL_VIDEO by enumField { description = "EXTERNAL_VIDEO" }          /** An external video that is not part of a collection. */
    }

    /** Name / description of the [DbScoreOption]. */
    var description by xdRequiredStringProp(unique = true)
        private set

    /**
     * Converts this [DbHintOption] to a RESTful API representation [ApiHintOption].
     *
     * @return [ApiHintOption]
     */
    fun toApi() = ApiHintOption.values().find { it.toDb() == this } ?: throw IllegalStateException("Option ${this.description} is not supported.")
}
