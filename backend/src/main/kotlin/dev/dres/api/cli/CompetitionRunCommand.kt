package dev.dres.api.cli

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.path
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import com.jakewharton.picnic.table
import dev.dres.data.dbo.DAO
import dev.dres.data.model.UID
import dev.dres.data.model.competition.TaskDescriptionTarget
import dev.dres.data.model.run.AbstractInteractiveTask
import dev.dres.data.model.run.InteractiveSynchronousCompetition
import dev.dres.data.model.run.RunActionContext
import dev.dres.data.model.run.interfaces.Competition
import dev.dres.data.model.submissions.SubmissionStatus
import dev.dres.data.model.submissions.aspects.ItemAspect
import dev.dres.data.model.submissions.aspects.TemporalSubmissionAspect
import dev.dres.data.model.submissions.aspects.TextAspect
import dev.dres.run.*
import dev.dres.utilities.extensions.UID
import dev.dres.utilities.extensions.toDateString
import java.io.FileOutputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

class CompetitionRunCommand(internal val runs: DAO<Competition>) : NoOpCliktCommand(name = "run") {

    init {
        subcommands(
            OngoingCompetitionRunsCommand(),
            ListCompetitionRunsCommand(),
            DeleteRunCommand(),
            ExportRunCommand(),
            ReactivateRunCommand(),
            CompetitionRunsHistoryCommand(),
            ResetSubmissionStatusCommand(),
            ExportRunJudgementsCommand()
        )
    }

    override fun aliases(): Map<String, List<String>> {
        return mapOf(
            "ls" to listOf("ongoing"),
            "la" to listOf("list"),
            "remove" to listOf("delete"),
            "drop" to listOf("delete")
        )
    }

    /**
     * Helper class that contains all information regarding a [RunManager].
     */
    data class RunSummary(val id: String, val name: String, val description: String?, val task: String?)

    /**
     * Lists all ongoing competitions runs for the current DRES instance.
     */
    inner class OngoingCompetitionRunsCommand :
        CliktCommand(name = "ongoing", help = "Lists all ongoing competition runs.") {
        private val plain by option("-p", "--plain", help = "Plain print. No fancy tables").flag(default = false)
        override fun run() {
            if (RunExecutor.managers().isEmpty()) {
                println("No runs are currently ongoing!")
                return
            }
            if (plain) {
                RunExecutor.managers().filterIsInstance(InteractiveRunManager::class.java).forEach {
                    println(
                        "${
                            RunSummary(
                                it.id.string,
                                it.name,
                                it.description.description,
                                it.currentTaskDescription(RunActionContext.INTERNAL).name
                            )
                        } (${it.status})"
                    )
                }
            } else {
                println(
                    table {
                        cellStyle {
                            border = true
                            paddingLeft = 1
                            paddingRight = 1
                        }
                        header {
                            row("id", "type", "name", "description", "currentTask", "status")
                        }
                        body {
                            RunExecutor.managers().filterIsInstance(InteractiveRunManager::class.java).forEach {
                                when(it) {
                                    is InteractiveSynchronousRunManager -> row(
                                        it.id.string,
                                        "Synchronous",
                                        it.name,
                                        it.description.description,
                                        it.currentTaskDescription(RunActionContext.INTERNAL).name,
                                        it.status
                                    )
                                    is InteractiveAsynchronousRunManager -> row(
                                        it.id.string,
                                        "Asynchronous",
                                        it.name,
                                        it.description.description,
                                        "N/A",
                                        it.status
                                    )
                                    else -> row("??", "??", "??", "??", "??", "??")
                                }

                            }
                        }
                    }
                )
            }
        }
    }

    /**
     * Lists all competition runs (ongoing and past) for the current DRES instance.
     */
    inner class ListCompetitionRunsCommand :
        CliktCommand(name = "list", help = "Lists all (ongoing and past) competition runs.") {
        private val plain by option("-p", "--plain", help = "Plain print. No fancy tables").flag(default = false)
        override fun run() {
            if (plain) {
                this@CompetitionRunCommand.runs.sortedByDescending { it.started }.forEach {
                    println(
                        "${
                            RunSummary(
                                it.id.string,
                                it.name,
                                it.description.description,
                                if (it is InteractiveSynchronousCompetition) it.currentTask?.description?.name
                                    ?: "N/A" else "N/A"
                            )
                        }"
                    )
                }
            } else {
                println(
                    table {
                        cellStyle {
                            border = true
                            paddingLeft = 1
                            paddingRight = 1
                        }
                        header {
                            row("id", "name", "description", "lastTask", "status", "start", "end")
                        }
                        body {
                            this@CompetitionRunCommand.runs.sortedByDescending { it.started }.forEach {
                                val status = if (it.hasStarted && !it.hasEnded && !it.isRunning) {
                                    "started"
                                } else if (it.hasStarted && !it.hasEnded && it.isRunning) {
                                    "running"
                                } else if (it.hasEnded) {
                                    "ended"
                                } else if (!it.hasStarted) {
                                    "idle"
                                } else {
                                    "unknown"
                                }
                                row(
                                    it.id.string,
                                    it.name,
                                    it.description.description,
                                    if (it is InteractiveSynchronousCompetition) it.currentTask?.description?.name
                                        ?: "N/A" else "N/A",
                                    status,
                                    it.started?.toDateString() ?: "-",
                                    it.ended?.toDateString() ?: "-",
                                )
                            }
                        }
                    }
                )
            }
        }
    }

    /**
     * Deletes a selected competition run for the current DRES instance.
     */
    inner class DeleteRunCommand :
        CliktCommand(name = "delete", help = "Deletes an existing competition run.", printHelpOnEmptyArgs = true) {
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
                println("Run with ID ${id.string} could not be deleted because it doesn't exist!")
            }
        }
    }

    /**
     * Exports a specific competition run as JSON.
     */
    inner class ExportRunCommand : CliktCommand(name = "export", help = "Exports the selected competition run to a JSON file.", printHelpOnEmptyArgs = true) {

        /** [UID] of the [Competition] that should be exported. .*/
        private val id: UID by option("-i", "--id").convert { it.UID() }.required()

        /** Path to the file that should be created .*/
        private val path: Path by option("-o", "--output").path().required()

        /** Flag indicating whether export should be pretty printed.*/
        private val pretty: Boolean by option("-p", "--pretty", help = "Flag indicating whether exported JSON should be pretty printed.").flag("-u", "--ugly", default = true)

        override fun run() {
            val run = this@CompetitionRunCommand.runs[this.id]
            if (run == null) {
                println("Run does not seem to exist.")
                return
            }

            val mapper = jacksonObjectMapper()
            Files.newBufferedWriter(path, StandardOpenOption.WRITE, StandardOpenOption.CREATE).use {
                val writer = if (this.pretty) {
                    mapper.writerWithDefaultPrettyPrinter()
                } else {
                    mapper.writer()
                }
                writer.writeValue(it, run)
            }
            println("Successfully wrote run ${run.id} to $path.")
        }
    }

    inner class ReactivateRunCommand : CliktCommand(name = "reactivate", help = "Reactivates a previously ended competition run", printHelpOnEmptyArgs = true) {

        private val id: UID by option("-r", "--run").convert { it.UID() }.required()

        override fun run() {
            val run = this@CompetitionRunCommand.runs[this.id]
            if (run == null) {
                println("Run does not seem to exist.")
                return
            }

            if(!run.hasEnded || run.isRunning) {
                println("Run has not ended.")
                return
            }

            if (RunExecutor.managers().any { it.id == run.id }) {
                println("Run already active.")
                return
            }

            run.reactivate()
            RunExecutor.schedule(run)

            println("Run reactivated")


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
                it.description.teams.forEach { team ->
                    println(team)
                }

                println()
                println("All Tasks:")
                it.description.tasks.forEach { task ->
                    println(task)
                }

                println()
                println("Evaluated Tasks:")
                it.tasks.forEach { t ->
                    println(t.description)

                    if (t is InteractiveSynchronousCompetition.Task) {
                        println("Submissions")
                        t.submissions.forEach { s -> println(s) }
                    }


                }
                println()
            }

        }

    }


    inner class ResetSubmissionStatusCommand :
        NoOpCliktCommand(
            name = "resetSubmission",
            help = "Resets Submission Status to INDETERMINATE",
            printHelpOnEmptyArgs = true
        ) {

        init {
            subcommands(
                ResetSingleSubmissionStatusCommand(),
                ResetTaskSubmissionStatusCommand(),
                ResetTaskGroupSubmissionStatusCommand()
            )
        }


        inner class ResetSingleSubmissionStatusCommand :
            CliktCommand(
                name = "submission",
                help = "Resets the status of individual submissions",
                printHelpOnEmptyArgs = true
            ) {

            private val runId: UID by option("-r", "--run", help = "Id of the run").convert { it.UID() }.required()
            private val ids: List<String> by option("-i", "--ids", help = "UIDs of the submissions to reset").multiple()

            override fun run() {
                /* Fetch competition run. */
                val run = this@CompetitionRunCommand.runs[this.runId]
                if (run == null) {
                    println("Run does not seem to exist.")
                    return
                }

                if (run is InteractiveSynchronousCompetition) {

                    /* Fetch submissions and reset them. */
                    val submissions = run.tasks.flatMap {
                        it.submissions
                    }.filter {
                        it.uid.string in ids
                    }
                    submissions.forEach { it.status = SubmissionStatus.INDETERMINATE }

                    /* Update competition run through dao. */
                    this@CompetitionRunCommand.runs.update(run)
                    println("Successfully reset ${submissions.size} submissions.")
                } else {
                    println("Operation not supported for run type")
                }
            }
        }

        inner class ResetTaskSubmissionStatusCommand :
            CliktCommand(
                name = "task",
                help = "Resets the status of all submissions of specified tasks.",
                printHelpOnEmptyArgs = true
            ) {

            private val runId: UID by option("-r", "--run", help = "UID of the runs").convert { it.UID() }.required()
            private val ids: List<String> by option("-i", "--ids", help = "UIDs of the task runs to resets").multiple()

            override fun run() {

                /* Fetch competition run. */
                val run = this@CompetitionRunCommand.runs[this.runId]
                if (run == null) {
                    println("Run does not seem to exist.")
                    return
                }

                if (run is InteractiveSynchronousCompetition) {
                    /* Fetch submissions and reset them. */
                    val submissions = run.tasks.filter {
                        it.uid.string in ids
                    }.flatMap {
                        it.submissions
                    }
                    submissions.forEach { it.status = SubmissionStatus.INDETERMINATE }

                    this@CompetitionRunCommand.runs.update(run)
                    println("Successfully reset ${submissions.size} submissions.")
                } else {
                    println("Operation not supported for run type")
                }
            }
        }

        inner class ResetTaskGroupSubmissionStatusCommand :
            CliktCommand(
                name = "taskGroup",
                help = "Resets the status all submissions for tasks within a task group",
                printHelpOnEmptyArgs = true
            ) {

            private val runId: UID by option("-r", "--run", help = "Id of the run").convert { it.UID() }.required()
            private val taskGroup: String by option(
                "-g",
                "--taskGroup",
                help = "Name of the Task Group to reset"
            ).required()

            override fun run() {

                val run = this@CompetitionRunCommand.runs[this.runId]
                if (run == null) {
                    println("Run does not seem to exist.")
                    return
                }

                if (run is InteractiveSynchronousCompetition) {

                    val submissions =
                        run.tasks.filter { it.description.taskGroup.name == taskGroup }.flatMap { it.submissions }
                    submissions.forEach { it.status = SubmissionStatus.INDETERMINATE }

                    this@CompetitionRunCommand.runs.update(run)

                    println("reset ${submissions.size} submissions")
                } else {
                    println("Operation not supported for run type")
                }

            }
        }

    }

    inner class ExportRunJudgementsCommand : CliktCommand(
        name = "exportJudgements",
        help = "Exports all judgements made for all relevant tasks of a run as CSV",
        printHelpOnEmptyArgs = true
    ) {

        private val runId: UID by option("-r", "--run", help = "Id of the run").convert { it.UID() }.required()

        private fun fileOutputStream(file: String): OutputStream = FileOutputStream(file)

        private val outputStream: OutputStream by option(
            "-f",
            "--file",
            help = "Path of the file the judgements are to be exported to"
        )
            .convert { fileOutputStream(it) }
            .default(System.out)


        private val header = listOf("TaskId", "TaskName", "ItemId", "StartTime", "EndTime", "Status")

        override fun run() {

            val run = this@CompetitionRunCommand.runs[this.runId]
            if (run == null) {
                println("Run does not seem to exist.")
                return
            }


            val relevantTasks = run.tasks
                .filter { it.description.target is TaskDescriptionTarget.JudgementTaskDescriptionTarget || it.description.target is TaskDescriptionTarget.VoteTaskDescriptionTarget }
                .filterIsInstance(AbstractInteractiveTask::class.java)

            if (relevantTasks.isEmpty()) {
                println("No judged tasks in run.")
                return
            }

            csvWriter().open(outputStream) {
                writeRow(header)

                relevantTasks.forEach { task ->

                    val submittedItems = task.submissions.groupBy {
                        when {
                            it is ItemAspect && it is TemporalSubmissionAspect -> Triple(it.item.name, it.start, it.end)
                            it is ItemAspect -> Triple(it.item.name, 0L, 0L)
                            it is TextAspect -> Triple(it.text, 0L, 0L)
                            else -> Triple("unknown", 0L, 0L)
                        }
                    }

                    submittedItems.entries.forEach {
                        val status = it.value.map { s -> s.status }.toSet() //should only contain one element

                        writeRow(
                            listOf(
                                task.uid.string,
                                task.description.name,
                                it.key.first,
                                it.key.second,
                                it.key.third,
                                status
                            )
                        )

                    }

                }

            }

        }

    }


}