package dev.dres.api.cli


import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.output.CliktHelpFormatter
import com.github.ajalt.clikt.output.HelpFormatter
import dev.dres.data.model.Config
import jetbrains.exodus.database.TransientEntityStore
import org.jline.builtins.Completers
import org.jline.builtins.Completers.TreeCompleter.node
import org.jline.reader.*
import org.jline.reader.impl.completer.AggregateCompleter
import org.jline.reader.impl.completer.StringsCompleter
import org.jline.terminal.TerminalBuilder
import java.io.IOException
import java.util.*
import java.util.regex.Pattern
import kotlin.system.exitProcess

/**
 * This is a singleton instance of the [Cli].
 *
 * @version 1.0.0
 * @author Loris Sauter
 */
object Cli {

    private const val PROMPT = "DRES> "

    private lateinit var clikt:CliktCommand

    /**
     * Blocking call that executes the CLI subsystem.
     *
     * @param store The [TransientEntityStore] instance used to access persistent data.
     * @param config The [Config] instance with which DRES was started.
     */
    fun loop(store: TransientEntityStore, config: Config) {

        clikt = DRESBaseCommand().subcommands(
            CompetitionCommand(store, config),
            UserCommand(),
            MediaCollectionCommand(store),
            CompetitionRunCommand(store),
            OpenApiCommand(),
            ExecutionCommand()
        )

        val terminal = try {
            TerminalBuilder.builder()
                //.streams(System.`in`, System.out)
                .build()
        } catch (e: IOException) {
            System.err.println("Could not initialize terminal: ${e.message}")
            exitProcess(-1)
        }

        val completer = DelegateCompleter(
            AggregateCompleter(
                StringsCompleter("quit", "exit", "help"),
                // Based on https://github.com/jline/jline3/wiki/Completion
                // However, this is not working as subcommands are not completed
                Completers.TreeCompleter(
                        clikt.registeredSubcommands().map {
                            if(it.registeredSubcommands().isNotEmpty()){
                                node(it.commandName, node(*it.registeredSubcommandNames().toTypedArray()))
                            }else{
                                node(it.commandName)
                            }
                        }
                ),
                // Pseudo-solution. Not ideal, as all subcommands are flattened
                /*AggregateCompleter(
                    StringsCompleter(clikt.registeredSubcommandNames()),
                    StringsCompleter(
                        clikt.registeredSubcommands().flatMap { it.registeredSubcommandNames() })
                ),*/
                Completers.FileNameCompleter()
            )
        )

        val lineReader = LineReaderBuilder.builder()
            .terminal(terminal)
            .completer(completer)
            .build()

        while (true) {
            try {
                val line = lineReader.readLine(PROMPT).trim()
                if (line.toLowerCase() == "exit" || line.lowercase() == "quit") {
                    break
                }
                if (line.toLowerCase() == "help") {
                    println(clikt.getFormattedHelp()) //TODO overwrite with something more useful in a cli context
                    continue
                }
                if (line.isBlank()) {
                    continue
                }

                try {
                    execute(line)
                } catch (e: Exception) {
                    when (e) {
                        is com.github.ajalt.clikt.core.NoSuchSubcommand -> println("command not found")
                        is com.github.ajalt.clikt.core.PrintHelpMessage -> println(e.command.getFormattedHelp())
                        is com.github.ajalt.clikt.core.MissingParameter -> println(e.localizedMessage)
                        is com.github.ajalt.clikt.core.NoSuchOption -> println(e.localizedMessage)
                        is com.github.ajalt.clikt.core.UsageError -> println("invalid command")
                        else -> e.printStackTrace()
                    }
                }
            } catch (e: EndOfFileException) {
                System.err.println("Could not read from terminal due to EOF. If you're running DRES in Docker, try running the container in interactive mode.")
                break
            }  catch (e: UserInterruptException) {
                break
            }
        }
    }

    fun execute(line: String){
        if(!::clikt.isInitialized){
            error("CLI not initialised. Aborting...") // Technically, this should never ever happen
        }
        clikt.parse(splitLine(line))
    }

    val lineSplitRegex: Pattern = Pattern.compile("[^\\s\"']+|\"([^\"]*)\"|'([^']*)'")

    //based on https://stackoverflow.com/questions/366202/regex-for-splitting-a-string-using-space-when-not-surrounded-by-single-or-double/366532
    @JvmStatic
    fun splitLine(line: String?): List<String> {
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

    class DRESBaseCommand : NoOpCliktCommand(name = "dres") {

        init {
            context { helpFormatter = CliHelpFormatter() }
        }

    }

    /**
     * Delegate for [Completer] to dynamically exchange and / or adapt a completer.
     * Delegates incoming completion requests to the delegate
     */
    class DelegateCompleter(var delegate: Completer) : Completer {
        override fun complete(
            reader: LineReader?,
            line: ParsedLine?,
            candidates: MutableList<Candidate>?
        ) {
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
