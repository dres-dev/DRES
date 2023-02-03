package dev.dres.api.rest.types.competition.tasks

import dev.dres.data.model.template.task.DbHintType

/**
 * The RESTful API equivalent for [DbHintType].
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
enum class ApiHintType {
    EMPTY, TEXT, VIDEO, IMAGE;

    /**
     * Converts this [ApiHintType] to a [DbHintType] representation. Requires an ongoing transaction.
     *
     * @return [DbHintType]
     */
    fun toDb(): DbHintType = when(this) {
        EMPTY -> DbHintType.EMPTY
        TEXT -> DbHintType.TEXT
        VIDEO -> DbHintType.VIDEO
        IMAGE -> DbHintType.IMAGE
    }
}