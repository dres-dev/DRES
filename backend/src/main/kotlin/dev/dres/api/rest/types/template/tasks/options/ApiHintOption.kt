package dev.dres.api.rest.types.template.tasks.options

import dev.dres.data.model.template.task.options.DbHintOption

/**
 * A RESTful API representation of [DbHintOption].
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
enum class ApiHintOption {
    IMAGE_ITEM, VIDEO_ITEM_SEGMENT, TEXT, EXTERNAL_IMAGE, EXTERNAL_VIDEO;

    /**
     * Converts this [ApiHintOption] to a RESTful API representation [DbHintOption].
     *
     * @return [DbHintOption]
     */
    fun toDb() = when(this) {
        IMAGE_ITEM -> DbHintOption.IMAGE_ITEM
        VIDEO_ITEM_SEGMENT -> DbHintOption.VIDEO_ITEM_SEGMENT
        TEXT -> DbHintOption.TEXT
        EXTERNAL_IMAGE -> DbHintOption.EXTERNAL_IMAGE
        EXTERNAL_VIDEO -> DbHintOption.EXTERNAL_VIDEO
    }
}
