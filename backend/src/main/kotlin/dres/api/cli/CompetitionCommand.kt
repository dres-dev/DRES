package dres.api.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.validate
import dres.data.dbo.DAO
import dres.data.model.basics.media.MediaCollection
import dres.data.model.competition.CompetitionDescription
import dres.data.model.competition.interfaces.MediaSegmentTaskDescription
import dres.utilities.FFmpegUtil
import java.io.File

class CompetitionCommand(internal val competitions: DAO<CompetitionDescription>, internal val collections: DAO<MediaCollection>) : NoOpCliktCommand(name = "competition") {

    init {
        this.subcommands(CreateCompetitionCommand(), ListCompetitionCommand(), ShowCompetitionCommand(), PrepareCompetitionCommand())
    }

    abstract inner class AbstractCompetitionCommand(name: String, help: String) : CliktCommand(name = name, help = help) {

        protected val competitionId: Long by option("-c", "--competition")
                .convert { this@CompetitionCommand.competitions.find { c -> c.name == it }?.id ?: -1 }
                .required()
                .validate { require(it > -1) {"Competition not found"} }

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
                println("${it.name} (${it.teams.size} Teams, ${it.tasks.size} Tasks): ${it.description}")
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

            competition.tasks.forEach(::println)

            println()
        }

    }

    inner class PrepareCompetitionCommand : AbstractCompetitionCommand(name = "prepare", help = "Checks the used Media Items and generates precomputed Queries") {

        private val cacheLocation = File("task-cache") //TODO make configurable

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
                FFmpegUtil.prepareMediaSegmentTask(it, collection.basePath!!, cacheLocation)

            }

        }

    }

}