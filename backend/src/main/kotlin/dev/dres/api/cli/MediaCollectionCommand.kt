package dev.dres.api.cli

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.groups.mutuallyExclusiveOptions
import com.github.ajalt.clikt.parameters.groups.required
import com.github.ajalt.clikt.parameters.groups.single
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.float
import com.github.ajalt.clikt.parameters.types.long
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import com.jakewharton.picnic.table
import dev.dres.api.rest.types.collection.*
import dev.dres.data.model.media.*
import dev.dres.mgmt.MediaCollectionMaintenance
import dev.dres.mgmt.MediaCollectionManager
import dev.dres.utilities.extensions.cleanPathString
import kotlinx.dnq.query.*
import org.slf4j.LoggerFactory
import org.slf4j.MarkerFactory
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.relativeTo
import kotlin.streams.asSequence

/**
 * A collection of [CliktCommand]s for [DbMediaItem], [DbMediaCollection] and [DbMediaSegment] management.
 *
 * @author Luca Rossetto
 * @author Ralph Gasser
 * @author Loris Sauter
 * @version 2.0.0
 */
class MediaCollectionCommand :
    NoOpCliktCommand(name = "collection") {
    private val logMarker = MarkerFactory.getMarker("CLI")
    private val logger = LoggerFactory.getLogger(this.javaClass)


    init {
        this.subcommands(
            Create(),
            Delete(),
            Update(),
            List(),
            Show(),
            Check(),
            Scan(),
            AddItem(),
            DeleteItem(),
            Export(),
            Import(),
            ImportSegments()
        )
    }

    override fun aliases(): Map<String, kotlin.collections.List<String>> {
        return mapOf(
            "ls" to listOf("list"),
            "remove" to listOf("delete"),
            "rm" to listOf("delete"),
            "drop" to listOf("delete")
        )
    }

    companion object {
        enum class SortField {
            ID, NAME, LOCATION
        }

        /** used for chunking of sequences to be committed */
        const val transactionChunkSize = 1000

    }

    /**
     * Wrapper to both handle [CollectionId] and collection name as a [String].
     */
    sealed class CollectionAddress {
        data class IdAddress(val id: CollectionId): CollectionAddress(){
            override fun toString(): String {
                return "CollectionAddress(id=$id)"
            }
        }
        data class NameAddress(val name: String): CollectionAddress(){
            override fun toString(): String {
                return "CollectionAddress(name=$name)"
            }
        }
    }

    /**
     * Base command for media collection related commands.
     * Provides infrastructure to get the collection specified by the user
     */
    abstract class AbstractCollectionCommand(
        name: String,
        help: String
    ) :
        CliktCommand(name = name) {

        private val helpText: String = help

        override fun help(context: com.github.ajalt.clikt.core.Context): String = helpText
        override val printHelpOnEmptyArgs: Boolean = true

        /** The [CollectionAddress] of the [DbMediaCollection] affected by this [AbstractCollectionCommand]. */
        protected val collectionAddress: CollectionAddress by mutuallyExclusiveOptions<CollectionAddress>(
            option(
                "-i",
                "--id",
                help = "ID of a media collection."
            ).convert { CollectionAddress.IdAddress(it) },
            option(
                "-c",
                "--collection",
                help = "The collection name"
            ).convert { CollectionAddress.NameAddress(it) }
        ).single().required()

        /**
         * Resolves the [ApiMediaCollection] which is addressed by the given [CollectionAddress]
         */
        protected fun resolve(addr: CollectionAddress): ApiMediaCollection {
            val col = when (addr) {
                is CollectionAddress.IdAddress -> {
                    MediaCollectionManager.getCollection(addr.id)
                }

                is CollectionAddress.NameAddress -> {
                    MediaCollectionManager.getCollectionByName(addr.name)
                }
            }
            if (col != null) {
                return col
            } else {
                throw IllegalArgumentException("Could not find media collection for address $addr")
            }
        }

        protected fun resolvePopulated(addr: CollectionAddress): ApiPopulatedMediaCollection {
            return MediaCollectionManager.getPopulatedCollection(resolve(addr).id!!)
                ?: throw IllegalArgumentException("Could not find media collection for address $addr")
        }

    }

    /**
     * [CliktCommand] to create a new [DbMediaCollection].
     */
    inner class Create :
        CliktCommand(name = "create") {
            override fun help(context: com.github.ajalt.clikt.core.Context): String = "Creates a new media collection."
            override val printHelpOnEmptyArgs: Boolean = true

        /** The name of the new [DbMediaCollection]. */
        private val name: String by option("-n", "--name", help = "Name of the Collection to be created").required()

        /** A description of the new [DbMediaCollection]. */
        private val description: String by option(
            "-d",
            "--description",
            help = "Description of the Collection to be created"
        ).default("")

        /** The base path to the new [DbMediaCollection]. */
        private val basePath: String by option(
            "-p",
            "--path",
            help = "Base path of the Collection all contained Items will be specified relative to"
        ).required()

        override fun run() {
            if (!Files.exists(Paths.get(this.basePath))) {
                this@MediaCollectionCommand.logger.warn("Collection base path ${this.basePath} does not exist!")
            }

            try {
                MediaCollectionManager.createCollection(name, description, basePath)
                println("Successfully added new media collection.")
            } catch (e: IllegalArgumentException) {
                println("Could not create collection: ${e.message}")
            }

        }
    }

    /**
     * [CliktCommand] to create a new [DbMediaCollection].
     */
    inner class Delete :
        AbstractCollectionCommand("delete", help = "Deletes a media collection.") {
        override fun run() {
            try{
                MediaCollectionManager.deleteCollection(resolve(collectionAddress).id!!)
                    ?: throw IllegalArgumentException("Could not find media collection for address $collectionAddress")
                println("Collection deleted successfully.")
            }catch(ex: IllegalArgumentException){
                println(ex.message)
            }
        }
    }

    /**
     * [CliktCommand] to create a new [DbMediaCollection].
     */
    inner class Update :
        AbstractCollectionCommand(name = "update", help = "Updates an existing Collection") {

        /** The new name for the [DbMediaCollection]. */
        private val newName: String? by option("-n", "--name", help = "The new name of the collection")

        /** The new description for the [DbMediaCollection]. */
        private val newDescription: String? by option(
            "-d",
            "--description",
            help = "Description of the Collection to be created"
        )

        /** The new path for the [DbMediaCollection]. */
        private val newPath: String? by option(
            "-p",
            "--path",
            help = "Base path of the Collection all contained Items will be specified relative to"
        )

        override fun run() {
            val collection = try {
                resolve(collectionAddress)
            }catch(ex: IllegalArgumentException){
                println(ex.message)
                return
            }

            val updated = collection.copy(
                name = (this.newName ?: collection.name),
                description = (this.newDescription ?: collection.description),
                basePath = (this.newPath?.cleanPathString() ?: collection.basePath)
            )

            MediaCollectionManager.updateCollection(updated)

            println("Successfully updated media collection.")
        }
    }

    /**
     * [CliktCommand] to list all [DbMediaCollection]s.
     */
    inner class List : CliktCommand(name = "list") {
        override fun help(context: com.github.ajalt.clikt.core.Context): String = "Lists all media collections."
        val plain by option(
            "-p",
            "--plain",
            help = "Plain print: No fancy table presentation for machine readable output"
        ).flag(default = false)

        override fun run() {

            val collections = MediaCollectionManager.getCollections()

            println("Available media collections ${collections.size}")
            if (this.plain) {
                collections.forEach(::println)
            } else {
                println(
                    table {
                        cellStyle {
                            border = true
                            paddingLeft = 1
                            paddingRight = 1
                        }
                        header {
                            row("id", "name", "description", "basePath", "# items")
                        }
                        body {
                            MediaCollectionManager.getCollections().forEach { c ->
                                row(c.id, c.name, c.description ?: "", c.basePath, c.itemCount)
                            }
                        }
                    }
                )
            }
        }
    }

    /**
     * [CliktCommand] to show a [DbMediaCollection]'s [DbMediaItem]s in detail.
     */
    inner class Show :
        AbstractCollectionCommand("show", help = "Lists the content of a media collection.") {

        /** The property of the [DbMediaItem]s to sort by. */
        private val sort by option(
            "-s",
            "--sort",
            help = "Chose which sorting to use"
        ).enum<SortField>(ignoreCase = true).defaultLazy { SortField.NAME }

        private val plain by option("-p", "--plain", help = "Plain formatting. No fancy tables").flag(default = false)

        override fun run() {
            /* Find media collection. */
            val collection = try{
                resolvePopulated(collectionAddress)
            }catch(ex: IllegalArgumentException){
                println(ex.message)
                return
            }

            val items = collection.items.sortedBy {
                when (sort) {
                    SortField.ID -> it.mediaItemId
                    SortField.NAME -> it.name
                    SortField.LOCATION -> it.location
                }
            }

            /* Print items. */
            if (this.plain) {
                items.forEach(::println)
            } else {
                println(
                    table {
                        cellStyle {
                            border = true
                            paddingLeft = 1
                            paddingRight = 1
                        }
                        header {
                            row("id", "name", "location", "type", "durationMs", "fps")
                        }
                        body {
                            items.forEach {
                                row(
                                    it.mediaItemId,
                                    it.name,
                                    it.location,
                                    it.type,
                                    it.durationMs ?: "n/a",
                                    it.fps ?: "n/a"
                                )
                            }
                        }
                    }
                )
            }
        }
    }

    /**
     * [CliktCommand] to validate a [DbMediaCollection]'s [DbMediaItem]s.
     */
    inner class Check : AbstractCollectionCommand(
        "check",
        help = "Checks if all the files in a media collection are present and accessible."
    ) {
        override fun run() {
            val collection = try {
                resolve(collectionAddress)
            } catch (ex: IllegalArgumentException) {
                println(ex.message)
                return
            }
            val result = MediaCollectionMaintenance.check(collection.id!!) ?: return
            result.issues.forEach { println("Item ${it.name}: ${it.problem}") }
            if (result.issueCount > result.issues.size) println("... and ${result.issueCount - result.issues.size} more.")
            println("Successfully checked ${result.ok} of ${result.total} media items")
        }
    }

    /**
     * [CliktCommand] to validate a [DbMediaCollection]'s [DbMediaItem]s.
     */
    inner class Scan : AbstractCollectionCommand(
        "scan",
        help = "Scans a collection directory and adds found items"
    ) {


        /** The file suffices that should be considered as images. */
        private val imageTypes by option(
            "-it",
            "--imageType",
            help = "Image file types (endings) to be considered in the scan"
        ).convert { it.lowercase() }.multiple()
        /** The file suffices that should be considered as images. */

        /** The file suffices that should be considered as videos. */
        private val videoTypes by option(
            "-vt",
            "--videoType",
            help = "Video file types (endings) to be considered in the scan"
        ).convert { it.lowercase() }.multiple()



        override fun run() {
            val collection = try {
                resolve(collectionAddress)
            } catch (ex: IllegalArgumentException) {
                println(ex.message)
                return
            }
            val result = try {
                MediaCollectionMaintenance.scan(collection.id!!, this.videoTypes, this.imageTypes) { println(it) }
            } catch (e: IllegalArgumentException) {
                println("Failed to scan collection: ${e.message}")
                return
            }
            if (result.issueCount > 0) {
                val file = File("issues-scan-${collection.name}-${System.currentTimeMillis()}.json")
                jacksonObjectMapper().writeValue(file, result.issues)
                println("There have been ${result.issueCount} issues while scanning. You might want to check them at ${file.path}")
            }
            println("\n${result.message}")
        }

    }

    /**
     * [CliktCommand] to delete [DbMediaItem]s.
     */
    inner class DeleteItem :
        AbstractCollectionCommand("deleteItem", help = "Deletes media item(s).") {
        /** The item ID matching the name of the [DbMediaItem] to delete. */
        private val itemId: MediaId? by option("-ii", "--itemId", help = "ID of the media item.")

        /** The name of the [DbMediaItem] to delete. */
        private val itemName: String? by option("-in", "--itemName", help = "The exact name of the media item.")

        /** A RegEx matching the name of the [DbMediaItem] to delete. */
        private val itemNameRegex: Regex? by option(
            "-e",
            "--regex",
            help = "Regex for item names"
        ).convert { it.toRegex() }


        override fun run() {
            /* Sanity check. */
            if (itemName == null && itemId == null && itemNameRegex == null) {
                println("Item(s) not specified.")
                return
            }

            /* Find media collection. */
            val collection = try {
                resolve(collectionAddress)
            } catch (ex: IllegalArgumentException) {
                println(ex.message)
                return
            }

            val result = MediaCollectionMaintenance.deleteItems(collection.id!!) { id, name ->
                if (this.itemId != null || this.itemName != null) {
                    id == this.itemId || name == this.itemName
                } else {
                    this.itemNameRegex!!.matches(name)
                }
            } ?: return
            result.blocked.forEach { println("Kept ${it.location}: ${it.problem}") }
            if (result.blockedCount > result.blocked.size) println("... and ${result.blockedCount - result.blocked.size} more.")
            println("${result.deleted} media items deleted successfully" +
                    if (result.blockedCount > 0) ", ${result.blockedCount} kept because they are still in use." else ".")
        }
    }

    /**
     * [CliktCommand] to delete [DbMediaItem]s.
     */
    inner class AddItem :
        AbstractCollectionCommand(name = "add", help = "Adds a media item to a media collection.") {

        /** The [ApiMediaType] of the new [DbMediaItem]. */
        private val type: ApiMediaType by option(
            "-t",
            "--type",
            help = "Type of the new media item."
        ).enum<ApiMediaType>().required()

        /** The relative path of the new [DbMediaItem]. */
        private val path: String by option(
            "-p",
            "--path",
            help = "Path of the new media item. relative to the collection base path"
        ).required()

        /** The duration of the new [DbMediaItem]. */
        private val duration: Long? by option("-d", "--duration", help = "video duration in milliseconds").long()

        /** The fps rate of the new [DbMediaItem]. */
        private val fps: Float? by option("-f", "--fps", help = "video frame rate (required for videos)").float()

        override fun run() {
            /* Find media collection. */
            val collection = try{
                resolve(collectionAddress)
            }catch(ex: IllegalArgumentException){
                println(ex.message)
                return
            }

            /* Check if file exists. */
            val fullPath = Paths.get(collection.basePath!!, this@AddItem.path)
            if (!Files.exists(fullPath)) {
                println("Warning: Media item $fullPath doesn't seem to exist. Continuing anyway...")
            }

            /* Add new media item. */
            val item = ApiMediaItem(
                type = this@AddItem.type,
                name = Paths.get(this@AddItem.path).nameWithoutExtension,
                location = this@AddItem.path,
                durationMs = this@AddItem.duration,
                fps = this@AddItem.fps,
                collectionId = collection.id!!,
                mediaItemId = ""
            )

            MediaCollectionManager.addMediaItem(item)

            println("Media item added successfully.")
        }
    }

    /**
     * [CliktCommand] to export a [DbMediaCollection].
     */
    inner class Export :
        AbstractCollectionCommand("export", help = "Exports a media collection into a CSV file.") {

        /** The output path for the export. */
        private val output: Path by option(
            "-o",
            "--output",
            help = "Path of the file the media collection should to be exported to."
        ).convert { Paths.get(it) }.required()


        override fun run() {
            val collection = try {
                resolve(collectionAddress)
            } catch (ex: IllegalArgumentException) {
                println(ex.message)
                return
            }
            val csv = MediaCollectionMaintenance.exportCsv(collection.id!!) ?: return
            Files.writeString(this.output, csv, StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW)
            println("Exported collection ${collection.name} to ${this.output}.")
        }
    }

    /**
     * [CliktCommand] to import a [DbMediaCollection].
     */
    inner class Import :
        AbstractCollectionCommand("import", help = "Imports a media collection from a CSV file.") {

        /** [Path] to the input file. */
        private val input: Path by option(
            "--input",
            help = "Path of the file the media collection should be imported from."
        )
            .convert { Paths.get(it) }.required()

        override fun run() {
            var attempted = 0
            var inserted = 0

            /* Check for input file's existence. */
            if (!Files.exists(this.input)) {
                println("Input file not found.")
                return
            }

            /* Find media collection. */
            val collection = try{
                resolve(collectionAddress)
            }catch(ex: IllegalArgumentException){
                println(ex.message)
                return
            }

            /* Load file. */
            Files.newInputStream(this.input, StandardOpenOption.READ).use { ips ->
                val chunks = csvReader().readAllWithHeader(ips).chunked(transactionChunkSize)

                chunks.forEach { rows ->

                    val items = rows.map { row ->
                        attempted++
                        ApiMediaItem(
                            type = ApiMediaType.valueOf(row.getValue("itemType").uppercase()),
                            name = row.getValue("name"),
                            location = row.getValue("location"),
                            durationMs = row["duration"]?.toLongOrNull(),
                            fps = row["fps"]?.toFloatOrNull(),
                            collectionId = collection.id!!,
                            mediaItemId = ""
                        )
                    }

                    inserted += MediaCollectionManager.addMediaItems(collection.id!!, items)

                }
            }
            println("Successfully imported $inserted of $attempted media items.")
        }
    }


    /**
     * [CliktCommand] to import a [DbMediaSegment]s.
     *
     * Uses the VBS format.
     */
    inner class ImportSegments() : AbstractCollectionCommand(

        "importSegments",
        "Imports the Segment information for the Items in a Collection from a CSV file"
    ) {


        /** [Path] to the input file. */
        private val input: Path by option(
            "--input",
            help = "Path of the file the media segments should be imported from."
        )
            .convert { Paths.get(it) }.required()

        override fun run() {
            var inserted = 0

            /* Check for input file's existence. */
            if (!Files.exists(this.input)) {
                println("Input file not found.")
                return
            }


            /* Find media collection. */
            val collection = try{
                resolve(collectionAddress)
            }catch(ex: IllegalArgumentException){
                println(ex.message)
                return
            }
            /* Load file. */
            Files.newInputStream(this.input, StandardOpenOption.READ).use { ips ->
                print("Reading input file...")
                val rows: kotlin.collections.List<Map<String, String>> = csvReader().readAllWithHeader(ips)
                println("Done! Reading ${rows.size} rows")

                rows.mapIndexedNotNull { index, row ->
                    val segmentName = row["name"] ?: return@mapIndexedNotNull null
                    val videoName = row["video"] ?: return@mapIndexedNotNull null
                    val start = row["start"]?.toIntOrNull() ?: return@mapIndexedNotNull null
                    val end = row["end"]?.toIntOrNull() ?: return@mapIndexedNotNull null
                    index to ApiMediaSegment(videoName, segmentName, start, end)
                }
                    .sortedBy { it.second.mediaItemName }
                    .asSequence().chunked(transactionChunkSize)
                    .forEach { chunk ->
                        try {
                            inserted += MediaCollectionManager.addSegments(collection.id!!, chunk.map { it.second })
                        }catch(ex: RuntimeException){
                            val errorMsg = "An error (${ex.javaClass.simpleName}) occurred during ingesting from rows ${chunk.first().first} to ${chunk.last().first}"
                            System.err.println("$errorMsg: ${ex.message}")
                            this@MediaCollectionCommand.logger.error(
                                this@MediaCollectionCommand.logMarker,
                                errorMsg, ex
                            )
                        }
                    }
            }

            println("Done! Inserted $inserted valid segments.")
        }
    }
}
