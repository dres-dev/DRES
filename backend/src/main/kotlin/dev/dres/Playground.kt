package dev.dres

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import dev.dres.data.dbo.DAO
import dev.dres.data.model.UID
import dev.dres.data.model.run.interfaces.Competition
import dev.dres.data.model.submissions.SubmissionStatus
import dev.dres.data.model.submissions.aspects.StatusAspect
import dev.dres.data.serializers.CompetitionRunSerializer
import dev.dres.data.serializers.CompetitionSerializer
import dev.dres.data.serializers.MediaItemSerializer
import java.io.File
import java.nio.file.Paths


object Playground {

    data class StatusConrainer(override var status: SubmissionStatus) : StatusAspect

    @JvmStatic
    fun main(args: Array<String>) {
        val competitionId = UID("8c57b76e-416f-44ea-a783-7d0e8becf7a3")

        val mapper = jacksonObjectMapper()

        val mediaItems = DAO(Paths.get("C:/Users/Lucaro/Desktop/vbs21/data/mediaItems.db"), MediaItemSerializer)
        val competitionSerializer = CompetitionSerializer(mediaItems)

        val runs: DAO<Competition> = DAO(Paths.get("C:/Users/Lucaro/Desktop/vbs21/data/runs.db"), CompetitionRunSerializer(competitionSerializer))

        val writer = File("C:/Users/Lucaro/Desktop/vbs21/mediaItems.csv").printWriter()

        mediaItems.sortedBy { it.name }.forEach {
            writer.println("${it.id.string},${it.name}")
        }

        writer.flush()
        writer.close()


//        val writer = File("C:\\Users\\Lucaro\\Desktop\\vbs21\\run.json").printWriter()
//
//        writer.println(mapper.writeValueAsString(runs.filter { it.id  == competitionId }.first()))
//
//        writer.flush()
//        writer.close()


//        val audit = DAO(Paths.get("C:\\Users\\Lucaro\\Desktop\\vbs21\\data\\auditLog.db"), AuditLogEntrySerializer)
//
//
//
//        var flag = false
//
//        val logEntries = audit.sortedBy { it.timestamp }.filter {
//            flag || if (it is CompetitionStartAuditLogEntry && it.competition == competitionId) {
//                flag = true
//                true
//            } else false
//        }
//
//        val writer = File("C:\\Users\\Lucaro\\Desktop\\vbs21\\audits.json").printWriter()
//
//        logEntries.forEach {
//            writer.println(mapper.writeValueAsString(it))
//        }
//
//        writer.flush()
//        writer.close()

//        val elements = listOf(
//            StatusConrainer(SubmissionStatus.CORRECT),
//            StatusConrainer(SubmissionStatus.CORRECT),
//            StatusConrainer(SubmissionStatus.CORRECT),
//            StatusConrainer(SubmissionStatus.CORRECT),
//            StatusConrainer(SubmissionStatus.CORRECT)
//
//        )
//
//        println(InferredAveragePrecisionScorer.infAP(elements))


//
//
//        val audit = DAO(Paths.get("C:\\Users\\Lucaro\\Downloads\\LSC2020\\data_branch/auditLog.db"), AuditLogEntrySerializer)
//
//        val mediaItems = DAO(Paths.get("C:\\Users\\Lucaro\\Downloads\\LSC2020\\data_branch/mediaItems.db"), MediaItemSerializer)
//        val competitionSerializer = CompetitionSerializer(mediaItems)
//
//        val competitions = DAO(Paths.get("C:\\Users\\Lucaro\\Downloads\\LSC2020\\data_branch/competitions.db"), competitionSerializer)
//
//
//        val competition = competitions[UID("db2ef71a-df42-4fbd-a2ea-2e1c8b393edd")]!!
//
//        val targets = competition.tasks.map {
//            it.name to (it.target as TaskDescriptionTarget.MultipleMediaItemTarget).items.map { it.name }
//        }.toMap()
//
//        val teams = competition.teams.map { it.name }
//
////        println(audit.find { it.id.string == "b54fc9dd-5a86-4c92-be7b-7642b35c887b" }!!.timestamp)
////
////        return
//
//        val events = audit.filter { it.timestamp > 1604053744241 }.sortedBy { it.timestamp }
//
//        val taskStartTimes = mutableMapOf<String, Long>()
//
//        val submissionWriter = File("submissions.csv").printWriter()
//        val taskStartWriter = File("taskStart.csv").printWriter()
//
//        submissionWriter.println("timestamp,task,time,team,member,session,item,correct")
//        taskStartWriter.println("timestamp,task")
//
//        for (event in events) {
//
//            when(event) {
//
//                is TaskStartAuditLogEntry -> {
//
//                    taskStartTimes[event.taskName] = event.timestamp
//                    taskStartWriter.println("${event.timestamp},${event.taskName}")
//
//                }
//
//
//                is SubmissionAuditLogEntry -> {
//
//                    val taskName = event.taskName
//
//                    if (!targets.containsKey(taskName)){
//                        println("ignoring $taskName")
//                        continue
//                    }
//
//                    val time = event.submission.timestamp - taskStartTimes[taskName]!!
//                    val item = event.submission.item.name
//                    val correct = (targets[taskName] ?:
//                    error("$taskName not found")).contains(item)
//
//
//
//                    //submissionWriter.println("${event.timestamp},$taskName,$time,${teams[event.submission.teamId]},${event.submission.member.string},${event.user},$item,$correct")
//
//                }
//
//                else -> {
//                    println("ignoring $event")
//                }
//
//            }
//
//        }
//
//        submissionWriter.flush()
//        submissionWriter.close()
//        taskStartWriter.flush()
//        taskStartWriter.close()
//

    }

}