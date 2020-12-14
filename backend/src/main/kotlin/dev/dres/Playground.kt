package dev.dres

import dev.dres.data.dbo.DAO
import dev.dres.data.model.UID
import dev.dres.data.model.competition.TaskDescriptionTarget
import dev.dres.data.serializers.AuditLogEntrySerializer
import dev.dres.data.serializers.CompetitionSerializer
import dev.dres.data.serializers.MediaItemSerializer
import dev.dres.run.audit.SubmissionAuditLogEntry
import dev.dres.run.audit.TaskStartAuditLogEntry
import java.io.File
import java.nio.file.Paths


object Playground {

    @JvmStatic
    fun main(args: Array<String>) {


        val audit = DAO(Paths.get("C:\\Users\\Lucaro\\Downloads\\LSC2020\\data_branch/auditLog.db"), AuditLogEntrySerializer)

        val mediaItems = DAO(Paths.get("C:\\Users\\Lucaro\\Downloads\\LSC2020\\data_branch/mediaItems.db"), MediaItemSerializer)
        val competitionSerializer = CompetitionSerializer(mediaItems)

        val competitions = DAO(Paths.get("C:\\Users\\Lucaro\\Downloads\\LSC2020\\data_branch/competitions.db"), competitionSerializer)


        val competition = competitions[UID("db2ef71a-df42-4fbd-a2ea-2e1c8b393edd")]!!

        val targets = competition.tasks.map {
            it.name to (it.target as TaskDescriptionTarget.MultipleMediaItemTarget).items.map { it.name }
        }.toMap()

        val teams = competition.teams.map { it.name }

//        println(audit.find { it.id.string == "b54fc9dd-5a86-4c92-be7b-7642b35c887b" }!!.timestamp)
//
//        return

        val events = audit.filter { it.timestamp > 1604053744241 }.sortedBy { it.timestamp }

        val taskStartTimes = mutableMapOf<String, Long>()

        val submissionWriter = File("submissions.csv").printWriter()
        val taskStartWriter = File("taskStart.csv").printWriter()

        submissionWriter.println("timestamp,task,time,team,member,session,item,correct")
        taskStartWriter.println("timestamp,task")

        for (event in events) {

            when(event) {

                is TaskStartAuditLogEntry -> {

                    taskStartTimes[event.taskName] = event.timestamp
                    taskStartWriter.println("${event.timestamp},${event.taskName}")

                }


                is SubmissionAuditLogEntry -> {

                    val taskName = event.taskName

                    if (!targets.containsKey(taskName)){
                        println("ignoring $taskName")
                        continue
                    }

                    val time = event.submission.timestamp - taskStartTimes[taskName]!!
                    val item = event.submission.item.name
                    val correct = (targets[taskName] ?:
                    error("$taskName not found")).contains(item)



                    //submissionWriter.println("${event.timestamp},$taskName,$time,${teams[event.submission.teamId]},${event.submission.member.string},${event.user},$item,$correct")

                }

                else -> {
                    println("ignoring $event")
                }

            }

        }

        submissionWriter.flush()
        submissionWriter.close()
        taskStartWriter.flush()
        taskStartWriter.close()


    }

}