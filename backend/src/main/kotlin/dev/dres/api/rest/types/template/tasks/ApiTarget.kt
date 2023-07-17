package dev.dres.api.rest.types.template.tasks

import dev.dres.api.rest.types.collection.time.ApiTemporalRange
import dev.dres.data.model.template.task.DbTaskTemplateTarget

/**
 * The RESTful API equivalent for [DbTaskTemplateTarget].
 *
 * @author Luca Rossetto & Ralph Gasser
 * @version 1.0.0
 */
data class ApiTarget(val type: ApiTargetType, val target: String? = null, val range: ApiTemporalRange? = null)
