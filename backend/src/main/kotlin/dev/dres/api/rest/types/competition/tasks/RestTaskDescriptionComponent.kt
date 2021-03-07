package dev.dres.api.rest.types.competition.tasks

import dev.dres.data.dbo.DAO
import dev.dres.data.model.basics.media.MediaItem
import dev.dres.data.model.basics.time.TemporalRange
import dev.dres.data.model.competition.*
import dev.dres.data.model.competition.options.QueryComponentOption
import dev.dres.utilities.extensions.UID
import java.nio.file.Paths
import javax.management.Query

/**
 * The RESTful API equivalent for [TaskDescriptionHint].
 *
 * @author Luca Rossetto & Ralph Gasser
 * @version 1.0
 */
data class RestTaskDescriptionComponent(
        /**
         * The type of this component
         */
        val type: QueryComponentOption,

        /**
         * Start time in seconds of when this component is active (Task time)
         * Must be a positive integer, including zero (0) (indicates the component is available from the start)
         */
        val start: Long? = null,

        /**
         * End time in seconds of when this component is not active anymore (Task time)
         * Must be a positive integer, greater than [start]
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
         * This is the path to the data.
         */
        val path: String? = null,

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
                 * Generates a [RestTaskDescriptionComponent] from a [TaskDescriptionHint] and returns it.
                 *
                 * @param hint The [TaskDescriptionHint] to convert.
                 */
                fun fromComponent(hint: TaskDescriptionHint) = when(hint) {
                        is TaskDescriptionHint.TextTaskDescriptionHint -> RestTaskDescriptionComponent(type = QueryComponentOption.TEXT, start = hint.start, end = hint.end, description = hint.text)
                        is TaskDescriptionHint.ImageItemTaskDescriptionHint -> RestTaskDescriptionComponent(type = QueryComponentOption.IMAGE_ITEM, start = hint.start, end = hint.end, mediaItem = hint.item.id.string)
                        is TaskDescriptionHint.VideoItemSegmentTaskDescriptionHint -> RestTaskDescriptionComponent(type = QueryComponentOption.VIDEO_ITEM_SEGMENT, start = hint.start, end = hint.end, mediaItem = hint.item.id.string, range = hint.temporalRange)
                        is TaskDescriptionHint.ExternalImageTaskDescriptionHint -> RestTaskDescriptionComponent(type = QueryComponentOption.EXTERNAL_IMAGE, path = hint.imageLocation.toString(), start = hint.start, end = hint.end)
                        is TaskDescriptionHint.ExternalVideoTaskDescriptionHint -> RestTaskDescriptionComponent(type = QueryComponentOption.EXTERNAL_VIDEO, path = hint.videoLocation.toString(), start = hint.start, end = hint.end)
                }
        }

        /**
         * Converts this [RestTaskDescriptionComponent] to the corresponding [TaskDescriptionHint] and returns it.
         *
         * @param mediaItems [DAO] used to perform media item lookups.
         */
        fun toComponent(mediaItems: DAO<MediaItem>) = when(this.type){
                QueryComponentOption.IMAGE_ITEM -> TaskDescriptionHint.ImageItemTaskDescriptionHint(mediaItems[this.mediaItem!!.UID()] as MediaItem.ImageItem, this.start, this.end)
                QueryComponentOption.VIDEO_ITEM_SEGMENT -> TaskDescriptionHint.VideoItemSegmentTaskDescriptionHint(mediaItems[this.mediaItem!!.UID()] as MediaItem.VideoItem, this.range!!, this.start, this.end)
                QueryComponentOption.TEXT -> TaskDescriptionHint.TextTaskDescriptionHint(this.description ?: "", this.start, this.end)
                QueryComponentOption.EXTERNAL_IMAGE -> TaskDescriptionHint.ExternalImageTaskDescriptionHint(Paths.get(this.path ?: throw IllegalArgumentException("Field 'payload' is not specified but required for external image item.")), this.start, this.end)
                QueryComponentOption.EXTERNAL_VIDEO -> TaskDescriptionHint.ExternalVideoTaskDescriptionHint(Paths.get(this.path ?: throw IllegalArgumentException("Field 'payload' is not specified but required for external video item.")), this.start, this.end)
        }
}