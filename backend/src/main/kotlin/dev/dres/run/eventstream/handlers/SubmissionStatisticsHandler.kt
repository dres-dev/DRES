package dev.dres.run.eventstream.handlers

import dev.dres.data.model.run.EvaluationId
import dev.dres.data.model.submissions.DbSubmission
import dev.dres.data.model.submissions.DbVerdictStatus
import dev.dres.run.eventstream.*
import kotlinx.dnq.query.first
import kotlinx.dnq.query.size
import org.slf4j.LoggerFactory
import java.io.File
import java.io.PrintWriter

class SubmissionStatisticsHandler : StreamEventHandler {

    private val writer = PrintWriter(File("statistics/submission_statistics_${System.currentTimeMillis()}.csv").also { it.parentFile.mkdirs() })

    private val submissionTaskMap = mutableMapOf<EvaluationId, MutableList<DbSubmission>>()
    private val taskStartMap = mutableMapOf<EvaluationId, Long>()
    private val taskNameMap = mutableMapOf<EvaluationId, String>()

    private val logger = LoggerFactory.getLogger(this.javaClass)

    init {
        writer.println("task,team,type,value")
    }

    override fun handle(event: StreamEvent) {
        when (event) {
            is TaskStartEvent -> {
                submissionTaskMap[event.taskId] = mutableListOf()
                taskStartMap[event.taskId] = event.timeStamp
                taskNameMap[event.taskId] = event.taskTemplate.name
            }
            is SubmissionEvent -> if (event.taskId != null && taskStartMap.containsKey(event.taskId)){
                submissionTaskMap[event.taskId]!!.add(event.submission)
            }
            is TaskEndEvent -> {
                val submissions = submissionTaskMap[event.taskId]
                val start = taskStartMap[event.taskId]
                val name = taskNameMap[event.taskId]

                if (submissions == null || start == null || name == null) {
                    logger.info("Task '{}' not found in previously started tasks. Already ended previously?", name)
                    return
                }

                computeStatistics(submissions, start, name)
                submissionTaskMap.remove(event.taskId)
                taskStartMap.remove(event.taskId)
                taskNameMap.remove(event.taskId)
            }
            else -> {/* ignore */
            }
        }
    }

    /**
     * TODO: Check and maybe generalise.
     *
     * I assume here, that there this handler requires a single verdict per submission. Is this a valid assumption?
     */
    private fun computeStatistics(submissions: List<DbSubmission>, taskStart: Long, task: String) {
        val submissionsByTeam = submissions.groupBy { it.team.teamId }
        submissionsByTeam.mapValues { it.value.size }.forEach{
            (teamId, count) -> writer.println("$task,${teamId},\"totalSubmissionsPerTeam\",$count")
        }
        submissionsByTeam.mapValues {
            it.value.firstOrNull { s ->
                require(s.answerSets.size() == 1) { "SubmissionStatisticsHandler can only process single-verdict submissions." }
                s.answerSets.first().status == DbVerdictStatus.CORRECT
            }?.timestamp?.minus(taskStart) }.filter { it.value != null }.forEach{
                (teamId, time) -> writer.println("$task,${teamId},\"timeUntilCorrectSubmission\",$time")
            }
        submissionsByTeam.mapValues {
            it.value.indexOfFirst { s ->
                require(s.answerSets.size() == 1) { "SubmissionStatisticsHandler can only process single-verdict submissions." }
                s.answerSets.first().status == DbVerdictStatus.CORRECT
            }
        }.forEach{
            (teamId, count) -> writer.println("$task,${teamId},\"incorrectBeforeCorrectSubmissions\",$count")
        }
        writer.flush()
    }
}
