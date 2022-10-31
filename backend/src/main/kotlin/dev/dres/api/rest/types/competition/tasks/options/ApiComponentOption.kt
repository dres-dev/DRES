package dev.dres.api.rest.types.competition.tasks.options

import dev.dres.data.model.competition.task.options.HintOption

/**
 * A RESTful API representation of [HintOption].
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
enum class ApiComponentOption(val option: HintOption) {
    IMAGE_ITEM(HintOption.IMAGE_ITEM),
    VIDEO_ITEM_SEGMENT(HintOption.VIDEO_ITEM_SEGMENT),
    TEXT(HintOption.TEXT),
    EXTERNAL_IMAGE(HintOption.EXTERNAL_IMAGE),
    EXTERNAL_VIDEO(HintOption.EXTERNAL_VIDEO)
}