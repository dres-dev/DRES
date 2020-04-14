package dres

import dres.data.model.basics.MediaItem
import dres.data.model.basics.TemporalPoint
import dres.data.model.basics.TemporalRange
import dres.data.model.basics.TemporalUnit
import dres.data.model.competition.TaskDescriptionBase
import dres.data.model.competition.TaskGroup
import dres.data.model.competition.TaskType
import dres.utilities.FFmpegUtil
import java.io.File


object Playground {

    @JvmStatic
    fun main(args: Array<String>) {

        val taskGroup = TaskGroup("testTasks", TaskType.KIS_VISUAL, 100000)
        val videoItem = MediaItem.VideoItem(1, "test", "testVideo.mp4", 1, 10_000, 24.0f)
        val temporalRange = TemporalRange(TemporalPoint(1.0, TemporalUnit.SECONDS), TemporalPoint(6.0, TemporalUnit.SECONDS))
        val description = TaskDescriptionBase.KisVisualTaskDescription("testTask", taskGroup, 60000, videoItem, temporalRange)

        FFmpegUtil.prepareMediaSegmentTask(description, "v3c1", File("task-cache"))


    }

}