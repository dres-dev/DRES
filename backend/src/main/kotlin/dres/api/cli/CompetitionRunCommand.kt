package dres.api.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands
import dres.run.RunExecutor

class CompetitionRunCommand : NoOpCliktCommand(name = "runs") {

    init {
        subcommands(ListCompetitionRunsCommand())
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

}