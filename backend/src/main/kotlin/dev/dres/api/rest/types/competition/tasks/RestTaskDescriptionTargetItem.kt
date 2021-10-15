package dev.dres.api.rest.types.competition.tasks

/**
 * The RESTful API representation of a target item.
 *
 * @see RestTaskDescriptionTarget
 * @author Ralph Gasser
 * @version 1.0
 *
 */
data class RestTaskDescriptionTargetItem(val mediaItem: String, val temporalRange: RestTemporalRange? = null)