package dev.dres.api.rest.types.competition.tasks

import dev.dres.api.rest.types.collection.time.ApiTemporalRange
import dev.dres.data.model.template.task.TaskTemplateTarget

/**
 * The RESTful API equivalent for [TaskTemplateTarget].
 *
 * @author Luca Rossetto & Ralph Gasser
 * @version 1.0.0
 */
data class ApiTarget(val type: ApiTargetType, val target: String? = null, val range: ApiTemporalRange? = null)