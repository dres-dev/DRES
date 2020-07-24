package dres.api.cli

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.long
import com.jakewharton.picnic.table

import dres.data.dbo.DAO
import dres.data.model.UID
import dres.data.model.run.CompetitionRun
import dres.data.model.run.SubmissionStatus
import dres.run.RunExecutor
import dres.utilities.extensions.UID
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

class CompetitionRunCommand(internal val runs: DAO<CompetitionRun>) : NoOpCliktCommand(name = "run") {

    init {
        subcommands(OngoingCompetitionRunsCommand(), ListCompetitionRunsCommand(), DeleteRunCommand(), ExportRunCommand(), CompetitionRunsHistoryCommand(), ResetSubmissionStatusCommand())
    }

    /**
     * Helper class that contains all information regarding a [RunManager].
     */
    data class RunSummary(val id: String, val name: String, val description: String?, val task: String?)

    /**
     * Lists all ongoing competitions runs for the current DRES instance.
     */
    inner class OngoingCompetitionRunsCommand : CliktCommand(name = "ongoing", help = "Lists all ongoing competition runs.") {
        val plain by option("-p", "--plain", help = "Plain print. No fancy tables").flag(default = false)
        override fun run() {
            if (RunExecutor.managers().isEmpty()) {
                println("No runs are currently ongoing!")
                return
            }
            if (plain) {
                RunExecutor.managers().forEach {
                    println("${RunSummary(it.id.string, it.name, it.competitionDescription.description, it.currentTask?.name)} (${it.status})")
                }
            } else {
                table {
                    cellStyle {
                        border = true
                        paddingLeft = 1
                        paddingRight = 1
                    }
                    header {
                        row("id", "name", "description", "currentTask", "status")
                    }
                    body {
                        RunExecutor.managers().forEach {
                            row(it.id, it.name, it.competitionDescription.description, it.currentTask?.name
                                    ?: "N/A", it.status)
                        }
                    }
                }
            }
        }
    }

    /**
     * Lists all competition runs (ongoing and past) for the current DRES instance.
     */
    inner class ListCompetitionRunsCommand : CliktCommand(name = "list", help = "Lists all (ongoing and past) competition runs.") {
        val plain by option("-p", "--plain", help = "Plain print. No fancy tables").flag(default = false)
        override fun run() {
            if (plain) {
                this@CompetitionRunCommand.runs.forEach {
                    println("${RunSummary(it.id.string, it.name, it.competitionDescription.description, it.lastTask?.task?.name)}")
                }
            } else {
                table {
                    cellStyle {
                        border = true
                        paddingLeft = 1
                        paddingRight = 1
                    }
                    header {
                        row("id", "name", "description", "lastTask", "status")
                    }
                    body {
                        this@CompetitionRunCommand.runs.forEach {
                            val status = if(it.hasStarted && !it.hasEnded && !it.isRunning){
                                "started"
                            }else if(it.hasStarted && !it.hasEnded && it.isRunning){
                                "running"
                            }else if(it.hasEnded){
                                "ended"
                            }else if(!it.hasStarted){
                                "idle"
                            }else{
                                "unkown"
                            }
                            row(it.id, it.name, it.competitionDescription.description, it.lastTask?.task?.name
                                    ?: "N/A", status)
                        }
                    }
                }
            }
        }
    }

    /**
     * Deletes a selected competition run for the current DRES instance.
     */
    inner class DeleteRunCommand: CliktCommand(name = "delete", help = "Deletes an existing competition run.") {
        private val id: UID by option("-r", "--run").convert { it.UID() }.required()
        override fun run() {
            if (RunExecutor.managers().any { it.id == id }) {
                println("Run with ID $id could not be deleted because it is still running! Terminate it and try again.")
                return
            }

            val deleted = this@CompetitionRunCommand.runs.delete(this.id)
            if (deleted != null) {
                println("Run $deleted deleted successfully!")
            } else {
                println("Run with ID $id could not be deleted because it doesn't exist!")
            }
        }
    }

    /**
     * Exports a specific competition run as JSON.
     */
    inner class ExportRunCommand: CliktCommand(name = "export", help = "Exports the competition run as JSON.") {
        private val id: UID by option("-r", "--run").convert { it.UID() }.required()
        private val path: String by option("-o", "--output").required()
        override fun run() {
            val run = this@CompetitionRunCommand.runs[this.id]
            if (run == null) {
                println("Run does not seem to exist.")
                return
            }

            val path = Paths.get(this.path)
            val mapper = ObjectMapper()
            mapper.registerModule(KotlinModule())
            Files.newBufferedWriter(path, StandardOpenOption.WRITE, StandardOpenOption.CREATE).use {
                mapper.writeValue(it, run)
            }
            println("Successfully wrote run ${run.id} to $path.")
        }
    }

//    /**
//     * Exports a specific competition run as JSON.
//     */
//    inner class ExportLogsCommand: CliktCommand(name = "exportLogs", help = "Exports just the interaction logs of the competition run as JSON.") {
//        private val id: Long by option("-r", "--run").long().required()
//        private val path: String by option("-o", "--output").required()
//        override fun run() {
//            val run = this@CompetitionRunCommand.runs[this.id]
//            if (run == null) {
//                println("Run does not seem to exist.")
//                return
//            }
//
//            val path = Paths.get(this.path)
//            val mapper = ObjectMapper()
//            mapper.registerModule(KotlinModule())
//
//            val resultLogs = run.runs.flatMap { it.data.sessionQueryResultLogs.values }.flatten()
//            val interactionLogs = run.runs.flatMap { it.data.sessionQueryEventLogs.values }.flatten()
//
//            Files.newBufferedWriter(path, StandardOpenOption.WRITE, StandardOpenOption.CREATE).use {
//                it.appendln(mapper.writeValueAsString(resultLogs))
//                it.newLine()
//                it.appendln(mapper.writeValueAsString(interactionLogs))
//            }
//            println("Successfully wrote run ${run.id} to $path.")
//        }
//    }


    inner class CompetitionRunsHistoryCommand : CliktCommand(name = "history", help = "Lists past Competition Runs") {
        // TODO fancification with table

        override fun run() {

            this@CompetitionRunCommand.runs.forEach {

                println(it.name)

                println("Teams:")
                it.competitionDescription.teams.forEach {
                    println(it)
                }

                println()
                println("All Tasks:")
                it.competitionDescription.tasks.forEach {
                    println(it)
                }

                println()
                println("Evaluated Tasks:")
                it.runs.forEach {
                    println(it.task)

                    println("Submissions")
                    it.submissions.forEach { println(it) }
                }
                println()
            }

        }

    }


    inner class ResetSubmissionStatusCommand : NoOpCliktCommand(name = "resetSubmission", help = "Resets Submission Status to INDETERMINATE") {

        init {
            subcommands(ResetSingleSubmissionStatusCommand(), ResetTaskSubmissionStatusCommand(), ResetTaskGroupSubmissionStatusCommand())
        }


        inner class ResetSingleSubmissionStatusCommand : CliktCommand(name = "submission", help = "Resets the status of individual submissions") {

            private val runId: UID by option("-r", "--run", help = "Id of the run").convert { it.UID() }.required()
            private val ids: List<String> by option("-i", "--ids", help = "UIDs of the submissions to reset").multiple()

            override fun run() {

                val run = this@CompetitionRunCommand.runs[this.runId]
                if (run == null) {
                    println("Run does not seem to exist.")
                    return
                }

                val submissions = run.runs.flatMap { it.submissions }.filter { it.uid in ids }
                submissions.forEach { it.status = SubmissionStatus.INDETERMINATE }

                this@CompetitionRunCommand.runs.update(run)

                println("reset ${submissions.size} submissions")

            }
        }

        inner class ResetTaskSubmissionStatusCommand : CliktCommand(name = "task", help = "Resets the status of all submissions of specified tasks") {

            private val runId: UID by option("-r", "--run", help = "Id of the run").convert { it.UID() }.required()
            private val ids: List<String> by option("-i", "--ids", help = "UIDs of the tasks to reset").multiple()

            override fun run() {

                val run = this@CompetitionRunCommand.runs[this.runId]
                if (run == null) {
                    println("Run does not seem to exist.")
                    return
                }

                val submissions = run.runs.filter { it.uid in ids }.flatMap { it.submissions }
                submissions.forEach { it.status = SubmissionStatus.INDETERMINATE }

                this@CompetitionRunCommand.runs.update(run)

                println("reset ${submissions.size} submissions")

            }
        }

        inner class ResetTaskGroupSubmissionStatusCommand : CliktCommand(name = "taskGroup", help = "Resets the status all submissions for tasks within a task group") {

            private val runId: UID by option("-r", "--run", help = "Id of the run").convert { it.UID() }.required()
            private val taskGroup: String by option("-g", "--taskGroup", help = "Name of the Task Group to reset").required()

            override fun run() {

                val run = this@CompetitionRunCommand.runs[this.runId]
                if (run == null) {
                    println("Run does not seem to exist.")
                    return
                }

                val submissions = run.runs.filter { it.task.taskGroup.name == taskGroup }.flatMap { it.submissions }
                submissions.forEach { it.status = SubmissionStatus.INDETERMINATE }

                this@CompetitionRunCommand.runs.update(run)

                println("reset ${submissions.size} submissions")

            }
        }

    }


}