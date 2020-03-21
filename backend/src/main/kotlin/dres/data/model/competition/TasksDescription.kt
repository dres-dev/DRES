package dres.data.model.competition

import dres.data.model.basics.MediaItem
import dres.data.model.basics.VideoItem

/**
 * General [TasksDescription]
 *
 * @author Ralph Gasser
 * @version 1.0
 */
sealed class TasksDescription

/**
 * Describes a  [TaskType.KIS_VISUAL] [Task]
 *
 * @param item [MediaItem] the user should be looking for.
 */
data class KisVisualTaskDescription(val item: VideoItem) : TasksDescription()

/**
 * Describes a [TaskType.KIS_TEXTUAL] [Task]
 *
 * @param item [MediaItem] the user should be looking for.
 */
data class KisTextualTaskDescription(val item: VideoItem, val descriptions: List<String>, val delay: Number = 30) : TasksDescription()

/**
 * Describes a [TaskType.AVS] video [Task]
 *
 * @param description Textual task description presented to the user.
 */
data class AvsTaskDescription(val description: String) : TasksDescription()