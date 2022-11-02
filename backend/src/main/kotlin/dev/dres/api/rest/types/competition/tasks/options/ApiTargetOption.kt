package dev.dres.api.rest.types.competition.tasks.options

import dev.dres.data.model.competition.task.options.TargetOption

/**
 * A RESTful API representation of [TargetOption].
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
enum class ApiTargetOption(val option: TargetOption){
    SINGLE_MEDIA_ITEM(TargetOption.MEDIA_ITEM),
    SINGLE_MEDIA_SEGMENT(TargetOption.MEDIA_SEGMENT),
    MULTIPLE_MEDIA_ITEMS(TargetOption.MULTIPLE_MEDIA_ITEMS),
    JUDGEMENT(TargetOption.JUDGEMENT),
    VOTE(TargetOption.VOTE),
    TEXT(TargetOption.TEXT)
}