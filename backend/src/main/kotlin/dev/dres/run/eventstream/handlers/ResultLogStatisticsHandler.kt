package dev.dres.run.eventstream.handlers

import dev.dres.data.model.media.MediaItem
import dev.dres.data.model.media.time.TemporalRange
import dev.dres.data.model.run.EvaluationId
import dev.dres.data.model.template.task.TaskTemplate
import dev.dres.run.eventstream.StreamEvent
import dev.dres.run.eventstream.StreamEventHandler
import jetbrains.exodus.database.TransientEntityStore
import java.io.File
import java.io.PrintWriter

class ResultLogStatisticsHandler(private val store: TransientEntityStore) : StreamEventHandler {

    private val writer = PrintWriter(File("statistics/result_log_statistics_${System.currentTimeMillis()}.csv").also { it.parentFile.mkdirs() })

    private val lastActiveTask = mutableMapOf<EvaluationId, TaskTemplate>()
    private val lastActiveTargets = mutableMapOf<EvaluationId, List<Pair<MediaItem, TemporalRange?>>>()


    init {
        writer.println("timestamp,task,session,item,segment,frame,reportedRank,listRank,inTime")
    }

    override fun handle(event: StreamEvent) {

        /* TODO: Fix / and maybe design + document in a way such that one understands what is going on... :-)

        when (event) {
            is TaskStartEvent -> {
                lastActiveTask[event.runId] = event.taskTemplate
                lastActiveTargets[event.runId] = when(event.taskTemplate.target) {
                    is TaskDescriptionTarget.JudgementTaskDescriptionTarget, is TaskDescriptionTarget.VoteTaskDescriptionTarget, -> return //no analysis possible
                    is TaskDescriptionTarget.MediaItemTarget -> listOf(event.taskTemplate.target.item to null)
                    is TaskDescriptionTarget.VideoSegmentTarget ->  listOf(event.taskTemplate.target.item to event.taskTemplate.target.temporalRange)
                    is TaskDescriptionTarget.MultipleMediaItemTarget -> event.taskTemplate.target.items.map { it to null }
                    is TaskDescriptionTarget.TextTaskDescriptionTarget -> return //TODO maybe some analysis would be possible, needs restructuring
                }
            }
            is QueryResultLogEvent -> {

                val relevantTask = lastActiveTask[event.runId] ?: return
                val relevantTargets = lastActiveTargets[event.runId] ?: return
                
                val correctItems = event.queryResultLog.results.mapIndexed {
                    index, queryResult ->
                    if ( relevantTargets.any { it.first.name == queryResult.item } )
                        index to queryResult else null }.filterNotNull()

                if (correctItems.isEmpty()) {
                    return
                }

                val temporalTargets = relevantTargets.filter { it.second != null }

                if (temporalTargets.isEmpty()) { //consider only items
                    correctItems.forEach {
                        writer.println("${System.currentTimeMillis()},${relevantTask.name},${event.session},${it.second.item},${it.second.segment},${it.second.frame},${it.second.rank},${it.first},n/a")
                    }
                } else { // consider also temporal range
                    val relevantTemporalTargets = temporalTargets.filter { it.first.name == relevantTask.name }

                    correctItems.forEach {
                        val correctTime = (it.second.segment != null || it.second.frame != null) && relevantTemporalTargets.any { target ->
                            val segments = this.segmentIndex[target.first.id].firstOrNull() ?: return@any false
                            val segment = TemporalRange(if (it.second.segment != null) {
                                TimeUtil.shotToTime(it.second.segment.toString(), segments)
                            } else {
                                TimeUtil.timeToSegment(TemporalPoint.Frame.toMilliseconds(it.second.frame!!, (target.first as MediaItem.VideoItem).fps), segments)
                            } ?: return@any false )

                            segment.overlaps(target.second!!)
                        }
                        writer.println("${System.currentTimeMillis()},${relevantTask.name},${event.session},${it.second.item},${it.second.segment},${it.second.frame},${it.second.rank},${it.first},$correctTime")
                    }
                }

                writer.flush()
                
            }

            else -> { /* ignore */ }

        } */
    }
}