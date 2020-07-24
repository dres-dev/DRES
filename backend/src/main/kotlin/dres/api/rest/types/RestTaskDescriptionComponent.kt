package dres.api.rest.types

import dres.data.model.basics.time.TemporalRange
import dres.data.model.competition.TaskDescriptionComponent
import dres.data.model.competition.TaskType

data class RestTaskDescriptionComponent(
        /**
         * The type of this component
         */
        val type: TaskType.QueryComponentType,
        /**
         * Start time in seconds of when this component is active (Task time)
         */
        val start: Long? = null,
        /**
         * End time in seconds of when this component is not active anymore (Task time)
         */
        val end: Long? = null,
        /**
         * In case [type] is [TaskType.QueryComponentType.TEXT]
         *
         * This is the actual description
         */
        val description: String? = null,
        /**
         * In case [type] is
         *
         * - [TaskType.QueryComponentType.EXTERNAL_IMAGE]
         * - [TaskType.QueryComponentType.EXTERNAL_VIDEO]
         *
         * This is the data. Interpretation depends on [dataType].
         */
        val payload: String? = null,
        /**
         * In case [type] is
         *
         * - [TaskType.QueryComponentType.EXTERNAL_IMAGE]
         * - [TaskType.QueryComponentType.EXTERNAL_VIDEO]
         *
         * This is the data type of the payload.
         * TODO: Is there a standard to use here? Could be URI or base64 actual data...
         */
        val dataType: String? = null,
        /**
         * In case [type] is
         *
         * - [TaskType.QueryComponentType.IMAGE_ITEM]
         * - [TaskType.QueryComponentType.VIDEO_ITEM_SEGMENT]
         *
         * This is the reference to the media item
         */
        val mediaItem: String? = null,
        /**
         * In case [type] is
         *
         * - [TaskType.QueryComponentType.VIDEO_ITEM_SEGMENT]
         * - [TaskType.QueryComponentType.EXTERNAL_VIDEO] TBD In case of blob this wouldn't be necessary
         *
         * This is the actual temporal range in video time
         */
        val range: TemporalRange? = null
) {

}

fun RestTaskDescriptionComponent(component: TaskDescriptionComponent): RestTaskDescriptionComponent {

    TODO()
}