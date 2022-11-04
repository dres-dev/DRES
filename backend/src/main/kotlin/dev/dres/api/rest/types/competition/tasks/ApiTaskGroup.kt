package dev.dres.api.rest.types.competition.tasks

import dev.dres.data.model.template.task.TaskGroup

/**
 * The RESTful API equivalent of a [TaskGroup].
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
data class ApiTaskGroup(val name: String, val type: String)