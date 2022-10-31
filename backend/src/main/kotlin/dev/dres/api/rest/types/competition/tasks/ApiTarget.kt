package dev.dres.api.rest.types.competition.tasks

import dev.dres.api.rest.types.collection.time.ApiTemporalRange
import dev.dres.data.model.competition.task.TaskDescriptionTarget

/**
 * The RESTful API equivalent for [TaskDescriptionTarget].
 *
 * @author Luca Rossetto & Ralph Gasser
 * @version 1.0.0
 */
data class ApiTarget(val type: ApiTargetType, val target: String? = null, val range: ApiTemporalRange? = null)