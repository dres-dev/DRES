package dev.dres.api.rest.types.template.tasks

import dev.dres.data.model.template.task.DbTaskGroup

/**
 * The RESTful API equivalent of a [DbTaskGroup].
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
data class ApiTaskGroup(val id: String?,val name: String, val type: String)
