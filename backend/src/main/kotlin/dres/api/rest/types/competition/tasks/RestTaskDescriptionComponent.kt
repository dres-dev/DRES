package dres.api.rest.types.competition.tasks

import dres.data.dbo.DAO
import dres.data.model.basics.media.MediaItem
import dres.data.model.basics.time.TemporalRange
import dres.data.model.competition.*
import dres.utilities.extensions.UID

/**
 * The RESTful API equivalent for [TaskDescriptionComponent].
 *
 * @author Luca Rossetto & Ralph Gasser
 * @version 1.0
 */
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

        companion object {
                /**
                 * Generates a [RestTaskDescriptionComponent] from a [TaskDescriptionComponent] and returns it.
                 *
                 * @param component The [TaskDescriptionComponent] to convert.
                 */
                fun fromComponent(component: TaskDescriptionComponent) = when(component) {
                        is TaskDescriptionComponent.TextTaskDescriptionComponent -> RestTaskDescriptionComponent(type = TaskType.QueryComponentType.TEXT, start = component.start, end = component.end, description = component.text)
                        is TaskDescriptionComponent.ImageItemTaskDescriptionComponent -> RestTaskDescriptionComponent(type = TaskType.QueryComponentType.IMAGE_ITEM, start = component.start, end = component.end, mediaItem = component.item.id.string)
                        is TaskDescriptionComponent.VideoItemSegmentTaskDescriptionComponent -> RestTaskDescriptionComponent(type = TaskType.QueryComponentType.VIDEO_ITEM_SEGMENT, start = component.start, end = component.end, mediaItem = component.item.id.string, range = component.temporalRange)
                is TaskDescriptionComponent.ExternalImageTaskDescriptionComponent -> TODO()
                        is TaskDescriptionComponent.ExternalVideoTaskDescriptionComponent -> TODO()
                }
        }

        /**
         * Converts this [RestTaskDescriptionComponent] to the corresponding [TaskDescriptionComponent] and returns it.
         *
         * @param mediaItems [DAO] used to perform media item lookups.
         */
        fun toComponent(mediaItems: DAO<MediaItem>) = when(this.type){
                TaskType.QueryComponentType.IMAGE_ITEM -> TaskDescriptionComponent.ImageItemTaskDescriptionComponent(mediaItems[this.mediaItem!!.UID()] as MediaItem.ImageItem, this.start, this.end)
                TaskType.QueryComponentType.VIDEO_ITEM_SEGMENT -> TaskDescriptionComponent.VideoItemSegmentTaskDescriptionComponent(mediaItems[this.mediaItem!!.UID()] as MediaItem.VideoItem, this.range!!, this.start, this.end)
                TaskType.QueryComponentType.TEXT -> TaskDescriptionComponent.TextTaskDescriptionComponent(this.description ?: "", this.start, this.end)
                TaskType.QueryComponentType.EXTERNAL_IMAGE -> TODO()
                TaskType.QueryComponentType.EXTERNAL_VIDEO -> TODO()
        }
}