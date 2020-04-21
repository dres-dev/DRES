package dres

import dres.data.dbo.DAO
import dres.data.dbo.DaoIndexer
import dres.data.serializers.MediaItemSegmentSerializer
import dres.data.serializers.MediaItemSerializer
import java.nio.file.Path


object Playground {

    @JvmStatic
    fun main(args: Array<String>) {

//        val taskGroup = TaskGroup("testTasks", TaskType.KIS_VISUAL, 100000)
//        val videoItem = MediaItem.VideoItem(1, "test", "testVideo.mp4", 1, 10_000, 24.0f)
//        val temporalRange = TemporalRange(TemporalPoint(1.0, TemporalUnit.SECONDS), TemporalPoint(6.0, TemporalUnit.SECONDS))
//        val description = TaskDescriptionBase.KisVisualTaskDescription("testTask", taskGroup, 60000, videoItem, temporalRange)
//
//        FFmpegUtil.prepareMediaSegmentTask(description, "v3c1", File("task-cache"))

        val mediaSegments = DAO(Path.of("data/mediaSegments.db"), MediaItemSegmentSerializer)
        val mediaItems = DAO(Path.of("data/mediaItems.db"), MediaItemSerializer)

        val shot = "10"

        var startTime = System.currentTimeMillis()

        val item = mediaItems.find {
            it.name == "01000" && it.collection == 1L
        }!!

        println(System.currentTimeMillis() - startTime)

//        startTime = System.currentTimeMillis()
//
//        val segment = mediaSegments.find { it.mediaItemId == item.id && it.name == shot }!!
//
//        print("finding in dao ")
//        println(System.currentTimeMillis() - startTime)
//
//        println(segment)
//
//
//        startTime = System.currentTimeMillis()
//
//        val allSegments = mediaSegments.map { it }
//
//        print("copy to list ")
//        println(System.currentTimeMillis() - startTime)
//
//        startTime = System.currentTimeMillis()
//
//        val segment2 = allSegments.find { it.mediaItemId == item.id && it.name == shot }!!
//
//        print("finding in list ")
//        println(System.currentTimeMillis() - startTime)
//
//
//        startTime = System.currentTimeMillis()
//        val allSegmentsMap = mediaSegments.map { Pair(it.mediaItemId, it.name) to it.range }.toMap()
//        print("copy to map ")
//        println(System.currentTimeMillis() - startTime)
//
//        startTime = System.currentTimeMillis()
//        val segment3 = allSegmentsMap[Pair(item.id, shot)]
//        print("lookup in map ")
//        println(System.currentTimeMillis() - startTime)

        startTime = System.currentTimeMillis()
        val indexer = DaoIndexer(mediaSegments){Pair(it.mediaItemId, it.name)}
        print("creating indexer ")
        println(System.currentTimeMillis() - startTime)

        startTime = System.currentTimeMillis()
        println(indexer[Pair(item.id, shot)].first())
        print("lookup in indexer ")
        println(System.currentTimeMillis() - startTime)
    }

}