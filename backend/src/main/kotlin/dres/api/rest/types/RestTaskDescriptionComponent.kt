package dres.api.rest.types

import dres.data.model.basics.time.TemporalRange
import dres.data.model.competition.TaskDescriptionComponent
import dres.data.model.competition.TaskType

data class RestTaskDescriptionComponent(
        val type: TaskType.QueryComponentType,
        val start: Long? = null,
        val end: Long? = null,
        val description: String? = null,
        val payload: String? = null,
        val dataType: String? = null,
        val mediaItem: String? = null,
        val range: TemporalRange? = null
) {

}

fun RestTaskDescriptionComponent(component: TaskDescriptionComponent): RestTaskDescriptionComponent {
    TODO()
}