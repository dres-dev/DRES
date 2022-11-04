package dev.dres.api.rest.types.competition.tasks

import dev.dres.data.model.template.task.HintType

/**
 * The RESTful API equivalent for [HintType].
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
enum class ApiHintType(val type: HintType) {
    EMPTY(HintType.EMPTY),
    TEXT(HintType.TEXT),
    VIDEO(HintType.VIDEO),
    IMAGE(HintType.IMAGE)
}