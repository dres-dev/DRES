package dev.dres.api.rest.types.template.tasks

import dev.dres.api.rest.types.collection.time.ApiTemporalRange
import dev.dres.data.model.template.task.DbTaskTemplateTarget

/**
 * The RESTful API equivalent for [DbTaskTemplateTarget].
 *
 * @author Luca Rossetto & Ralph Gasser & Loris Sauter
 * @version 1.1.0
 */
data class ApiTarget(
    val type: ApiTargetType,
    /**
     * The actual target, the semantic of this value depends on the type
     */
    val target: String? = null,
    val range: ApiTemporalRange? = null,
    /**
     * The name of the target, which is defined for certain types (e.g. [ApiTargetType]-MEDIA_TIEM )
     */
    val name: String? = null
)
