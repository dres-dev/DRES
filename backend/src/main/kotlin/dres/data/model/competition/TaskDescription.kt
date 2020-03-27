package dres.data.model.competition

import dres.data.model.basics.MediaItem
import dres.data.model.basics.TemporalRange

/**
 * General [TaskDescription]
 *
 * @author Ralph Gasser
 * @version 1.0
 */
sealed class TaskDescription {

    companion object {
        const val KIS_VISUAL_TASK_DESCRIPTION = 0
        const val KIS_TEXTUAL_TASK_DESCRIPTION = 1
        const val AVS_TASK_DESCRIPTION = 2
    }


    /**
     * Describes a  [TaskType.KIS_VISUAL] [Task]
     *
     * @param item [MediaItem] the user should be looking for.
     */
    data class KisVisualTaskDescription(val item: MediaItem.VideoItem, val temporalRange: TemporalRange) : TaskDescription()

    /**
     * Describes a [TaskType.KIS_TEXTUAL] [Task]
     *
     * @param item [MediaItem] the user should be looking for.
     */
    data class KisTextualTaskDescription(val item: MediaItem.VideoItem, val temporalRange: TemporalRange, val descriptions: List<String>, val delay: Int = 30) : TaskDescription()

    /**
     * Describes a [TaskType.AVS] video [Task]
     *
     * @param description Textual task description presented to the user.
     */
    data class AvsTaskDescription(val description: String) : TaskDescription()
}

