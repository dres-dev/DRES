package dev.dres.run.eventstream.handlers

import dev.dres.data.model.UID
import dev.dres.data.model.competition.TaskDescription
import dev.dres.data.model.run.ItemSubmission
import dev.dres.data.model.run.Submission
import dev.dres.data.model.run.TemporalSubmission
import dev.dres.run.eventstream.*
import dev.dres.run.score.interfaces.IncrementalTaskRunScorer
import dev.dres.run.score.interfaces.RecalculatingTaskRunScorer
import java.io.File
import java.io.PrintWriter

class TeamCombinationScoreHandler : StreamEventHandler {

    private val writer = PrintWriter(File("statistics/combined_team_scores_${System.currentTimeMillis()}.csv").also { it.parentFile.mkdirs() })

    private val tasks = mutableMapOf<UID, TaskDescription>()
    private val taskStartMap = mutableMapOf<UID, Long>()
    private val submissionTaskMap = mutableMapOf<UID, MutableList<Submission>>()

    init {
        writer.println("task,team1,team2,score")
    }

    override fun handle(event: StreamEvent) {

        when(event){
            is TaskStartEvent -> {
                tasks[event.taskId] = event.taskDescription
                taskStartMap[event.taskId] = event.timeStamp
                submissionTaskMap[event.taskId] = mutableListOf()
            }
            is SubmissionEvent -> if (event.taskId != null && tasks.containsKey(event.taskId)){
                submissionTaskMap[event.taskId]!!.add(event.submission)
            }
            is TaskEndEvent -> {

                val taskDescription = tasks[event.taskId] ?: return

                val scorer = taskDescription.newScorer()

                val submissions = submissionTaskMap[event.taskId] ?: return

                val teams = submissions.map { it.teamId }.toSet().toList().sortedBy { it.string }

                val combinations = teams.mapIndexed { firstIndex, uidA ->
                    teams.mapIndexed {secondIndex, uidB -> if (firstIndex > secondIndex) (uidA to uidB) else null}
                }.flatten().filterNotNull().map { UID() to it }.toMap()

                val combinedSubmissions = submissions.flatMap { submission ->
                    combinations.map {
                        if (it.value.first == submission.teamId || it.value.second == submission.teamId) {
                            when (submission) {
                                is ItemSubmission -> submission.copy(teamId = it.key).apply { this.status = submission.status }
                                is TemporalSubmission -> submission.copy(teamId = it.key).apply { this.status = submission.status }
                            }
                        } else {
                            null
                        }
                    }.filterNotNull()
                }

                when(scorer) {
                    is RecalculatingTaskRunScorer -> {
                        scorer.computeScores(
                                combinedSubmissions,
                                combinations.keys,
                                taskStartMap[event.taskId]!!,
                                taskDescription.duration,
                                event.timeStamp
                        )
                    }
                    is IncrementalTaskRunScorer -> {
                        combinedSubmissions.forEach { scorer.update(it) }
                    }
                    else -> throw IllegalStateException("unsupported scorer type $scorer")
                }

                val scores = scorer.scores().mapKeys { combinations[it.key]!! }

                scores.forEach {
                    writer.println("${event.taskId.string},${it.key.first.string},${it.key.second.string},${it.value}")
                }

                writer.flush()

                tasks.remove(event.taskId)
                submissionTaskMap.remove(event.taskId)
                taskStartMap.remove(event.taskId)

            }
            else -> { /* ignore */ }

        }

    }

}