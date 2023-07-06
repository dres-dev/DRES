package dev.dres.api.rest.types.template.tasks

import dev.dres.data.model.template.task.DbTargetType

/**
 * The RESTful API equivalent for [DbTargetType].
 *
 * @author Luca Rossetto & Ralph Gasser
 * @version 1.0.0
 */
enum class ApiTargetType {
    JUDGEMENT, JUDGEMENT_WITH_VOTE, MEDIA_ITEM, MEDIA_ITEM_TEMPORAL_RANGE, TEXT;

    /**
     * Converts this [ApiTargetType] to a [DbTargetType] representation. Requires an ongoing transaction.
     *
     * @return [DbTargetType]
     */
    fun toDb(): DbTargetType = when(this) {
        JUDGEMENT -> DbTargetType.JUDGEMENT
        JUDGEMENT_WITH_VOTE -> DbTargetType.JUDGEMENT_WITH_VOTE
        MEDIA_ITEM -> DbTargetType.MEDIA_ITEM
        MEDIA_ITEM_TEMPORAL_RANGE -> DbTargetType.MEDIA_ITEM_TEMPORAL_RANGE
        TEXT -> DbTargetType.TEXT
    }
}
