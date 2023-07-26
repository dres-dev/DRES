package dev.dres.api.cli

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.path
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import com.jakewharton.picnic.table
import dev.dres.data.model.template.task.DbTargetType
import dev.dres.data.model.run.*
import dev.dres.data.model.run.interfaces.EvaluationId
import dev.dres.data.model.run.interfaces.Run
import dev.dres.data.model.submissions.DbSubmission
import dev.dres.data.model.submissions.DbVerdictStatus
import dev.dres.data.model.template.task.DbTaskTemplate
import dev.dres.run.*
import dev.dres.utilities.extensions.toDateString
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.*
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

/**
 * A collection of [CliktCommand]s for [Run] management.
 *
 * @author Luca Rossetto
 * @author Ralph Gasser
 * @version 2.0.0
 */
class EvaluationCommand(internal val store: TransientEntityStore) : NoOpCliktCommand(name = "evaluation") {

    init {
        subcommands(
            Ongoing(),
            List(),
            Delete(),
            Export(),
            Reactivate(),
            History(),
            ResetSubmission(),
            ExportJudgements()
        )
    }

    override fun aliases(): Map<String, kotlin.collections.List<String>> {
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
    inner class Ongoing :
        CliktCommand(name = "ongoing", help = "Lists all ongoing evaluations.") {
        private val plain by option("-p", "--plain", help = "Plain print. No fancy tables").flag(default = false)
        override fun run() = this@EvaluationCommand.store.transactional(true) {
            if (RunExecutor.managers().isEmpty()) {
                println("No evaluations are currently ongoing!")
                return@transactional
            }
            if (this.plain) {
                RunExecutor.managers().filterIsInstance(InteractiveRunManager::class.java).forEach {
                    println("${RunSummary(it.id, it.name, it.template.description, it.currentTaskTemplate(RunActionContext.INTERNAL).name)} (${it.status})")
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
                                        it.id,
                                        "Synchronous",
                                        it.name,
                                        it.template.description,
                                        it.currentTaskTemplate(RunActionContext.INTERNAL).name,
                                        it.status
                                    )
                                    is InteractiveAsynchronousRunManager -> row(
                                        it.id,
                                        "Asynchronous",
                                        it.name,
                                        it.template.description,
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
     * [CliktCommand] to list all evaluation runs (ongoing and past) for the current DRES instance.
     */
    inner class List : CliktCommand(name = "list", help = "Lists all (ongoing and past) evaluations.") {
        private val plain by option("-p", "--plain", help = "Plain print. No fancy tables").flag(default = false)
        override fun run() = this@EvaluationCommand.store.transactional(true) {
            val query =  DbEvaluation.all().sortedBy(DbEvaluation::started)
            if (this.plain) {
                query.asSequence().forEach {
                    println(
                        "${
                            RunSummary(
                                it.id,
                                it.name,
                                it.template.description,
                                if (it.type == DbEvaluationType.INTERACTIVE_SYNCHRONOUS) {
                                    it.tasks.firstOrNull()?.template?.name ?: "N/A"
                                } else {
                                    "N/A"
                                }
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
                            row("id", "name", "description", "type", "lastTask", "status", "start", "end")
                        }
                        body {
                            query.asSequence().forEach {
                                row(
                                    it.id,
                                    it.name,
                                    it.template.description,
                                    it.type.description,
                                    if (it.type == DbEvaluationType.INTERACTIVE_SYNCHRONOUS) {
                                        it.tasks.firstOrNull()?.template?.name ?: "N/A"
                                    } else {
                                        "N/A"
                                    },
                                    it.status.description,
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
     * [CliktCommand] to list past evaluation runs for the current DRES instance.
     */
    inner class History : CliktCommand(name = "history", help = "Lists past evaluations.") {
        // TODO fancification with table

        override fun run() = this@EvaluationCommand.store.transactional(true) {
            val query =  DbEvaluation.query(DbEvaluation::ended ne null).sortedBy(DbEvaluation::started)
            query.asSequence().forEach {
                println(it.name)

                println("Teams:")
                it.template.teams.asSequence().forEach { team ->
                    println(team)
                }

                println()
                println("All Tasks:")
                it.template.tasks.sortedBy(DbTaskTemplate::idx).asSequence().forEach { task ->
                    println(task)
                }

                println()
                println("Evaluated Tasks:")
                it.tasks.asSequence().forEach { t ->
                    println(t.template)
                    if (t.evaluation.type == DbEvaluationType.INTERACTIVE_SYNCHRONOUS) {
                        println("Submissions")
                        t.answerSets.asSequence().forEach { s -> println(s) }
                    }
                }
                println()
            }
        }
    }

    /**
     * Deletes a selected [DbEvaluation] for the current DRES instance.
     */
    inner class Delete : CliktCommand(name = "delete", help = "Deletes an existing evaluation.", printHelpOnEmptyArgs = true) {
        private val id: EvaluationId by option("-r", "--run").required()

        override fun run() {
            if (RunExecutor.managers().any { it.id == id }) {
                println("Evaluation with ID $id could not be deleted because it is still running! Terminate it and try again.")
                return
            }

            this@EvaluationCommand.store.transactional {
                val evaluation = DbEvaluation.query(DbEvaluation::id eq this.id).firstOrNull()
                if (evaluation == null) {
                    println("Evaluation with ID ${this.id} could not be deleted because it doesn't exist!")
                    return@transactional
                }
                evaluation.delete()
            }
        }
    }

    /**
     *  [CliktCommand] to export a specific competition run as JSON.
     */
    inner class Export : CliktCommand(name = "export", help = "Exports the selected evaluation to a JSON file.", printHelpOnEmptyArgs = true) {

        /** [EvaluationId] of the [DbEvaluation] that should be exported. .*/
        private val id: EvaluationId by option("-i", "--id").required()

        /** Path to the file that should be created .*/
        private val path: Path by option("-o", "--output").path().required()

        /** Flag indicating whether export should be pretty printed.*/
        private val pretty: Boolean by option("-p", "--pretty", help = "Flag indicating whether exported JSON should be pretty printed.").flag("-u", "--ugly", default = true)

        override fun run() = this@EvaluationCommand.store.transactional(true) {
            val evaluation = DbEvaluation.query(DbEvaluation::id eq this.id).firstOrNull()
            if (evaluation == null) {
                println("Evaluation ${this.id} does not seem to exist.")
                return@transactional
            }

            val mapper = jacksonObjectMapper()
            Files.newBufferedWriter(path, StandardOpenOption.WRITE, StandardOpenOption.CREATE).use {
                /*val writer = if (this.pretty) {
                    mapper.writerWithDefaultPrettyPrinter()
                } else {
                    mapper.writer()
                }
                writer.writeValue(it, run)*/
                // TODO: Export must be re-conceived based on API classes.
            }
            println("Successfully exported evaluation ${this.id} to $path.")
        }
    }

    /**
     * [CliktCommand] to reactivate an [DbEvaluation] that has previously ended.
     */
    inner class Reactivate : CliktCommand(name = "reactivate", help = "Reactivates a previously ended evaluation", printHelpOnEmptyArgs = true) {

        /** [EvaluationId] of the [DbEvaluation] that should be reactivated. .*/
        private val id: EvaluationId by option("-i", "--id").required()

        override fun run() {
            val run = this@EvaluationCommand.store.transactional {
                val evaluation = DbEvaluation.query(DbEvaluation::id eq this.id).firstOrNull()
                if (evaluation == null) {
                    println("Evaluation ${this.id} does not seem to exist.")
                    return@transactional null
                }

                if (evaluation.ended == null) {
                    println("Evaluation has not ended yet.")
                    return@transactional null
                }

                if (RunExecutor.managers().any { it.id == evaluation.id }) {
                    println("Evaluation is already active.")
                    return@transactional null
                }

                /* Create run and reactivate. */
                val manager = evaluation.toRunManager(this@EvaluationCommand.store)
                manager.evaluation.reactivate()
                manager
            }

            if (run != null) {
                this@EvaluationCommand.store.transactional {
                    RunExecutor.schedule(run)
                }
            }


            println("Evaluation ${this.id} was reactivated.")
        }
    }

    /**
     * [CliktCommand] to reset the status of [DbSubmission]s.
     */
    inner class ResetSubmission : CliktCommand(name = "resetSubmission", help = "Resets submission status to INDETERMINATE for selected submissions.", printHelpOnEmptyArgs = true) {

        /** [EvaluationId] of the [DbEvaluation] that should be reactivated. .*/
        private val id: EvaluationId by option("-i", "--id").required()

        /** The [EvaluationId]s to reset. */
        private val submissionIds: kotlin.collections.List<EvaluationId> by option("-s", "--submissions", help = "IDs of the submissions to reset.").multiple()

        /** The [EvaluationId]s to reset [DbSubmission]s for. */
        private val taskIds: kotlin.collections.List<EvaluationId> by option("-t", "--tasks", help = "IDs of the tasks to resetsubmissions for.").multiple()

        /** The names of the task groups to reset [DbSubmission]s for. */
        private val taskGroups: kotlin.collections.List<String> by option("-g", "--groups", help = "Names of the task groups to reset submissions for.").multiple()

        override fun run() = this@EvaluationCommand.store.transactional {
            /* Fetch competition run. */
            val evaluation = DbEvaluation.query(DbEvaluation::id eq this.id).firstOrNull()
            if (evaluation == null) {
                println("Evaluation ${this.id} does not seem to exist.")
                return@transactional
            }

            if (evaluation.type == DbEvaluationType.INTERACTIVE_SYNCHRONOUS) {
                /* Prepare query. */
                var query = if (this.taskIds.isNotEmpty()) {
                    evaluation.tasks.filter { it.id.isIn(this@ResetSubmission.taskIds) }.flatMapDistinct { it.answerSets }
                } else if (this.taskGroups.isNotEmpty()) {
                    evaluation.tasks.filter { it.template.taskGroup.name.isIn(this@ResetSubmission.taskGroups) }.flatMapDistinct { it.answerSets }
                } else {
                    evaluation.tasks.flatMapDistinct { it.answerSets }
                }

                if (this.submissionIds.isNotEmpty()) {
                    query = query.filter { it.id.isIn(this@ResetSubmission.submissionIds) }
                }

                var affected = 0
                query.asSequence().forEach {
                    affected += 1
                    it.status = DbVerdictStatus.INDETERMINATE
                }

                println("Successfully reset $affected} submissions.")
            } else {
                println("Operation not supported for run type")
            }
        }
    }

    /**
     * [CliktCommand] to export judgements made for relevant tasks as CSVs.
     */
    inner class ExportJudgements : CliktCommand(name = "exportJudgements", help = "Exports all judgements made for all relevant tasks of an evaluation as CSV", printHelpOnEmptyArgs = true) {
        /** [EvaluationId] of the [DbEvaluation] for which judgements should be exported.*/
        private val id: EvaluationId by option("-r", "--run", help = "Id of the run").required()

        /** The [Path] to the output file. */
        private val path: Path by option("-o", "--output", help = "Path to the file the judgements are to be exported to.").convert { Paths.get(it) }.required()

        private val header = listOf("TaskId", "TaskName", "ItemId", "StartTime", "EndTime", "Status")

        override fun run() = this@EvaluationCommand.store.transactional(true) {
            /* Fetch competition run. */
            val evaluation = DbEvaluation.query(DbEvaluation::id eq this.id).firstOrNull()
            if (evaluation == null) {
                println("Evaluation ${this.id} does not seem to exist.")
                return@transactional
            }

            val tasks = evaluation.tasks.filter {
                it.template.targets.filter { it.type.isIn(listOf(DbTargetType.JUDGEMENT,DbTargetType.JUDGEMENT_WITH_VOTE)) }.isNotEmpty()
            }

            if (tasks.isEmpty) {
                println("No judged tasks in run.")
                return@transactional
            }

            Files.newOutputStream(this.path, StandardOpenOption.WRITE).use { os ->
                csvWriter().open(os) {
                    writeRow(header)
                    tasks.asSequence().forEach { task ->
                        val submittedItems = task.answerSets.asSequence().groupBy { s ->
                            Triple(s.answers.firstOrNull()?.item?.name?: "unknown", s.answers.firstOrNull()?.start, s.answers.firstOrNull()?.end) //TODO flatten?
                        }
                        submittedItems.entries.forEach { items ->
                            val status = items.value.map { s -> s.status }.toSet() //should only contain one element
                            writeRow(
                                listOf(task.id, task.template.name, items.key.first, items.key.second, items.key.third, status)
                            )
                        }
                    }
                }
            }
        }
    }
}