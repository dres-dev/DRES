package dev.dres.api.rest.types.competition.tasks

import dev.dres.data.model.basics.time.TemporalRange

/**
 * The RESTful API representation of a target item.
 *
 * @see RestTaskDescriptionTarget
 * @author Ralph Gasser
 * @version 1.0
 *
 */
data class RestTaskDescriptionTargetItem(val mediaItem: String, val temporalRange: TemporalRange? = null)