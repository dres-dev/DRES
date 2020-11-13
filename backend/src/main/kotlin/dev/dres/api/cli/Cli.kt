package dev.dres.api.cli


import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.output.CliktHelpFormatter
import com.github.ajalt.clikt.output.HelpFormatter
import dev.dres.data.dbo.DataAccessLayer
import dev.dres.data.model.Config
import org.jline.builtins.Completers
import org.jline.reader.*
import org.jline.reader.impl.completer.AggregateCompleter
import org.jline.reader.impl.completer.StringsCompleter
import org.jline.terminal.Terminal
import org.jline.terminal.TerminalBuilder
import java.io.IOException
import java.util.*
import java.util.regex.Pattern


object Cli {

    private const val PROMPT = "DRES> "


    /**
     * blocking call
     */
    fun loop(dataAccessLayer: DataAccessLayer, config: Config) {

        val clikt = DRESBaseCommand().subcommands(
                CompetitionCommand(dataAccessLayer.competitions, dataAccessLayer.collections, config),
                UserCommand(),
                MediaCollectionCommand(
                        dataAccessLayer.collections,
                        dataAccessLayer.mediaItems,
                        dataAccessLayer.mediaItemPathIndex,
                        dataAccessLayer.mediaItemCollectionIndex,
                        dataAccessLayer.mediaSegments),
                CompetitionRunCommand(dataAccessLayer.runs))

        var terminal: Terminal? = null
        try {
            terminal = TerminalBuilder.terminal() //basic terminal
        } catch (e: IOException) {
            System.err.println("Could not initialize Terminal: ")
            System.err.println(e.message)
            System.err.println("Exiting...")
            System.exit(-1)
        }

        val completer = DelegateCompleter(AggregateCompleter(
                StringsCompleter("quit", "exit", "help"),
                // Based on https://github.com/jline/jline3/wiki/Completion
                // However, this is not working as subcommands are not completed
                /*Completers.TreeCompleter(
                        clikt.registeredSubcommands().map {
                            if(it.registeredSubcommands().isNotEmpty()){
                                val list = mutableListOf(it.commandName as Any)
                                list.addAll(it.registeredSubcommandNames().map { node(it) })
                                node(list.first())
                            }else{
                                node(it.commandName)
                            }
                        }
                ),*/
                // Pseudo-solution. Not ideal, as all subcommands are flattened
                AggregateCompleter(
                        StringsCompleter(clikt.registeredSubcommandNames()),
                        StringsCompleter(clikt.registeredSubcommands().flatMap { it.registeredSubcommandNames() })
                ),
                Completers.FileNameCompleter()
        ))

        val lineReader = LineReaderBuilder.builder()
                .terminal(terminal)
                .completer(completer)
                .build()

        while (true) {

            val line = lineReader.readLine(PROMPT).trim()
            if (line.toLowerCase() == "exit" || line.toLowerCase() == "quit") {
                break
            }
            if (line.toLowerCase() == "help") {
                println(clikt.getFormattedHelp()) //TODO overwrite with something more useful in a cli context
                continue
            }
            if (line.isBlank()){
                continue
            }

            try {
                clikt.parse(splitLine(line))
            } catch (e: Exception) {

                when (e) {
                    is com.github.ajalt.clikt.core.NoSuchSubcommand -> println("command not found")
                    is com.github.ajalt.clikt.core.PrintHelpMessage -> println(e.command.getFormattedHelp())
                    is com.github.ajalt.clikt.core.MissingParameter -> println(e.localizedMessage)
                    is com.github.ajalt.clikt.core.NoSuchOption -> println(e.localizedMessage)
                    else -> e.printStackTrace()
                }

            }

        }


    }

    val lineSplitRegex: Pattern = Pattern.compile("[^\\s\"']+|\"([^\"]*)\"|'([^']*)'")

    //based on https://stackoverflow.com/questions/366202/regex-for-splitting-a-string-using-space-when-not-surrounded-by-single-or-double/366532
    private fun splitLine(line: String?): List<String> {
        if (line == null || line.isEmpty()) {
            return emptyList()
        }
        val matchList: MutableList<String> = ArrayList()
        val regexMatcher = lineSplitRegex.matcher(line)
        while (regexMatcher.find()) {
            if (regexMatcher.group(1) != null) {
                // Add double-quoted string without the quotes
                matchList.add(regexMatcher.group(1))
            } else if (regexMatcher.group(2) != null) {
                // Add single-quoted string without the quotes
                matchList.add(regexMatcher.group(2))
            } else {
                // Add unquoted word
                matchList.add(regexMatcher.group())
            }
        }
        return matchList
    }

    class DRESBaseCommand : NoOpCliktCommand(name = "dres"){

        init {
            context { helpFormatter = CliHelpFormatter()}
        }

    }

    /**
     * Delegate for [Completer] to dynamically exchange and / or adapt a completer.
     * Delegates incoming completion requests to the delegate
     */
    class DelegateCompleter(var delegate: Completer):Completer{
        override fun complete(reader: LineReader?, line: ParsedLine?, candidates: MutableList<Candidate>?) {
            delegate.complete(reader, line, candidates)
        }
    }

    class CliHelpFormatter : CliktHelpFormatter() {
        override fun formatHelp(
                prolog: String,
                epilog: String,
                parameters: List<HelpFormatter.ParameterHelp>,
                programName: String
        ) = buildString {
            addOptions(parameters)
            addArguments(parameters)
            addCommands(parameters)
        }
    }

}