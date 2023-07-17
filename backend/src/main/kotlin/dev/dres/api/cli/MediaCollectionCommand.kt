package dev.dres.api.cli

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.float
import com.github.ajalt.clikt.parameters.types.long
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import com.github.kokorin.jaffree.StreamType
import com.github.kokorin.jaffree.ffprobe.FFprobe
import com.github.kokorin.jaffree.ffprobe.FFprobeResult
import com.jakewharton.picnic.table
import dev.dres.DRES
import dev.dres.api.rest.types.collection.ApiMediaType
import dev.dres.data.model.config.Config
import dev.dres.data.model.media.*
import dev.dres.data.model.media.time.TemporalPoint
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.*
import org.slf4j.LoggerFactory
import org.slf4j.MarkerFactory
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.concurrent.TimeUnit
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.relativeTo
import kotlin.streams.asSequence

/**
 * A collection of [CliktCommand]s for [DbMediaItem], [DbMediaCollection] and [DbMediaSegment] management.
 *
 * @author Luca Rossetto
 * @author Ralph Gasser
 * @version 2.0.0
 */
class MediaCollectionCommand(private val store: TransientEntityStore, private val config: Config) :
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
            "drop" to listOf("delete")
        )
    }

    companion object {
        enum class SortField {
            ID, NAME, LOCATION, DURATION, FPS
        }

        /** used for chunking of sequences to be committed */
        const val transactionChunkSize = 1000

    }

    /**
     *
     */
    abstract inner class AbstractCollectionCommand(name: String, help: String) :
        CliktCommand(name = name, help = help, printHelpOnEmptyArgs = true) {

        /** The [CollectionId] of the [DbMediaCollection] affected by this [AbstractCollectionCommand]. */
        protected val id: CollectionId? by option("-i", "--id", help = "ID of a media collection.")

        /** The name of the [DbMediaCollection] affected by this [AbstractCollectionCommand]. */
        protected val name: String? by option("-c", "--collection", help = "Name of a media collection.")

        /**
         * Loads and returns the [DbMediaCollection] for the specified parameters.
         * This is a convenience method and requires a transaction context.
         *
         * @return [DbMediaCollection] or null
         */
        protected fun getCollection(): DbMediaCollection? =
            DbMediaCollection.query((DbMediaCollection::id eq this.id).or(DbMediaCollection::name eq this.name))
                .firstOrNull()
    }

    /**
     * [CliktCommand] to create a new [DbMediaCollection].
     */
    inner class Create :
        CliktCommand(name = "create", help = "Creates a new media collection.", printHelpOnEmptyArgs = true) {

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
            this@MediaCollectionCommand.store.transactional {
                DbMediaCollection.new {
                    this.name = this@Create.name
                    this.description = this@Create.description
                    this.path = this@Create.basePath
                }
            }
            println("Successfully added new media collection.")
        }
    }

    /**
     * [CliktCommand] to create a new [DbMediaCollection].
     */
    inner class Delete : AbstractCollectionCommand("delete", help = "Deletes a media collection.") {
        override fun run() {
            this@MediaCollectionCommand.store.transactional {
                val collection = this.getCollection()
                if (collection == null) {
                    println("Failed to delete collection; specified collection not found.")
                    return@transactional
                }
                collection.delete()
            }
            println("Collection deleted successfully.")
        }
    }

    /**
     * [CliktCommand] to create a new [DbMediaCollection].
     */
    inner class Update : AbstractCollectionCommand(name = "update", help = "Updates an existing Collection") {

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
            this@MediaCollectionCommand.store.transactional {
                val collection = this.getCollection()
                if (collection == null) {
                    println("Failed to update collection; specified collection not found.")
                    return@transactional
                }

                /* Update collection. */
                collection.name = (this.newName ?: collection.name)
                collection.description = (this.newDescription ?: collection.path)
                collection.path = (this.newPath ?: collection.path)

            }
            println("Successfully updated media collection.")
        }
    }

    /**
     * [CliktCommand] to list all [DbMediaCollection]s.
     */
    inner class List : CliktCommand(name = "list", help = "Lists all media collections.") {
        val plain by option(
            "-p",
            "--plain",
            help = "Plain print: No fancy table presentation for machine readable output"
        ).flag(default = false)

        override fun run() = this@MediaCollectionCommand.store.transactional(true) {
            println("Available media collections ${DbMediaCollection.all().size()}")
            if (this.plain) {
                DbMediaCollection.all().asSequence().forEach { println(it) }
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
                            DbMediaCollection.all().asSequence().forEach { c ->
                                row(c.id, c.name, c.description ?: "", c.path, c.items.size())
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
    inner class Show : AbstractCollectionCommand("show", help = "Lists the content of a media collection.") {

        /** The property of the [DbMediaItem]s to sort by. */
        private val sort by option(
            "-s",
            "--sort",
            help = "Chose which sorting to use"
        ).enum<SortField>(ignoreCase = true).defaultLazy { SortField.NAME }

        private val plain by option("-p", "--plain", help = "Plain formatting. No fancy tables").flag(default = false)

        override fun run() = this@MediaCollectionCommand.store.transactional(true) {
            /* Find media collection. */
            val collection = this.getCollection()
            if (collection == null) {
                println("Collection not found.")
                return@transactional
            }

            /* Query for items.. */
            val query = collection.items.sortedBy(
                when (this.sort) {
                    SortField.ID -> DbMediaItem::id
                    SortField.NAME -> DbMediaItem::name
                    SortField.LOCATION -> DbMediaItem::location
                    SortField.DURATION -> DbMediaItem::durationMs
                    SortField.FPS -> DbMediaItem::fps
                }
            )

            /* Print items. */
            if (this.plain) {
                query.asSequence().forEach { println(it) }
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
                            query.asSequence().forEach {
                                row(
                                    it.id,
                                    it.name,
                                    it.location,
                                    it.type.description,
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
        override fun run() = this@MediaCollectionCommand.store.transactional(true) {
            /* Find media collection. */
            val collection = this.getCollection()
            if (collection == null) {
                println("Collection not found.")
                return@transactional
            }

            /* Check items. */
            var counter = 0
            for (item in collection.items.asSequence()) {
                val path = item.pathToOriginal()
                if (Files.exists(path)) {
                    if (Files.isReadable(path)) {
                        counter++
                    } else {
                        println("Item ${item.name} at $path not readable.")
                    }
                } else {
                    println("Item ${item.name} at $path not found.")
                }
            }
            println("Successfully checked $counter of ${collection.items.size()} media itemss")
        }
    }

    /**
     * [CliktCommand] to validate a [DbMediaCollection]'s [DbMediaItem]s.
     */
    inner class Scan : AbstractCollectionCommand("scan", help = "Scans a collection directory and adds found items") {


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

        private fun analyze(videoPath: Path, countFrames: Boolean = false): FFprobeResult =
            FFprobe.atPath(this@MediaCollectionCommand.config.cache.ffmpegPath())
                .setInput(videoPath)
                .setShowStreams(true)
                .setCountFrames(countFrames)
                .setSelectStreams(StreamType.VIDEO)
                .execute()


        override fun run() {
            /* Sanity cehck. */
            if (imageTypes.isEmpty() && videoTypes.isEmpty()) {
                println("No file types specified.")
                return
            }

            /* Find media collection. */
            val collection = this.getCollection()
            if (collection == null) {
                println("Collection not found.")
                return
            }

            val base = Paths.get(collection.path)
            if (!Files.exists(base)) {
                println("Failed to scan collection; '${collection.path}' does not exist.")
                return
            }

            if (!Files.isReadable(base)) {
                println("Failed to scan collection;  '${collection.path}' is not readable.")
                return
            }

            if (!Files.isDirectory(base)) {
                println("Failed to scan collection; '${collection.path}' is no directory.")
                return
            }

            /* Now scan directory. */
            val issues = mutableMapOf<Path, String>()
            var fileCounter = 0
            Files.walk(base).filter {
                Files.isRegularFile(it) && (it.extension in imageTypes || it.extension in videoTypes)
            }.asSequence().chunked(transactionChunkSize).forEach { list ->

                this@MediaCollectionCommand.store.transactional {

                    list.forEach {
                        val relativePath = it.relativeTo(base)
                        val exists =
                            DbMediaItem.query((DbMediaItem::collection eq collection) and (DbMediaItem::location eq relativePath.toString())).isNotEmpty
                        if (!exists) {
                            try {
                                when (it.extension.lowercase()) {
                                    in this.imageTypes -> {
                                        println("Found image $it; analyzing...")
                                        collection.items.add(DbMediaItem.new {
                                            this.type = DbMediaType.IMAGE
                                            this.name = it.fileName.nameWithoutExtension
                                            this.location = relativePath.toString()
                                        })
                                    }

                                    in videoTypes -> {
                                        println("Found video $it; analyzing...")
                                        val result = this.analyze(it).streams.first()
                                        val fps = (result.rFrameRate ?: result.avgFrameRate!!).toFloat()
                                        val duration = result.getDuration(TimeUnit.MILLISECONDS).let { duration ->
                                            if (duration != null) {
                                                duration
                                            } else {
                                                println("Cannot read duration from file, counting frames")
                                                val analysis = this.analyze(it, countFrames = true)
                                                val frames = analysis.streams.first().nbReadFrames
                                                println("Counted $frames frames")
                                                ((frames * 1000) / fps).toLong()
                                            }
                                        }

                                        println("Found frame rate to be $fps frames per seconds and duration $duration ms")
                                        collection.items.add(DbMediaItem.new {
                                            this.type = DbMediaType.VIDEO
                                            this.name = it.fileName.nameWithoutExtension
                                            this.location = relativePath.toString()
                                            this.durationMs = duration
                                            this.fps = fps
                                        })
                                    }
                                }
                            } catch (e: Throwable) {
                                this@MediaCollectionCommand.logger.error(
                                    this@MediaCollectionCommand.logMarker,
                                    "An error occurred with $it. Noting and skipping..."
                                )
                                println("An error occurred with $it. Noting and skipping...")
                                issues[it] = e.stackTraceToString()
                            }
                        }
                        ++fileCounter

                    }
                }

            }
            if (issues.isNotEmpty()) {
                val file = File("issues-scan-${collection.name}-${System.currentTimeMillis()}.json")
                println("There have been ${issues.size} issues while scanning. You might want to check them at ${file.path}")
                val om = jacksonObjectMapper()
                om.writeValue(file, issues)
                println("done")
            }
            println("\nAdded $fileCounter elements to collection")
        }
    }

    /**
     * [CliktCommand] to delete [DbMediaItem]s.
     */
    inner class DeleteItem : AbstractCollectionCommand("deleteItem", help = "Deletes media item(s).") {
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


        override fun run() = this@MediaCollectionCommand.store.transactional {
            /* Sanity check. */
            if (itemName == null && itemId == null && itemNameRegex == null) {
                println("Item(s) not specified.")
                return@transactional
            }

            /* Find media collection. */
            val collection = this.getCollection()
            if (collection == null) {
                println("Collection not found.")
                return@transactional
            }

            var counter = 0
            if (this.itemId != null || this.itemName != null) {
                collection.items.filter { (it.id eq itemId).or(it.name eq itemName) }.asSequence().forEach {
                    it.delete()
                    counter += 1
                }
            } else if (this.itemNameRegex != null) {
                collection.items.asSequence().forEach {
                    if (this.itemNameRegex!!.matches(it.name)) {
                        it.delete()
                        counter += 1
                    }
                }
            }
            println("$counter media items deleted successfully.")
        }
    }

    /**
     * [CliktCommand] to delete [DbMediaItem]s.
     */
    inner class AddItem : AbstractCollectionCommand(name = "add", help = "Adds a media item to a media collection.") {

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
        private val duration: Long? by option("-d", "--duration", help = "video duration in seconds").long()

        /** The fps rate of the new [DbMediaItem]. */
        private val fps: Float? by option("-f", "--fps").float().required()

        override fun run() = this@MediaCollectionCommand.store.transactional {
            /* Find media collection. */
            val collection = this.getCollection()
            if (collection == null) {
                println("Collection not found.")
                return@transactional
            }

            /* Check if file exists. */
            val fullPath = Paths.get(collection.path, this@AddItem.path)
            if (!Files.exists(fullPath)) {
                println("Warning: Media item $fullPath doesn't seem to exist. Continuing anyway...")
            }

            /* Add new media item. */
            collection.items.add(DbMediaItem.new {
                this.type = this@AddItem.type.toDb()
                this.name = Paths.get(this@AddItem.path).nameWithoutExtension
                this.location = this@AddItem.path
                this.durationMs = this@AddItem.duration
                this.fps = this@AddItem.fps
            })

            println("Media item added successfully.")
        }
    }

    /**
     * [CliktCommand] to export a [DbMediaCollection].
     */
    inner class Export : AbstractCollectionCommand("export", help = "Exports a media collection into a CSV file.") {

        /** The output path for the export.. */
        private val output: Path by option(
            "-o",
            "--output",
            help = "Path of the file the media collection should to be exported to."
        ).convert { Paths.get(it) }.required()

        /** The header of an exported CSV file. */
        private val header = listOf("itemType", "name", "location", "duration", "fps")

        override fun run() = this@MediaCollectionCommand.store.transactional(true) {
            /* Find media collection. */
            val collection = this.getCollection()
            if (collection == null) {
                println("Collection not found.")
                return@transactional
            }

            Files.newOutputStream(this.output, StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW).use { os ->
                csvWriter().open(os) {
                    writeRow(this@Export.header)
                    collection.items.asSequence().forEach {
                        writeRow(
                            listOf(
                                it.type.description,
                                it.name,
                                it.location,
                                it.durationMs?.toString(),
                                it.fps?.toString()
                            )
                        )
                    }
                }
            }
        }
    }

    /**
     * [CliktCommand] to import a [DbMediaCollection].
     */
    inner class Import : AbstractCollectionCommand("import", help = "Imports a media collection from a CSV file.") {

        /** [Path] to the input file. */
        private val input: Path by option(
            "--input",
            help = "Path of the file the media collection should be imported from."
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
            val collection = this.getCollection()
            if (collection == null) {
                println("Collection not found.")
                return
            }
            /* Load file. */
            Files.newInputStream(this.input, StandardOpenOption.READ).use { ips ->
                val chunks = csvReader().readAllWithHeader(ips).chunked(transactionChunkSize)

                chunks.forEach { rows ->
                    this@MediaCollectionCommand.store.transactional {
                        for (row in rows) {
                            collection.items.add(DbMediaItem.new {
                                this.type = ApiMediaType.valueOf(row.getValue("itemType").uppercase()).toDb()
                                this.name = row.getValue("name")
                                this.location = row.getValue("location")
                                this.durationMs = row["duration"]?.toLongOrNull()
                                this.fps = row["fps"]?.toFloatOrNull()
                            })
                            ++inserted
                        }
                    }
                }
            }
            println("Successfully imported $inserted media items.")
        }
    }


    data class ListSegment(val video: String, val name: String, val start: Int, val end: Int)

    /**
     * [CliktCommand] to import a [DbMediaSegment]s.
     *
     * Uses the VBS format.
     */
    inner class ImportSegments : AbstractCollectionCommand(
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
            val collection = this.getCollection()
            if (collection == null) {
                println("Collection not found.")
                return
            }
            /* Load file. */
            Files.newInputStream(this.input, StandardOpenOption.READ).use { ips ->
                print("Reading input file...")
                val rows: kotlin.collections.List<Map<String, String>> = csvReader().readAllWithHeader(ips)
                println("Done! Reading ${rows.size} rows")

                rows.mapNotNull {
                    val segmentName = it["name"] ?: return@mapNotNull null
                    val videoName = it["video"] ?: return@mapNotNull null
                    val start = it["start"]?.toIntOrNull() ?: return@mapNotNull null
                    val end = it["end"]?.toIntOrNull() ?: return@mapNotNull null
                    ListSegment(videoName, segmentName, start, end)
                }
                    .groupBy(ListSegment::video)
                    .asSequence().chunked(transactionChunkSize)
                    .forEach { chunk ->
                        this@MediaCollectionCommand.store.transactional {
                            chunk.forEach { (videoName, segments) ->
                                val videoItem = collection.items.filter { it.name eq videoName }.firstOrNull()
                                videoItem?.segments?.addAll(
                                    segments.map {
                                        DbMediaSegment.new {
                                            this.name = it.name
                                            this.start = it.start
                                            this.end = it.end
                                        }
                                    }
                                )
                                ++inserted
                            }
                        }
                    }
            }

            println("Done! Inserted $inserted valid segments.")
        }
    }
}
