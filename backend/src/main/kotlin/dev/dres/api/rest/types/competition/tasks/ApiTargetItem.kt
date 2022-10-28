package dev.dres.api.rest.types.competition.tasks

/**
 * The RESTful API representation of a target item.
 *
 * @see ApiTarget
 * @author Ralph Gasser
 * @version 1.0
 *
 */
data class ApiTargetItem(val target: String, val temporalRange: RestTemporalRange? = null)