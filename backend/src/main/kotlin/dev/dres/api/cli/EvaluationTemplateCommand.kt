package dev.dres.api.cli

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.clikt.parameters.types.path
import com.jakewharton.picnic.table
import dev.dres.DRES
import dev.dres.api.rest.types.template.ApiEvaluationTemplate
import dev.dres.data.model.template.DbEvaluationTemplate
import dev.dres.data.model.template.task.DbTaskTemplate
import dev.dres.mgmt.cache.CacheManager
import dev.dres.utilities.TemplateUtil
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.*
import org.joda.time.DateTime
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.concurrent.TimeUnit

/**
 * A collection of [CliktCommand]s for [DbEvaluationTemplate] management.
 *
 * @author Luca Rossetto
 * @author Ralph Gasser
 * @version 2.0.0
 */
class EvaluationTemplateCommand(private val store: TransientEntityStore, private val cache: CacheManager) :
    NoOpCliktCommand(name = "template") {

    init {
        this.subcommands(
            Create(),
            List(),
            Show(),
            Prepare(),
            Delete(),
            Copy(),
            Export(),
            Import()
        )
    }

    override fun aliases(): Map<String, kotlin.collections.List<String>> {
        return mapOf(
            "ls" to listOf("list"),
            "remove" to listOf("delete"),
            "drop" to listOf("delete"),
            "add" to listOf("create")
        )
    }

    abstract inner class AbstractEvaluationCommand(name: String, help: String, printHelpOnEmptyArgs: Boolean = true) :
        CliktCommand(name = name, help = help, printHelpOnEmptyArgs = printHelpOnEmptyArgs) {
        protected val id: String? by option("-i", "--id")
        protected val name: String? by option("-t", "--template")
    }

    /**
     * [CliktCommand] to create a new [DbEvaluationTemplate].
     */
    inner class Create : CliktCommand(name = "create", help = "Creates a new Template") {

        private val name: String by option("-c", "--competition", help = "Name of the new Template")
            .required()
            .validate { require(it.isNotEmpty()) { "Template description must be non empty." } }

        private val description: String by option("-d", "--description", help = "Description of the new Template")
            .required()
            .validate { require(it.isNotEmpty()) { "Template description must be non empty." } }

        override fun run() {
            val newCompetition = this@EvaluationTemplateCommand.store.transactional {
                DbEvaluationTemplate.new {
                    this.name = this@Create.name
                    this.description = this@Create.description
                    this.created = DateTime.now()
                    this.modified = DateTime.now()
                }.toApi()
            }
            println("New template '$newCompetition' created with ID = ${newCompetition.id}.")
        }
    }

    /**
     * [CliktCommand] to delete a [DbEvaluationTemplate].
     */
    inner class Delete : AbstractEvaluationCommand(name = "delete", help = "Deletes a template") {
        override fun run() {
            this@EvaluationTemplateCommand.store.transactional {
                val competition =
                    DbEvaluationTemplate.query((DbEvaluationTemplate::id eq this.id).or(DbEvaluationTemplate::name eq this.name))
                        .firstOrNull()
                if (competition == null) {
                    println("Could not find template to delete.")
                    return@transactional
                }
                competition.delete()
            }
            println("Successfully deleted template.")
        }
    }

    /**
     * [CliktCommand] to copy a [DbEvaluationTemplate].
     */
    inner class Copy : AbstractEvaluationCommand(name = "copy", help = "Copies a Template") {
        override fun run() {
            this@EvaluationTemplateCommand.store.transactional {
                val evaluationTemplate =
                    DbEvaluationTemplate.query((DbEvaluationTemplate::id eq this.id).or(DbEvaluationTemplate::name eq this.name))
                        .firstOrNull()
                if (evaluationTemplate == null) {
                    println("Could not find template to copy.")
                    return@transactional
                }

                TemplateUtil.copyTemplate(evaluationTemplate)
                println("template copied")
            }
            //println("Successfully copied template.")
        }
    }

    /**
     * [CliktCommand] to list all [DbEvaluationTemplate]s.
     */
    inner class List : CliktCommand(name = "list", help = "Lists an overview of all Templates") {
        override fun run() = this@EvaluationTemplateCommand.store.transactional(true) {
            var no = 0
            println(table {
                cellStyle {
                    border = true
                    paddingLeft = 1
                    paddingRight = 1
                }
                header {
                    row("name", "id", "# teams", "# tasks", "description")
                }
                body {
                    DbEvaluationTemplate.all().filter { it.instance eq false }.asSequence().forEach { c ->
                        row(c.name, c.id, c.teams.size(), c.tasks.size(), c.description).also { no++ }
                    }
                }
            })
            println("Listed $no templates")
        }
    }

    /**
     * [CliktCommand] to show a specific [DbEvaluationTemplate].
     */
    inner class Show : AbstractEvaluationCommand(name = "show", help = "Shows details of a Template") {
        override fun run() = this@EvaluationTemplateCommand.store.transactional(true) {
            val template = DbEvaluationTemplate.query(
                (DbEvaluationTemplate::id eq this.id).or(DbEvaluationTemplate::name eq this.name)
            ).firstOrNull()

            if (template == null) {
                println("Could not find specified template.")
                return@transactional
            }

            println("${template.name}: ${template.description}")
            println("Teams:")

            template.teams.asSequence().forEach(::println)

            println()
            println("Tasks:")

            template.tasks.sortedBy(DbTaskTemplate::idx).asSequence().forEach { _ ->
                /* TODO: it.printOverview(System.out) */
                println()
            }
            println()
        }

    }

    /**
     * [CliktCommand] to prepare a specific [DbEvaluationTemplate].
     */
    inner class Prepare : AbstractEvaluationCommand(
        name = "prepare",
        help = "Checks the used media items and generates precomputed previews."
    ) {

        override fun run() = this@EvaluationTemplateCommand.store.transactional(true) {
            val competition = DbEvaluationTemplate.query(
                (DbEvaluationTemplate::id eq this.id).or(DbEvaluationTemplate::name eq this.name)
            ).firstOrNull()

            if (competition == null) {
                println("Could not find specified template.")
                return@transactional
            }

            /* Fetch all videos in the competition. */
            val videos = competition.getAllVideos().toList()
            val await = videos.mapNotNull { source ->

                when (source) {
                    is DbEvaluationTemplate.VideoSource.ItemSource -> {
                        val item = source.item
                        val path = item.pathToOriginal()
                        if (!Files.exists(path)) {
                            println("ERROR: Media file $path not found for item ${item.name}.")
                            return@mapNotNull null
                        }

                        println("Rendering ${item.name}$ at ${source.range}")
                        this@EvaluationTemplateCommand.cache.asyncPreviewVideo(
                            item,
                            source.range.start.toMilliseconds(),
                            source.range.end.toMilliseconds()
                        )
                    }

                    is DbEvaluationTemplate.VideoSource.PathSource -> {
                        val path = DRES.EXTERNAL_ROOT.resolve(source.path)
                        if (!Files.exists(path)) {
                            println("ERROR: Media file $path not found for external video.")
                            return@mapNotNull null
                        }

                        println("Rendering ${path.fileName}$ at ${source.range}")
                        this@EvaluationTemplateCommand.cache.asyncPreviewVideo(
                            path,
                            source.range.start.toMilliseconds(),
                            source.range.end.toMilliseconds()
                        )
                    }
                }
            }

            val success = await.all {
                try {
                    it.get(60, TimeUnit.SECONDS)
                    true
                } catch (e: Throwable) {
                    e.printStackTrace()
                    false
                }
            }

            if (success) {
                println("Video preparation completed successfully")
            } else {
                println("An error occurred during video preparation")
            }

        }
    }


    /**
     * Exports a specific competition to a JSON file.
     */
    inner class Export : AbstractEvaluationCommand(name = "export", help = "Exports a template as JSON.") {

        /** Path to the file that should be created .*/
        private val path: Path by option("-o", "--out", help = "The destination file for the template.").path()
            .required()

        /** Flag indicating whether export should be pretty printed.*/
        private val pretty: Boolean by option(
            "-p",
            "--pretty",
            help = "Flag indicating whether exported JSON should be pretty printed."
        ).flag("-u", "--ugly", default = true)

        override fun run() = this@EvaluationTemplateCommand.store.transactional(true) {
            val template = DbEvaluationTemplate.query(
                (DbEvaluationTemplate::id eq this.id).or(DbEvaluationTemplate::name eq this.name)
            ).firstOrNull()

            if (template == null) {
                println("Could not find specified template.")
                return@transactional
            }

            val mapper = jacksonObjectMapper()
            Files.newBufferedWriter(this.path, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE).use {
                val writer = if (this.pretty) {
                    mapper.writerWithDefaultPrettyPrinter()
                } else {
                    mapper.writer()
                }
                writer.writeValue(it, template.toApi())
            }
            println("Successfully wrote template '${template.name}' (ID = ${template.id}) to $path.")
        }
    }


    /**
     * Imports a template from a JSON file.
     */
    inner class Import : CliktCommand(name = "import", help = "Imports a template from JSON.") {

        /** Path to the file that should be imported.*/
        private val path: Path by option("-i", "--in", help = "The file to import the template from.").path().required()

        override fun run() = this@EvaluationTemplateCommand.store.transactional {

            /* Read template from file */
            val reader = jacksonObjectMapper().readerFor(ApiEvaluationTemplate::class.java)
            val template = try {
                Files.newBufferedReader(this.path).use {
                    val tree = reader.readTree(it)
                    reader.readValue<ApiEvaluationTemplate>(tree)
                }
            } catch (e: Throwable) {
                println("Could not import template from $path: ${e.message}.")
                return@transactional
            }

            /* Create/update template. */
            if (template != null) {

                val target = DbEvaluationTemplate.new()

                val import =  //null out ids to force new task creation
                    template.copy(
                        tasks = template.tasks.map {
                            it.copy(id = null)
                        },
                        teams = template.teams.map {
                            it.copy(id = null)
                        }
                    )


                TemplateUtil.updateDbTemplate(target, import)
                println("template imported")

            } else {
                println("Could not import template from $path: Unknown format.")
            }
        }
    }
}