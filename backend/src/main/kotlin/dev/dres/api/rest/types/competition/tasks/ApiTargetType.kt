package dev.dres.api.rest.types.competition.tasks

import dev.dres.data.model.template.task.TargetType

/**
 * The RESTful API equivalent for [TargetType].
 *
 * @author Luca Rossetto & Ralph Gasser
 * @version 1.0.0
 */
enum class ApiTargetType {
    JUDGEMENT, JUDGEMENT_WITH_VOTE, MEDIA_ITEM, MEDIA_ITEM_TEMPORAL_RANGE, TEXT;

    /**
     * Converts this [ApiTargetType] to a [TargetType] representation. Requires an ongoing transaction.
     *
     * @return [TargetType]
     */
    fun toTargetType(): TargetType = when(this) {
        JUDGEMENT -> TargetType.JUDGEMENT
        JUDGEMENT_WITH_VOTE -> TargetType.JUDGEMENT_WITH_VOTE
        MEDIA_ITEM -> TargetType.MEDIA_ITEM
        MEDIA_ITEM_TEMPORAL_RANGE -> TargetType.MEDIA_ITEM_TEMPORAL_RANGE
        TEXT -> TargetType.TEXT
    }
}