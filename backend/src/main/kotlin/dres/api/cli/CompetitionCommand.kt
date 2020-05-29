package dres.api.cli

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.clikt.parameters.types.long
import dres.data.dbo.DAO
import dres.data.model.Config
import dres.data.model.basics.media.MediaCollection
import dres.data.model.competition.CompetitionDescription
import dres.data.model.competition.TaskDescriptionBase
import dres.data.model.competition.interfaces.MediaSegmentTaskDescription
import dres.utilities.FFmpegUtil
import java.io.File
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

class CompetitionCommand(internal val competitions: DAO<CompetitionDescription>, internal val collections: DAO<MediaCollection>, config: Config) : NoOpCliktCommand(name = "competition") {

    init {
        this.subcommands(CreateCompetitionCommand(), ListCompetitionCommand(), ShowCompetitionCommand(), PrepareCompetitionCommand(), DeleteCompetitionCommand(), CopyCompetitionCommand(), ExportCompetitionCommand())
    }

    private val taskCacheLocation = File(config.cachePath + "/tasks")

    abstract inner class AbstractCompetitionCommand(name: String, help: String) : CliktCommand(name = name, help = help) {
        private val id: Long? by option("-i", "--id").long()
        private val competition: String? by option("-c", "--competition")
        protected val competitionId: Long
            get() = when {
                this.id != null -> this.id!!
                this.competition != null -> this@CompetitionCommand.competitions.find { c -> c.name == this.competition!! }?.id ?: -1
                else -> 0
            }
    }

    inner class CreateCompetitionCommand : CliktCommand(name = "create", help = "Creates a new Competition") {
        private val name: String by option("-n", "--name", help = "Name of the new Competition")
                .required()
                .validate { require(it.isNotEmpty()) { "Competition name must be non empty." } }

        private val description: String by option("-d", "--description", help = "Description of the new Competition")
                .required()
                .validate {require(it.isNotEmpty()) { "Competition description must be non empty." } }

        override fun run() {
            val newCompetition = CompetitionDescription(id = -1, name = name, description = description, groups = mutableListOf(), teams = mutableListOf(), tasks = mutableListOf())
            val id = this@CompetitionCommand.competitions.append(newCompetition)
            println("New competition '$newCompetition' created with ID=$id.")
        }
    }


    inner class ListCompetitionCommand : CliktCommand(name = "list", help = "Lists an overview of all Competitions") {
        override fun run() {
            println("Competitions:")
            this@CompetitionCommand.competitions.forEach {
                println("${it.name}[${it.id}] (${it.teams.size} Teams, ${it.tasks.size} Tasks): ${it.description}")
            }
        }
    }

    inner class ShowCompetitionCommand : AbstractCompetitionCommand(name = "show", help = "Shows details of a Competition") {

        override fun run() {
            val competition = this@CompetitionCommand.competitions[competitionId]!!

            println("${competition.name}: ${competition.description}")
            println("Teams:")

            competition.teams.forEach (::println)

            println()
            println("Tasks:")

            competition.tasks.forEach {
                when(it){
                    is TaskDescriptionBase.KisVisualTaskDescription -> {
                        println("Visual Known Item Search Task '${it.name}' (${it.taskGroup.name}, ${it.taskGroup.type})")
                        println("Target: ${it.item.name} ${it.temporalRange.start} to ${it.temporalRange.end}")
                        println("Task Duration: ${it.duration}")
                    }
                    is TaskDescriptionBase.KisTextualTaskDescription -> {
                        println("Textual Known Item Search Task '${it.name}' (${it.taskGroup.name}, ${it.taskGroup.type})")
                        println("Target: ${it.item.name} ${it.temporalRange.start} to ${it.temporalRange.end}")
                        println("Task Duration: ${it.duration}")
                        println("Query Text:")
                        it.descriptions.forEach(::println)
                    }
                    is TaskDescriptionBase.AvsTaskDescription -> {
                        println("Ad-hoc Video Search Task '${it.name}' (${it.taskGroup.name}, ${it.taskGroup.type})")
                        println("Task Duration: ${it.duration}")
                    }
                }
                println()
            }

            println()
        }

    }

    inner class PrepareCompetitionCommand : AbstractCompetitionCommand(name = "prepare", help = "Checks the used Media Items and generates precomputed Queries") {

        override fun run() {
            val competition = this@CompetitionCommand.competitions[competitionId]!!



            val segmentTasks = competition.tasks.filterIsInstance(MediaSegmentTaskDescription::class.java)

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

                println("rendering ${it.name}")
                FFmpegUtil.prepareMediaSegmentTask(it, collection.basePath, this@CompetitionCommand.taskCacheLocation)

            }

        }

    }

    inner class DeleteCompetitionCommand : AbstractCompetitionCommand(name = "delete", help = "Deletes a Competition") {

        override fun run() {
            val competition = this@CompetitionCommand.competitions.delete(competitionId)

            if (competition != null){
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

            if (this@CompetitionCommand.competitions.any { it.name == name }){
                println("Competition with name '$name' already exists")
                return
            }

            val competition = this@CompetitionCommand.competitions[competitionId]!!
            val newCompetition = competition.copy(id = -1, name = name)

            this@CompetitionCommand.competitions.append(newCompetition)

            println("Copied")
            println(competition)
            println("to")
            println(newCompetition)

        }

    }

    /**
     * Exports a specific competition as JSON.
     */
    inner class ExportCompetitionCommand : AbstractCompetitionCommand(name ="export", help="Exports a competition description as JSON."){

        private val destination: String by option("-o", "--out", help="The destination file for the competition").required()

        override fun run() {
            val competition = this@CompetitionCommand.competitions[this.competitionId]
            if (competition == null) {
                println("Competition does not seem to exist.")
                return
            }

            val path = Paths.get(this.destination)
            val mapper = ObjectMapper()
            java.nio.file.Files.newBufferedWriter(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE).use {
                mapper.writeValue(it, competition)
            }
            println("Successfully wrote competition '${competition.name}' to $path.")
        }
    }
}