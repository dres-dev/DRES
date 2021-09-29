package dev.dres.api.cli

import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.clikt.parameters.types.path
import com.jakewharton.picnic.table
import dev.dres.data.dbo.DAO
import dev.dres.data.model.Config
import dev.dres.data.model.UID
import dev.dres.data.model.basics.media.MediaCollection
import dev.dres.data.model.competition.CompetitionDescription
import dev.dres.utilities.FFmpegUtil
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

/**
 * A collection of [CliktCommand]s for user management
 *
 * @author Luca Rossetto & Ralph Gasser
 * @version 1.1
 */
class CompetitionCommand(internal val competitions: DAO<CompetitionDescription>, internal val collections: DAO<MediaCollection>, config: Config) : NoOpCliktCommand(name = "competition") {

    init {
        this.subcommands(CreateCompetitionCommand(), ListCompetitionCommand(), ShowCompetitionCommand(), PrepareCompetitionCommand(), DeleteCompetitionCommand(), CopyCompetitionCommand(), ExportCompetitionCommand(), ImportCompetitionCommand())
    }

    override fun aliases(): Map<String, List<String>> {
        return mapOf(
                "ls" to listOf("list"),
                "remove" to listOf("delete"),
                "drop" to listOf("delete"),
                "add" to listOf("create")
        )
    }

    private val taskCacheLocation = File(config.cachePath + "/tasks")

    abstract inner class AbstractCompetitionCommand(name: String, help: String, printHelpOnEmptyArgs: Boolean = true) : CliktCommand(name = name, help = help, printHelpOnEmptyArgs = printHelpOnEmptyArgs) {
        private val id: String? by option("-i", "--id")
        private val competition: String? by option("-c", "--competition")
        protected val competitionId: UID
            get() = when {
                this.id != null -> UID(this.id!!)
                this.competition != null -> this@CompetitionCommand.competitions.find { c -> c.name == this.competition!! }?.id
                        ?: UID.EMPTY
                else -> UID.EMPTY
            }
    }

    inner class CreateCompetitionCommand : CliktCommand(name = "create", help = "Creates a new Competition") {
        private val name: String by option("-n", "--name", help = "Name of the new Competition")
                .required()
                .validate { require(it.isNotEmpty()) { "Competition name must be non empty." } }

        private val description: String by option("-d", "--description", help = "Description of the new Competition")
                .required()
                .validate { require(it.isNotEmpty()) { "Competition description must be non empty." } }

        override fun run() {
            val newCompetition = CompetitionDescription(id = UID.EMPTY, name = name, description = description, taskTypes = mutableListOf(), taskGroups = mutableListOf(), teams = mutableListOf(), teamGroups = mutableListOf(), tasks = mutableListOf(), participantCanView = true)
            val id = this@CompetitionCommand.competitions.append(newCompetition)
            println("New competition '$newCompetition' created with ID=${id.string}.")
        }
    }


    inner class ListCompetitionCommand : CliktCommand(name = "list", help = "Lists an overview of all Competitions") {
        override fun run() {
            var no = 0
            println(table {
                cellStyle {
                    border = true
                    paddingLeft = 1
                    paddingRight = 1
                }
                header {
                    row("name", "id", "# teams", "# tasks", "description", "participants can view")
                }
                body {
                    this@CompetitionCommand.competitions.forEach {
                        row(it.name, it.id.string, it.teams.size, it.tasks.size, it.description, if(it.participantCanView){"x"}else{""}).also { no++ }
                    }
                }
            })
            println("Listed $no competitions")
        }
    }

    inner class ShowCompetitionCommand : AbstractCompetitionCommand(name = "show", help = "Shows details of a Competition") {

        override fun run() {
            // TODO fancification
            val competition = this@CompetitionCommand.competitions[competitionId]!!

            println("${competition.name}: ${competition.description}")
            println("Teams:")

            competition.teams.forEach(::println)

            println()
            println("Tasks:")

            competition.tasks.forEach {
                it.printOverview(System.out)
                println()
            }

            println()
        }

    }

    inner class PrepareCompetitionCommand : AbstractCompetitionCommand(name = "prepare", help = "Checks the used Media Items and generates precomputed Queries") {

        override fun run() {
            val competition = this@CompetitionCommand.competitions[competitionId]!!


            val segmentTasks = competition.getAllCachedVideoItems()

            segmentTasks.forEach {
                val item = it.item
                val collection = this@CompetitionCommand.collections[item.collection]

                if (collection == null) {
                    println("ERROR: collection ${item.collection} not found")
                    return
                }

                val videoFile = File(File(collection.basePath), item.location)

                if (!videoFile.exists()) {
                    println("ERROR: file ${videoFile.absolutePath} not found for item ${item.name}")
                    return@forEach
                }

                println("rendering ${it.item} at ${it.temporalRange}")
                FFmpegUtil.prepareMediaSegmentTask(it, collection.basePath, this@CompetitionCommand.taskCacheLocation)

            }

        }

    }

    inner class DeleteCompetitionCommand : AbstractCompetitionCommand(name = "delete", help = "Deletes a Competition") {

        override fun run() {
            val competition = this@CompetitionCommand.competitions.delete(competitionId)

            if (competition != null) {
                println("Successfully deleted $competition")
            } else {
                println("Could not find competition to delete") //should not happen
            }

        }

    }

    inner class CopyCompetitionCommand : AbstractCompetitionCommand(name = "copy", help = "Copies a Competition") {

        private val name: String by option("-n", "--name", help = "Name of the copied Competition")
                .required()
                .validate { require(it.isNotEmpty()) { "Competition name must be non empty." } }


        override fun run() {

            if (this@CompetitionCommand.competitions.any { it.name == name }) {
                println("Competition with name '$name' already exists")
                return
            }

            val competition = this@CompetitionCommand.competitions[competitionId]!!
            val newCompetition = competition.copy(id = UID.EMPTY, name = name)

            this@CompetitionCommand.competitions.append(newCompetition)

            println("Copied")
            println(competition)
            println("to")
            println(newCompetition)

        }

    }

    /**
     * Exports a specific competition to a JSON file.
     */
    inner class ExportCompetitionCommand : AbstractCompetitionCommand(name = "export", help = "Exports a competition description as JSON.") {

        private val path: Path by option("-o", "--out", help = "The destination file for the competition.").path().required()

        override fun run() {
            val competition = this@CompetitionCommand.competitions[this.competitionId]
            if (competition == null) {
                println("Competition ${this.competitionId} does not seem to exist.")
                return
            }
            val mapper = jacksonObjectMapper()
            Files.newBufferedWriter(this.path, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE).use {
                mapper.writeValue(it, competition)
            }
            println("Successfully wrote competition '${competition.name}' (ID = ${competition.id}) to $path.")
        }
    }


    /**
     * Imports a competition from a JSON file.
     */
    inner class ImportCompetitionCommand : CliktCommand(name = "import", help = "Imports a competition description from JSON.") {

        /** Flag indicating whether a new competition should be created.*/
        private val new: Boolean by option("-n", "--new", help = "Flag indicating whether a new competition should be created.").flag("-u", "--update", default = true)


        /** Flag indicating whether competition should be imported from competition file (false) or run file (true).*/
        private val run: Boolean by option("-r", "--run", help = "Flag indicating whether a new competition should be created.").flag("-c", "--competition", default = false)

        /** Path to the file that should be imported.*/
        private val path: Path by option("-i", "--in", help = "The file to import the competition from.").path().required()

        override fun run() {

            /* Read competition from file. */
            val reader = jacksonObjectMapper().readerFor(CompetitionDescription::class.java)
            val competition = Files.newBufferedReader(this.path).use {
                if (this.run) {
                    val tree = reader.readTree(it)
                    reader.readValue<CompetitionDescription>(tree["description"])
                } else {
                    reader.readValue<CompetitionDescription>(it)
                }
            }

            /* Create/update competition. */
            if (this.new) {
                val id = this@CompetitionCommand.competitions.append(competition)
                println("Successfully imported new competition '${competition.name}' (ID = $id) from $path.")
            } else {
                this@CompetitionCommand.competitions.update(competition)
                println("Successfully updated competition '${competition.name}' (ID = ${competition.id}) from $path.")
            }
        }
    }
}