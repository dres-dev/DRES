package dev.dres.run.eventstream.handlers

import dev.dres.data.model.run.EvaluationId
import dev.dres.data.model.template.task.TaskTemplate
import dev.dres.data.model.submissions.Submission
import dev.dres.run.eventstream.*
import dev.dres.run.score.interfaces.TeamTaskScorer
import java.io.File
import java.io.PrintWriter

/**
 *
 */
class TeamCombinationScoreHandler : StreamEventHandler {

    private val writer = PrintWriter(File("statistics/combined_team_scores_${System.currentTimeMillis()}.csv").also { it.parentFile.mkdirs() })

    /**
     *
     */
    private val tasks = mutableMapOf<EvaluationId, TaskTemplate>()

    /**
     *
     */

    private val taskStartMap = mutableMapOf<EvaluationId, Long>()

    /**
     *
     */
    private val submissionTaskMap = mutableMapOf<EvaluationId, MutableList<Submission>>()

    init {
        writer.println("task,team1,team2,score")
    }

    override fun handle(event: StreamEvent) {

        when(event){
            is TaskStartEvent -> {
                tasks[event.taskId] = event.taskTemplate
                taskStartMap[event.taskId] = event.timeStamp
                submissionTaskMap[event.taskId] = mutableListOf()
            }
            is SubmissionEvent -> if (event.taskId != null && tasks.containsKey(event.taskId)){
                submissionTaskMap[event.taskId]!!.add(event.submission)
            }
            is TaskEndEvent -> {

                val taskDescription = tasks[event.taskId] ?: return

                val scorer = taskDescription.newScorer()

                if (scorer !is TeamTaskScorer) {
                    return
                }

                val submissions = submissionTaskMap[event.taskId] ?: return

                val teams = submissions.map { it.team.teamId }.toSet().toList().sortedBy { it }

                val combinations = teams.mapIndexed { firstIndex, uidA ->
                    teams.mapIndexed {secondIndex, uidB -> if (firstIndex > secondIndex) (uidA to uidB) else null}
                }.flatten().filterNotNull().map { EvaluationId() to it }.toMap()

                /* TODO: Fix. Not quite sure what is going on here. */
                /*val combinedSubmissions = submissions.flatMap { submission ->
                    combinations.map {
                        if (it.value.first == submission.team.teamId || it.value.second == submission.team.teamId) {
                            when (submission) {
                                is Submission.Item -> submission.copy(teamId = it.key).apply { this.status = submission.status }
                                is Submission.Temporal -> submission.copy(teamId = it.key).apply { this.status = submission.status }
                                is Submission.Text -> submission.copy(teamId = it.key).apply { this.status = submission.status }
                            }
                        } else {
                            null
                        }
                    }.filterNotNull()
                }

                when(scorer) {
                    is RecalculatingSubmissionTaskScorer -> {
                        scorer.computeScores(
                                combinedSubmissions,
                                TaskContext(
                                    combinations.keys,
                                    taskStartMap[event.taskId]!!,
                                    taskDescription.duration,
                                    event.timeStamp
                                )
                        )
                    }
                    is IncrementalSubmissionTaskScorer -> {
                        combinedSubmissions.forEach { scorer.update(it) }
                    }
                    else -> throw IllegalStateException("unsupported scorer type $scorer")
                }

                val scores = scorer.teamScoreMap().mapKeys { combinations[it.key]!! }

                scores.forEach {
                    writer.println("${event.taskId.string},${it.key.first.string},${it.key.second.string},${it.value}")
                }

                writer.flush()

                tasks.remove(event.taskId)
                submissionTaskMap.remove(event.taskId)
                taskStartMap.remove(event.taskId) */

            }
            else -> { /* ignore */ }
        }
    }
}