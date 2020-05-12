package dres.api.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands
import dres.data.dbo.DAO
import dres.data.model.run.CompetitionRun
import dres.run.RunExecutor

class CompetitionRunCommand(internal val runs: DAO<CompetitionRun>) : NoOpCliktCommand(name = "runs") {

    init {
        subcommands(ListCompetitionRunsCommand(), CompetitionRunsHistoryCommand())
    }

    inner class ListCompetitionRunsCommand: CliktCommand(name = "list", help = "Lists current Competition Runs") {


        override fun run() {


            if(RunExecutor.managers().isEmpty()){
                println("No Runs")
                return
            }

            RunExecutor.managers().forEach {
                println("Run '${it.name}' (${it.status})")
                println("Current Task: ${it.currentTask ?: "no current task"}")
                println()
            }

        }

    }

    inner class CompetitionRunsHistoryCommand: CliktCommand(name = "history", help = "Lists past Competition Runs") {


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
                    println(it.data.task)

                    println("Submissions")
                    it.data.submissions.forEach {
                        println(it)
                    }
                }
                println()
            }

        }

    }

}