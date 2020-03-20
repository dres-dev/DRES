package dres.api.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument

class CompetitionCommand : CliktCommand(name = "competition") {

    val action by argument()

    override fun run() {
        println("competition action selected: $action")
        //TODO("Not yet implemented")
    }
}