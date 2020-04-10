package dres.data.model.competition

import dres.data.model.basics.MediaItem
import dres.data.model.basics.TemporalRange
import dres.data.model.competition.interfaces.MediaSegmentTaskDescription
import dres.data.model.competition.interfaces.TaskDescription
import kotlinx.serialization.Serializable

@Serializable
data class KisVisualTaskDescription(override val name: String, override val taskGroup: TaskGroup, override val duration: Long, override val item: MediaItem.VideoItem, override val temporalRange: TemporalRange) : MediaSegmentTaskDescription{
    constructor(name: String, taskGroup: TaskGroup, item: MediaItem.VideoItem, temporalRange: TemporalRange) : this(name, taskGroup, taskGroup.defaultTaskDuration, item, temporalRange)
}

/**
 * Describes a [TaskType.KIS_TEXTUAL] [Task]
 *
 * @param item [MediaItem] the user should be looking for.
 */
@Serializable
data class KisTextualTaskDescription(override val name: String, override val taskGroup: TaskGroup, override val duration: Long, override val item: MediaItem.VideoItem, override val temporalRange: TemporalRange, val descriptions: List<String>, val delay: Int = 30) : MediaSegmentTaskDescription {
    constructor(name: String, taskGroup: TaskGroup, item: MediaItem.VideoItem, temporalRange: TemporalRange, descriptions: List<String>, delay: Int = 30) : this(name, taskGroup, taskGroup.defaultTaskDuration, item, temporalRange, descriptions, delay)
}

/**
 * Describes a [TaskType.AVS] video [Task]
 *
 * @param description Textual task description presented to the user.
 */
@Serializable
data class AvsTaskDescription(override val name: String, override val taskGroup: TaskGroup, override val duration: Long, val description: String) : TaskDescription {
    constructor(name: String, taskGroup: TaskGroup, description: String) : this(name, taskGroup, taskGroup.defaultTaskDuration, description)
}


