package dres.api.rest.types.competition.tasks

import dres.data.model.basics.time.TemporalRange
import dres.data.model.competition.*

data class RestTaskDescriptionComponent(
        /**
         * The type of this component
         */
        val type: TaskType.QueryComponentType,
        /**
         * Start time in seconds of when this component is active (Task time)
         * Must be a positive integer, including zero (0) (indicates the component is available from the start)
         */
        val start: Long? = null,
        /**
         * End time in seconds of when this component is not active anymore (Task time)
         * Must be a postive integer, greater than [start]
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

        return when(component) {
                is ImageItemTaskDescriptionComponent -> RestTaskDescriptionComponent(TaskType.QueryComponentType.IMAGE_ITEM, component.start, component.end, mediaItem = component.item.id.string)
                is VideoItemSegmentTaskDescriptionComponent -> RestTaskDescriptionComponent(TaskType.QueryComponentType.VIDEO_ITEM_SEGMENT, component.start, component.end, mediaItem = component.item.id.string)
                is TextTaskDescriptionComponent -> RestTaskDescriptionComponent(TaskType.QueryComponentType.TEXT, component.start, component.end, description = component.text)
                else -> throw IllegalArgumentException("translation for ${component.javaClass.simpleName} not supported")
        }

}