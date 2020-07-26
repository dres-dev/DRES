package dres.api.rest.types.competition.tasks

import dres.data.model.basics.time.TemporalRange

/**
 * The RESTful API representation of a target item.
 *
 * @see RestTaskDescriptionTarget
 * @author Ralph Gasser
 * @version 1.0
 *
 */
data class RestTaskDescriptionTargetItem(val segment: String, val temporalRange: TemporalRange? = null)