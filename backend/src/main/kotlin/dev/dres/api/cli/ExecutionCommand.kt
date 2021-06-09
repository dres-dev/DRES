package dev.dres.api.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.jakewharton.picnic.table

/**
 * Simple batch execution command.
 * Supports text files with line-separated DRES CLI commands.
 * Pound (`#`) prefixed lines are ignored, i.e. treated as comments.
 *
 * For convention purposes, the file suffix `ds` could be used, since it's a `dres script`.
 */
class ExecutionCommand :
    CliktCommand(name = "exec", help = "Executes a set of DRES CLI commands in a file", printHelpOnEmptyArgs = true) {


    val inFile by argument(
        "script",
        help = "The text file with line separated DRES CLI commands"
    ).file(canBeDir = false)

    val quiet: Boolean by option("-q", "--quiet").flag("--verbose", "-v", default = false)

    override fun run() {
        val lines = inFile.readLines()
        val results = mutableListOf<String>()
        val commands = mutableListOf<String>()
        lines.forEach { line ->
            try {
                if (line.isNotBlank() && !line.startsWith("#")) {
                    Cli.execute(line)
                    results.add("Success")
                    commands.add(line)
                    println()
                }
            } catch (e: Exception) {
                when (e) {
                    is com.github.ajalt.clikt.core.NoSuchSubcommand -> results.add("Unknown Command")
//                    is com.github.ajalt.clikt.core.MissingParameter -> results.add("Missing Parameter: ${e.localizedMessage}")
                    is com.github.ajalt.clikt.core.NoSuchOption -> results.add("No Such Option: ${e.localizedMessage}")
                    else -> results.add("Exception: ${e.printStackTrace()}")
                }

            }
        }
        if (!quiet) {
            println("Successfully executed ${results.count { it == "Success" }} / ${commands.size} commands")

            println(
                table {
                    cellStyle {
                        border = true
                        paddingLeft = 1
                        paddingRight = 1
                    }
                    header {
                        row("Command", "Result")
                    }
                    body {
                        commands.forEachIndexed { idx, command ->
                            row(command, results[idx])
                        }
                    }
                }
            )
        }
    }

}
