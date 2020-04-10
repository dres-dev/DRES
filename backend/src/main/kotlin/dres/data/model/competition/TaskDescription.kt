package dres.data.model.competition

import dres.data.model.basics.MediaItem
import dres.data.model.basics.TemporalRange
import dres.data.model.competition.interfaces.TaskDescription
import dres.data.model.competition.interfaces.MediaSegmentTaskDescription
import kotlinx.serialization.Serializable

@Serializable
data class KisVisualTaskDescription(override val taskGroup: TaskGroup, override val item: MediaItem.VideoItem, override val temporalRange: TemporalRange) : MediaSegmentTaskDescription

/**
 * Describes a [TaskType.KIS_TEXTUAL] [Task]
 *
 * @param item [MediaItem] the user should be looking for.
 */
@Serializable
data class KisTextualTaskDescription(override val taskGroup: TaskGroup, override val item: MediaItem.VideoItem, override val temporalRange: TemporalRange, val descriptions: List<String>, val delay: Int = 30) : MediaSegmentTaskDescription

/**
 * Describes a [TaskType.AVS] video [Task]
 *
 * @param description Textual task description presented to the user.
 */
@Serializable
data class AvsTaskDescription(override val taskGroup: TaskGroup, val description: String) : TaskDescription


