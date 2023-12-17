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
import dev.dres.api.rest.types.template.ApiEvaluationTemplate
import dev.dres.data.model.template.DbEvaluationTemplate
import dev.dres.mgmt.cache.CacheManager
import dev.dres.mgmt.TemplateManager
import java.lang.Exception
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

/**
 * A collection of [CliktCommand]s for [DbEvaluationTemplate] management.
 *
 * @author Luca Rossetto
 * @author Ralph Gasser
 * @version 2.0.0
 */
class EvaluationTemplateCommand(private val cache: CacheManager) :
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
            Import(),
            Rename()
        )
    }

    override fun aliases(): Map<String, kotlin.collections.List<String>> {
        return mapOf(
            "ls" to listOf("list"),
            "remove" to listOf("delete"),
            "rm" to listOf("delete"),
            "drop" to listOf("delete"),
            "add" to listOf("create"),
            "cp" to listOf("copy"),
            "clone" to listOf("copy")
        )
    }

    /**
     * [CliktCommand] to create a new [DbEvaluationTemplate].
     */
    inner class Create : CliktCommand(
        name = "create",
        help = "Creates a new Template",
        printHelpOnEmptyArgs = true
    ) {

        private val name: String by option("-t", "--template", help = "Name of the new Template")
            .required()
            .validate { require(it.isNotEmpty()) { "Template description must be non empty." } }

        private val description: String by option("-d", "--description", help = "Description of the new Template")
            .required()
            .validate { require(it.isNotEmpty()) { "Template description must be non empty." } }

        override fun run() {
            val newTemplate = TemplateManager.createEvaluationTemplate(name, description)
            println("New template '${newTemplate.name}' created with ID = ${newTemplate.id}.")
        }
    }

    /**
     * [CliktCommand] to delete a [DbEvaluationTemplate].
     */
    inner class Delete : CliktCommand(
        name = "delete",
        help = "Deletes a template",
        printHelpOnEmptyArgs = true
    ) {

        private val id: String by option("-i", "--id").required()
            .validate { require(it.isNotEmpty()) { "Id must be non empty." } }

        override fun run() {

            if (TemplateManager.deleteTemplate(id) == null) {
                println("Could not find template to delete.")
            } else {
                println("Successfully deleted template.")
            }

        }
    }

    /**
     * [CliktCommand] to copy a [DbEvaluationTemplate].
     */
    inner class Copy : CliktCommand(
        name = "copy",
        help = "Copies a Template",
        printHelpOnEmptyArgs = true
    ) {

        private val id: String by option("-i", "--id").required()
            .validate { require(it.isNotEmpty()) { "Id must be non empty." } }

        override fun run() {

            try {
                val copy = TemplateManager.copyTemplate(id)
                println("Successfully copied template. New id=${copy.id}")
            } catch (e: IllegalArgumentException) {
                println(e.message)
            }

        }
    }

    /**
     * [CliktCommand] to rename a [DbEvaluationTemplate].
     */
    inner class Rename : CliktCommand(
        name = "rename",
        help = "Renames a Template",
        printHelpOnEmptyArgs = true
    ) {

        private val id: String by option("-i", "--id").required()
            .validate { require(it.isNotEmpty()) { "Id must be non empty." } }

        private val newName: String by option("-n", "--name", help = "New name of the Template")
            .required()
            .validate { require(it.isNotEmpty()) { "Template name must be non empty." } }

        override fun run() {

            val currentTemplate = TemplateManager.getTemplate(id)

            if (currentTemplate == null) {
                println("Template with id '$id' not found")
                return
            }

            val updated = currentTemplate.copy(name = newName)

            TemplateManager.updateTemplate(updated)
            println("Template renamed")
        }
    }


    /**
     * [CliktCommand] to list all [DbEvaluationTemplate]s.
     */
    inner class List : CliktCommand(name = "list", help = "Lists an overview of all Templates") {
        override fun run() {
            var no = 0
            println()
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
                    TemplateManager.getTemplateOverview().forEach { c ->
                        row(c.name, c.id, c.teamCount, c.taskCount, c.description).also { no++ }
                    }
                }
            })
            println("Listed $no templates")
        }
    }

    /**
     * [CliktCommand] to show a specific [DbEvaluationTemplate].
     */
    inner class Show : CliktCommand(name = "show", help = "Shows details of a Template") {

        private val id: String by option("-i", "--id").required()
            .validate { require(it.isNotEmpty()) { "Id must be non empty." } }

        override fun run() {
            val template = TemplateManager.getTemplate(id)

            if (template == null) {
                println("Template with id '$id' not found")
                return
            }

            println("${template.name}: ${template.description}")
            println("Teams:")

            template.teams.forEach(::println)

            println()
            println("Tasks:")

            template.tasks.forEach(::println)
            println()
        }

    }

    /**
     * [CliktCommand] to prepare a specific [DbEvaluationTemplate].
     */
    inner class Prepare : CliktCommand(
        name = "prepare",
        help = "Checks the used media items and generates precomputed previews.",
        printHelpOnEmptyArgs = true
    ) {

        private val id: String by option("-i", "--id").required()
            .validate { require(it.isNotEmpty()) { "Id must be non empty." } }

        override fun run() {

            try {
                TemplateManager.prepareTemplate(id, this@EvaluationTemplateCommand.cache)
                println("Video preparation completed successfully")
            } catch (e: Exception) {
                println(e.message)
            }

        }
    }


    /**
     * Exports a specific evaluation to a JSON file.
     */
    inner class Export : CliktCommand(
        name = "export",
        help = "Exports a template as JSON.",
        printHelpOnEmptyArgs = true
    ) {

        /** Path to the file that should be created .*/
        private val path: Path by option("-o", "--out", help = "The destination file for the template.").path()
            .required()

        /** Flag indicating whether export should be pretty printed.*/
        private val pretty: Boolean by option(
            "-p",
            "--pretty",
            help = "Flag indicating whether exported JSON should be pretty printed."
        ).flag("-u", "--ugly", default = true)

        private val id: String by option("-i", "--id").required()
            .validate { require(it.isNotEmpty()) { "Id must be non empty." } }

        override fun run() {

            val template = TemplateManager.getTemplate(id)

            if (template == null) {
                println("Template with id '$id' not found")
                return
            }

            val mapper = jacksonObjectMapper()
            Files.newBufferedWriter(this.path, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE).use {
                val writer = if (this.pretty) {
                    mapper.writerWithDefaultPrettyPrinter()
                } else {
                    mapper.writer()
                }
                writer.writeValue(it, template)
            }
            println("Successfully wrote template '${template.name}' (ID = ${template.id}) to $path.")
        }
    }


    /**
     * Imports a template from a JSON file.
     */
    inner class Import : CliktCommand(
        name = "import",
        help = "Imports a template from JSON.",
        printHelpOnEmptyArgs = true
    ) {

        /** Path to the file that should be imported.*/
        private val path: Path by option("-i", "--in", help = "The file to import the template from.").path().required()

        override fun run() {

            /* Read template from file */
            val reader = jacksonObjectMapper().readerFor(ApiEvaluationTemplate::class.java)
            val template = try {
                Files.newBufferedReader(this.path).use {
                    val tree = reader.readTree(it)
                    reader.readValue<ApiEvaluationTemplate>(tree)
                }
            } catch (e: Throwable) {
                println("Could not import template from $path: ${e.message}.")
                return
            }

            if (template == null) {
                println("Could not import template from $path: Unknown format.")
                return
            }

            val created = TemplateManager.createEvaluationTemplate(template.name, template.description ?: "")

            val import = template.copy(id = created.id)

            TemplateManager.updateTemplate(import)
            println("template imported")


        }
    }
}
