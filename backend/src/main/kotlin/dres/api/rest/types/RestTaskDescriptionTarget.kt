package dres.api.rest.types

import dres.data.model.basics.time.TemporalRange
import dres.data.model.competition.TaskDescriptionTarget
import dres.data.model.competition.TaskType

data class RestTaskDescriptionTarget(
        val type: TaskType.TargetType,
        val mediaItems: List<String> = emptyList(),
        val range: TemporalRange? = null
) {

}

fun RestTaskDescriptionTarget(target: TaskDescriptionTarget) : RestTaskDescriptionTarget {

    TODO()

}