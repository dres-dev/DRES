package dev.dres.api.rest.types.competition.tasks.options

import dev.dres.data.model.template.task.options.DbTargetOption

/**
 * A RESTful API representation of [DbTargetOption].
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
enum class ApiTargetOption{
    SINGLE_MEDIA_ITEM, SINGLE_MEDIA_SEGMENT, JUDGEMENT, VOTE, TEXT;

    /**
     * Converts this [ApiTargetOption] to a [DbTargetOption] representation. Requires an ongoing transaction.
     *
     * @return [DbTargetOption]
     */
    fun toDb(): DbTargetOption = when(this) {
        SINGLE_MEDIA_ITEM -> DbTargetOption.MEDIA_ITEM
        SINGLE_MEDIA_SEGMENT -> DbTargetOption.MEDIA_SEGMENT
        JUDGEMENT -> DbTargetOption.JUDGEMENT
        VOTE -> DbTargetOption.VOTE
        TEXT -> DbTargetOption.TEXT
    }
}