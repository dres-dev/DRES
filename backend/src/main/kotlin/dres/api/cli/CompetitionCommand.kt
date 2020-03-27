package dres.api.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.validate
import dres.data.dbo.DAO
import dres.data.model.admin.PlainPassword
import dres.data.model.admin.UserName
import dres.data.model.competition.Competition

class CompetitionCommand(val competitions: DAO<Competition>) : NoOpCliktCommand(name = "competition") {

    init {
        this.subcommands(CreateCompetitionCommand(), ListCompetitionCommand(), ShowCompetitionCommand())
    }

    abstract inner class AbstractCompetitionCommand(private val name: String) : CliktCommand(name = name) {

        protected val competitionId: Long by option("-c", "--competition")
                .convert { this@CompetitionCommand.competitions.find { c -> c.name == it }?.id ?: -1 }
                .required()
                .validate { require(it > -1) {"Competition not found"} }

    }

    inner class CreateCompetitionCommand(): CliktCommand(name = "create") {
        private val name: String by option("-n", "--name")
                .required()
                .validate { require(it.isNotEmpty()) { "Competition name must be non empty." } }

        private val description: String by option("-d", "--description")
                .required()
                .validate {require(it.isNotEmpty()) { "Competition description must be non empty." } }

        override fun run() {
            val newCompetition = Competition(id = -1, name = name, description = description, teams = emptyList(), tasks = emptyList())
            val id = this@CompetitionCommand.competitions.append(newCompetition)
            println("New competition '$newCompetition' created with ID=$id.")
        }
    }


    inner class ListCompetitionCommand : CliktCommand(name = "list") {
        override fun run() {
            println("Competitions:")
            this@CompetitionCommand.competitions.forEach {
                println("${it.name} (${it.teams.size} Teams, ${it.tasks.size} Tasks): ${it.description}")
            }
        }
    }

    inner class ShowCompetitionCommand : AbstractCompetitionCommand(name = "show") {

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

}