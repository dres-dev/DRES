package dres.api.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.float
import com.github.ajalt.clikt.parameters.types.long
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import dres.data.dbo.DAO
import dres.data.model.basics.media.MediaCollection
import dres.data.model.basics.media.MediaItem
import dres.data.model.basics.media.MediaItemSegment
import dres.data.model.basics.time.TemporalRange
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class MediaCollectionCommand(val collections: DAO<MediaCollection>, val items: DAO<MediaItem>, val segments: DAO<MediaItemSegment>) :
        NoOpCliktCommand(name = "collection") {

    init {
        this.subcommands(CreateCollectionCommand(), ListCollectionsCommand(), ShowCollectionCommand(), AddMediaItemCommand(), ExportCollectionCommand(), ImportCollectionCommand(), DeleteCollectionCommand(), ImportMediaSegmentsCommand())
    }

    abstract inner class AbstractCollectionCommand(name: String, help: String) : CliktCommand(name = name, help = help) {
        private val collectionNameInput: String? by option("-c", "--collection", help = "Name of the Collection")

        private val collectionIdInput: Long? by option("-i", "--id", help = "Id of the Collection").convert { it.toLong() }

        fun actualCollectionId(): Long? = this.collectionIdInput ?: this.collectionNameInput?.let {
            this@MediaCollectionCommand.collections.find { c -> c.name == it }?.id
        }
    }

    inner class CreateCollectionCommand : CliktCommand(name = "create", help = "Creates a new Collection") {

        private val name: String by option("-n", "--name", help = "Name of the Collection to be created")
                .required()
                .validate { require(!this@MediaCollectionCommand.collections.any { c -> c.name == it }) { "collection with name '$it' already exists" } }

        private val description: String by option("-d", "--description", help = "Description of the Collection to be created")
                .default("")

        private val basePath: String by option("-p", "--path", help = "Base path of the Collection all contained Items will be specified relative to")
                .required()

        override fun run() {
            this@MediaCollectionCommand.collections.append(
                    MediaCollection(name = name, description = description, basePath = basePath)
            )
            println("added collection")
        }
    }

    inner class ListCollectionsCommand : CliktCommand(name = "list", help = "Lists all Collections") {
        override fun run() {
            println("Collections:")
            this@MediaCollectionCommand.collections.forEach {
                println(it)
            }
        }
    }

    inner class ShowCollectionCommand : AbstractCollectionCommand("show", help = "Shows the content of a Collection") {
        override fun run() {

            val collectionId = this.actualCollectionId()
            if (collectionId == null) {
                println("Collection not found.")
                return
            }

            val collectionItems = this@MediaCollectionCommand.items.filter { it.collection == collectionId }

            collectionItems.forEach {
                println(it)
            }

            println("listed ${collectionItems.size} Media Items")



        }
    }

    inner class DeleteCollectionCommand : AbstractCollectionCommand("delete", help = "Deletes a Collection") {
        override fun run() {

            val collectionId = this.actualCollectionId()
            if (collectionId == null) {
                println("Collection not found.")
                return
            }

            /* Delete media items belongig to the collection. */
            val items = this@MediaCollectionCommand.items.filter { it.collection == collectionId }
            var i = 1
            items.forEach {
                this@MediaCollectionCommand.items.delete(it)
                print("Deleting media item ${i++}/${items.size}...\r")
            }

            /* Delete actual collection. */
            this@MediaCollectionCommand.collections.delete(collectionId)

            println("Collection with ID $collectionId and ${items.size} associated media items deleted!")
        }
    }

    inner class AddMediaItemCommand : NoOpCliktCommand(name = "add", help = "Adds a Media Item to a Collection") {

        init {
            this.subcommands(AddImageCommand(), AddVideoCommand())
        }


        inner class AddImageCommand : AbstractCollectionCommand(name = "image", help = "Adds a new Image Media Item") {

            private val name: String by option("-n", "--name", help = "Name of the Item").required()
            private val path: String by option("-p", "--path", help = "Path of the Item relative to the Collection base path")
                    .required()

            override fun run() {

                val collectionId = this.actualCollectionId()
                if (collectionId == null) {
                    println("Collection not found.")
                    return
                }

                val existing = this@MediaCollectionCommand.items.filterIsInstance<MediaItem.ImageItem>().find { it.collection == collectionId && it.name == name }
                if (existing != null) {
                    println("item with name '$name' already exists in collection:")
                    println(existing)
                    return
                }
                this@MediaCollectionCommand.items.append(MediaItem.ImageItem(name = name, location = path, collection = collectionId, id = -1))
                println("item added")
            }

        }

        inner class AddVideoCommand : AbstractCollectionCommand(name = "video", help = "Adds a new Video Media Item") {

            private val name: String by option("-n", "--name", help = "Name of the Item").required()
            private val path: String by option("-p", "--path", help = "Path of the Item relative to the Collection base path")
                    .required()

            private val duration: Long by option("-d", "--duration", help = "video duration in seconds").long().required()
            private val fps: Float by option("-f", "--fps").float().required()

            override fun run() {
                val collectionId = this.actualCollectionId()
                if (collectionId == null) {
                    println("Collection not found.")
                    return
                }

                val existing = this@MediaCollectionCommand.items.filterIsInstance<MediaItem.VideoItem>().find { it.collection == collectionId && it.name == name }
                if (existing != null) {
                    println("item with name '$name' already exists in collection:")
                    println(existing)
                    return
                }
                this@MediaCollectionCommand.items.append(MediaItem.VideoItem(name = name, location = path, collection = collectionId, durationMs = duration, fps = fps, id = -1))
                println("item added")
            }

        }

    }

    inner class ExportCollectionCommand : AbstractCollectionCommand("export", help = "Exports a Collection to a CSV file") {

        private fun fileOutputStream(file: String): OutputStream = FileOutputStream(file)

        private val outputStream: OutputStream by option("-f", "--file", help = "Path of the file the Collection is to be exported to")
                .convert { fileOutputStream(it) }
                .default(System.out)

        private fun toRow(item: MediaItem): List<String?> = when (item) {
            is MediaItem.ImageItem -> listOf(item.itemType, item.name, item.location, null, null)
            is MediaItem.VideoItem -> listOf(item.itemType, item.name, item.location, item.duration.toString(), item.fps.toString())
        }

        private val header = listOf("itemType", "name", "location", "duration", "fps")

        override fun run() {
            csvWriter().open(outputStream) {
                writeRow(header)
                this@MediaCollectionCommand.items.forEach {
                    writeRow(toRow(it))
                }
            }
        }
    }

    inner class ImportCollectionCommand : AbstractCollectionCommand("import", help = "Imports a Collection from a CSV file") {

        private val inputFile: File by option("-f", "--file", help = "Path of the file the Collection is to be imported from")
                .convert { File(it) }
                .required()
                .validate { require(it.exists()) { "Input File not found" } }

        private fun fromRow(map: Map<String, String>, collectionId: Long): MediaItem? {

            if (!map.containsKey("itemType") || !map.containsKey("name") || !map.containsKey("location")) {
                return null
            }

            return when (map.getValue("itemType")) {
                "image" -> MediaItem.ImageItem(-1, map.getValue("name"), map.getValue("location"), collectionId)
                "video" -> {
                    if (map.containsKey("ms") && map.containsKey("fps")) {
                        return MediaItem.VideoItem(-1, map.getValue("name"), map.getValue("location"), collectionId, map.getValue("ms").toLong(), map.getValue("fps").toFloat())
                    }
                    return null
                }
                else -> null
            }
        }

        override fun run() {
            val collectionId = this.actualCollectionId()
            if (collectionId == null) {
                println("Collection not found.")
                return
            }

            val rows: List<Map<String, String>> = csvReader().readAllWithHeader(inputFile)
            val itemsFromFile = rows.mapNotNull { this.fromRow(it, collectionId) }

            val collectionItems = this@MediaCollectionCommand.items.filter { it.collection == collectionId }.toList()

            val itemsToInsert = itemsFromFile.filter { item ->
                collectionItems.none { it.name == item.name && it.location == item.location && it.itemType == it.itemType }
            }

            this@MediaCollectionCommand.items.batchAppend(itemsToInsert)
            println("Successfully imported ${itemsToInsert.size} of ${rows.size} rows")
        }

    }

    inner class ImportMediaSegmentsCommand : AbstractCollectionCommand("importSegments", "Imports the Segment information for the Items in a Collection from a CSV file") {

        private val inputFile: File by option("-f", "--file", help = "Path of the file the Segments are to be imported from")
                .convert { File(it) }
                .required()
                .validate { require(it.exists()) { "Input File not found" } }

        override fun run() {

            val collectionId = this.actualCollectionId()
            if (collectionId == null) {
                println("Collection not found.")
                return
            }

            print("loading collection information...")
            val itemIds = this@MediaCollectionCommand.items.filter { it.collection == collectionId }.map { it.name to it.id }.toMap()
            val mediaItemIds = itemIds.values.toSet()
            print(".")
            val existingSegments = this@MediaCollectionCommand.segments
                    .filter { mediaItemIds.contains(it.mediaItemId) }.map { it.mediaItemId to it.name }.toMutableSet()
            println("done")

            print("reading input file...")
            val rows: List<Map<String, String>> = csvReader().readAllWithHeader(inputFile)
            println("done, read ${rows.size} rows")

            print("analyzing segments...")
            val segments = rows.map {
                val video = it["video"] ?: return@map null
                val name = it["name"] ?: return@map null
                val start = it["start"]?.toLong() ?: return@map null
                val end = it["end"]?.toLong() ?: return@map null

                val videoId = itemIds[video] ?: return@map null

                //check for duplicates
                val pair = videoId to name
                if (existingSegments.contains(pair)){
                    return@map null
                }
                existingSegments.add(pair)

                MediaItemSegment(-1, videoId, name, TemporalRange(start, end))
            }.filterNotNull()
            println("done, generated ${segments.size} valid, non-duplicate segments")

            print("storing segments...")
            this@MediaCollectionCommand.segments.batchAppend(segments)
            println("done")
        }

    }
}