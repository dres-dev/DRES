package dev.dres.api.cli

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.clikt.parameters.types.path
import com.jakewharton.picnic.table
import dev.dres.data.model.Config
import dev.dres.data.model.template.EvaluationTemplate
import dev.dres.utilities.FFmpegUtil
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.*
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.*

/**
 * A collection of [CliktCommand]s for [EvaluationTemplate] management.
 *
 * @author Luca Rossetto
 * @author Ralph Gasser
 * @version 2.0.0
 */
class EvaluationCommand(private val store: TransientEntityStore, config: Config) : NoOpCliktCommand(name = "competition") {

    init {
        this.subcommands(
            Create(),
            List(),
            Show(),
            Prepare(),
            Delete(),
            Copy(),
            Export(),
            Import()
        )
    }

    override fun aliases(): Map<String, kotlin.collections.List<String>> {
        return mapOf(
            "ls" to listOf("list"),
            "remove" to listOf("delete"),
            "drop" to listOf("delete"),
            "add" to listOf("create")
        )
    }

    /** The cache location [Paths]. */
    private val cacheLocation = Paths.get(config.cachePath, "tasks")

    abstract inner class AbstractEvaluationCommand(name: String, help: String, printHelpOnEmptyArgs: Boolean = true) : CliktCommand(name = name, help = help, printHelpOnEmptyArgs = printHelpOnEmptyArgs) {
        protected val id: String? by option("-i", "--id")
        protected val name: String? by option("-c", "--competition")
    }

    /**
     * [CliktCommand] to create a new [EvaluationTemplate].
     */
    inner class Create : CliktCommand(name = "create", help = "Creates a new Competition") {

        private val name: String by option("-c", "--competition", help = "Name of the new Competition")
            .required()
            .validate { require(it.isNotEmpty()) { "Competition description must be non empty." } }

        private val description: String by option("-d", "--description", help = "Description of the new Competition")
                .required()
                .validate { require(it.isNotEmpty()) { "Competition description must be non empty." } }

        override fun run()  {
            val newCompetition = this@EvaluationCommand.store.transactional {
                EvaluationTemplate.new {
                    this.name = this@Create.name
                    this.description = this@Create.description
                }
            }
            println("New competition '$newCompetition' created with ID = ${newCompetition.id}.")
        }
    }

    /**
     * [CliktCommand] to delete a [EvaluationTemplate].
     */
    inner class Delete : AbstractEvaluationCommand(name = "delete", help = "Deletes a competition") {
        override fun run() {
            this@EvaluationCommand.store.transactional {
                val competition = EvaluationTemplate.query((EvaluationTemplate::id eq this.id).or(EvaluationTemplate::name eq this.name)).firstOrNull()
                if (competition == null) {
                    println("Could not find competition to delete.")
                    return@transactional
                }
                competition.delete()
            }
            println("Successfully deleted competition description.")
        }
    }

    /**
     * [CliktCommand] to copy a [EvaluationTemplate].
     */
    inner class Copy : AbstractEvaluationCommand(name = "copy", help = "Copies a Competition") {
        override fun run() {
            this@EvaluationCommand.store.transactional {
                val competition = EvaluationTemplate.query((EvaluationTemplate::id eq this.id).or(EvaluationTemplate::name eq this.name)).firstOrNull()
                if (competition == null) {
                    println("Could not find competition to copy.")
                    return@transactional
                }

                /* TODO: Copy competition. */
            }
            println("Successfully copied competition.")
        }
    }

    /**
     * [CliktCommand] to list all [EvaluationTemplate]s.
     */
    inner class List : CliktCommand(name = "list", help = "Lists an overview of all Competitions") {
        override fun run() = this@EvaluationCommand.store.transactional(true) {
            var no = 0
            println(table {
                cellStyle {
                    border = true
                    paddingLeft = 1
                    paddingRight = 1
                }
                header {
                    row("name", "id", "# teams", "# tasks", "description", )
                }
                body {
                    EvaluationTemplate.all().asSequence().forEach { c ->
                        row(c.name, c.id, c.teams.size(), c.tasks.size(), c.description).also { no++ }
                    }
                }
            })
            println("Listed $no competitions")
        }
    }

    /**
     * [CliktCommand] to show a specific [EvaluationTemplate].
     */
    inner class Show : AbstractEvaluationCommand(name = "show", help = "Shows details of a Competition") {
        override fun run() = this@EvaluationCommand.store.transactional(true) {
            val competition = EvaluationTemplate.query(
                (EvaluationTemplate::id eq this.id).or(EvaluationTemplate::name eq this.name)
            ).firstOrNull()

            if (competition == null) {
                println("Could not find specified competition description.")
                return@transactional
            }

            println("${competition.name}: ${competition.description}")
            println("Teams:")

            competition.teams.asSequence().forEach(::println)

            println()
            println("Tasks:")

            competition.tasks.asSequence().forEach { _ ->
                /* TODO: it.printOverview(System.out) */
                println()
            }
            println()
        }

    }

    /**
     * [CliktCommand] to prepare a specific [EvaluationTemplate].
     */
    inner class Prepare : AbstractEvaluationCommand(name = "prepare", help = "Checks the used Media Items and generates precomputed Queries") {

        override fun run() = this@EvaluationCommand.store.transactional(true) {
            val competition = EvaluationTemplate.query(
                (EvaluationTemplate::id eq this.id).or(EvaluationTemplate::name eq this.name)
            ).firstOrNull()

            if (competition == null) {
                println("Could not find specified competition description.")
                return@transactional
            }

            /* Fetch all videos in the competition. */
            val videos = competition.getAllVideos()
            videos.forEach { item ->
                val path = item.first.pathToOriginal()
                if (!Files.exists(path)) {
                    println("ERROR: Media file $path not found for item ${item.first.name}")
                    return@forEach
                }

                println("Rendering ${item.first.name}$ at ${item.second}")
                FFmpegUtil.extractSegment(item.first, item.second, this@EvaluationCommand.cacheLocation)
            }
        }
    }


    /**
     * Exports a specific competition to a JSON file.
     */
    inner class Export : AbstractEvaluationCommand(name = "export", help = "Exports a competition description as JSON.") {

        /** Path to the file that should be created .*/
        private val path: Path by option("-o", "--out", help = "The destination file for the competition.").path().required()

        /** Flag indicating whether export should be pretty printed.*/
        private val pretty: Boolean by option("-p", "--pretty", help = "Flag indicating whether exported JSON should be pretty printed.").flag("-u", "--ugly", default = true)

        override fun run() = this@EvaluationCommand.store.transactional(true) {
            val competition = EvaluationTemplate.query(
                (EvaluationTemplate::id eq this.id).or(EvaluationTemplate::name eq this.name)
            ).firstOrNull()

            if (competition == null) {
                println("Could not find specified competition description.")
                return@transactional
            }

            val mapper = jacksonObjectMapper()
            Files.newBufferedWriter(this.path, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE).use {
                val writer = if (this.pretty) {
                    mapper.writerWithDefaultPrettyPrinter()
                } else {
                    mapper.writer()
                }
                writer.writeValue(it, competition)
            }
            println("Successfully wrote competition '${competition.name}' (ID = ${competition.id}) to $path.")
        }
    }


    /**
     * Imports a competition from a JSON file.
     */
    inner class Import : CliktCommand(name = "import", help = "Imports a competition description from JSON.") {

        /** Flag indicating whether a new competition should be created.*/
        private val new: Boolean by option("-n", "--new", help = "Flag indicating whether a new competition should be created.").flag("-u", "--update", default = true)

        /** Path to the file that should be imported.*/
        private val path: Path by option("-i", "--in", help = "The file to import the competition from.").path().required()

        override fun run() {
            /* TODO: Probably won't work this way. */
            /* Read competition from file */
            /*val reader = jacksonObjectMapper().readerFor(CompetitionDescription::class.java)
            val competition = try {
                Files.newBufferedReader(this.path).use {
                    val tree = reader.readTree(it)
                    if (tree.get("id") != null && (tree.get("description") == null || tree.get("description").isNull || tree.get("description").isTextual)) {
                        reader.readValue<CompetitionDescription>(tree)
                    } else if (tree.get("id") != null && tree.get("description") != null && tree.get("description").isObject) {
                        reader.readValue<CompetitionDescription>(tree["description"])
                    } else {
                        null
                    }
                }
            } catch (e: Throwable) {
                println("Could not import competition from $path: ${e.message}.")
                return
            }

            /* Create/update competition. */
            if (competition != null) {
                if (this.new) {
                    val id = this@CompetitionCommand.competitions.append(competition)
                    println("Successfully imported new competition '${competition.name}' (ID = $id) from $path.")
                } else {
                    this@CompetitionCommand.competitions.update(competition)
                    println("Successfully updated competition '${competition.name}' (ID = ${competition.id}) from $path.")
                }
            } else {
                println("Could not import competition from $path: Unknown format.")
            }*/
        }
    }
}