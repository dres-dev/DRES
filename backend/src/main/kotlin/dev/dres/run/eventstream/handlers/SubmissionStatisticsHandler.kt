package dev.dres.run.eventstream.handlers

import dev.dres.data.model.submissions.Submission
import dev.dres.data.model.submissions.VerdictStatus
import dev.dres.run.eventstream.*
import org.slf4j.LoggerFactory
import java.io.File
import java.io.PrintWriter

class SubmissionStatisticsHandler : StreamEventHandler {

    private val writer = PrintWriter(File("statistics/submission_statistics_${System.currentTimeMillis()}.csv").also { it.parentFile.mkdirs() })

    private val submissionTaskMap = mutableMapOf<EvaluationId, MutableList<Submission>>()
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

    private fun computeStatistics(submissions: List<Submission>, taskStart: Long, task: String) {

        val submissionsByTeam = submissions.groupBy { it.teamId }

        submissionsByTeam.mapValues { it.value.size }.forEach{
            (teamId, count) -> writer.println("$task,${teamId.string},\"totalSubmissionsPerTeam\",$count")
        }
        submissionsByTeam.mapValues {
            it.value.firstOrNull { s -> s.status == VerdictStatus.CORRECT }?.timestamp?.minus(taskStart) }
                .filter { it.value != null }.forEach{
                    (teamId, time) -> writer.println("$task,${teamId.string},\"timeUntilCorrectSubmission\",$time")
                }
        submissionsByTeam.mapValues {
            it.value.indexOfFirst { s -> s.status == VerdictStatus.CORRECT } }.forEach{
            (teamId, count) -> writer.println("$task,${teamId.string},\"incorrectBeforeCorrectSubmissions\",$count")
        }
        writer.flush()


    }


}
