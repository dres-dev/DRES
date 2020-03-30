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
import dres.data.model.basics.MediaCollection
import dres.data.model.basics.MediaItem
import dres.data.model.basics.MediaItemSegment
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class MediaCollectionCommand(val collections: DAO<MediaCollection>, val items: DAO<MediaItem>, val segments: DAO<MediaItemSegment>) :
        NoOpCliktCommand(name = "collection") {

    init {
        this.subcommands(CreateCollectionCommand(), ListCollectionsCommand(), ShowCollectionCommand(), AddMediaItemCommand(), ExportCollectionCommand(), ImportCollectionCommand(), DeleteCollectionCommand())
    }

    abstract inner class AbstractCollectionCommand(name: String) : CliktCommand(name = name) {
        private val collectionNameInput: String? by option("-c", "--collection")

        private val collectionIdInput: Long? by option("-i", "--id").convert { it.toLong() }

        fun actualCollectionId(): Long? = this.collectionIdInput ?: this.collectionNameInput?.let {
            this@MediaCollectionCommand.collections.find { c -> c.name == it }?.id
        }
    }

    inner class CreateCollectionCommand : CliktCommand(name = "create") {

        private val name: String by option("-n", "--name")
                .required()
                .validate { require(!this@MediaCollectionCommand.collections.any { c -> c.name == it }) { "collection with name '$it' already exists" } }

        private val description: String by option("-d", "--description")
                .default("")

        override fun run() {
            this@MediaCollectionCommand.collections.append(
                    MediaCollection(name = name, description = description)
            )
            println("added collection")
        }
    }

    inner class ListCollectionsCommand : CliktCommand(name = "list") {
        override fun run() {
            println("Collections:")
            this@MediaCollectionCommand.collections.forEach {
                println(it)
            }
        }
    }

    inner class ShowCollectionCommand : AbstractCollectionCommand("show") {
        override fun run() {

            val collectionId = this.actualCollectionId()
            if (collectionId == null) {
                println("Collection not found.")
                return
            }

            println(this@MediaCollectionCommand.items[collectionId])
        }
    }

    inner class DeleteCollectionCommand : AbstractCollectionCommand("delete") {
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

    inner class AddMediaItemCommand : NoOpCliktCommand(name = "add") {

        init {
            this.subcommands(AddImageCommand(), AddVideoCommand())
        }


        inner class AddImageCommand : AbstractCollectionCommand(name = "image") {

            private val name: String by option("-n", "--name").required()
            private val path: String by option("-p", "--path")
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

        inner class AddVideoCommand : AbstractCollectionCommand(name = "video") {

            private val name: String by option("-n", "--name").required()
            private val path: String by option("-p", "--path")
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
                this@MediaCollectionCommand.items.append(MediaItem.VideoItem(name = name, location = path, collection = collectionId, ms = duration, fps = fps, id = -1))
                println("item added")
            }

        }

    }

    inner class ExportCollectionCommand : AbstractCollectionCommand("export") {

        private val outputStream: OutputStream by option("-f", "--file")
                .convert { FileOutputStream(it) as OutputStream }
                .default(System.out)

        private fun toRow(item: MediaItem): List<String?> = when (item) {
            is MediaItem.ImageItem -> listOf(item.itemType, item.name, item.location, null, null)
            is MediaItem.VideoItem -> listOf(item.itemType, item.name, item.location, item.duration().toString(), item.fps.toString())
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

    inner class ImportCollectionCommand : AbstractCollectionCommand("import") {

        private val inputFile: File by option("-f", "--file")
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

            var i = 0
            itemsToInsert.forEach {
                this@MediaCollectionCommand.items.append(it)
                print("Importing media item ${i++}/${itemsToInsert.size}...\r")
            }
            println("Successfully imported ${itemsToInsert.size} of ${rows.size} rows")
        }

    }
}