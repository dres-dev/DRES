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

    abstract inner class AbstractCollectionCommand(private val name: String) : CliktCommand(name = name) {

        protected val collectionId: Long by option("-c", "--collection")
                .convert { this@MediaCollectionCommand.collections.find { c -> c.name == it }?.id ?: -1 }
                .required()
                .validate { require(it > -1) { "Collection not found" } }

    }

    inner class CreateCollectionCommand : CliktCommand(name = "create") {

        private val name: String by option("-n", "--name")
                .required()
                .validate { require(!this@MediaCollectionCommand.collections.any { c -> c.name == it }) { "collection with name '$it' already exists" } }

        private val description: String by option("-d", "--description")
                .default("")

        private val basePath: String by option("-p", "--path")
                .required()

        override fun run() {
            this@MediaCollectionCommand.collections.append(
                    MediaCollection(name = name, description = description, basePath = basePath)
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
            this@MediaCollectionCommand.items.filter { it.collection == collectionId }.forEach {
                println(it)
            }
        }

    }

    inner class AddMediaItemCommand : NoOpCliktCommand(name = "add") {

        init {
            this.subcommands(AddImageCommand(), AddVideoCommans())
        }


        inner class AddImageCommand : AbstractCollectionCommand(name = "image") {

            private val name: String by option("-n", "--name").required()
            private val path: String by option("-p", "--path")
                    .required()

            override fun run() {
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

        inner class AddVideoCommans : AbstractCollectionCommand(name = "video") {

            private val name: String by option("-n", "--name").required()
            private val path: String by option("-p", "--path")
                    .required()

            private val duration: Long by option("-d", "--duration", help = "video duration in seconds").long().required()
            private val fps: Float by option("-f", "--fps").float().required()

            override fun run() {
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

        private fun toRow(item: MediaItem): List<String?> =
                when (item) {
                    is MediaItem.ImageItem -> listOf<String?>(item.itemType, item.name, item.location, null, null)
                    is MediaItem.VideoItem -> listOf<String?>(item.itemType, item.name, item.location, item.duration().toString(), item.fps.toString())
                }

        private val header = listOf<String>("itemType", "name", "location", "duration", "fps")

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

        private fun fromRow(map: Map<String, String>): MediaItem? {

            if (!map.containsKey("itemType") || !map.containsKey("name") || !map.containsKey("location")) {
                return null
            }

            return when (map["itemType"]!!) {
                "image" -> MediaItem.ImageItem(-1, map["name"]!!, map["location"]!!, this.collectionId)
                "video" -> {
                    if (!map.containsKey("ms") || !map.containsKey("fps")) {
                        return null
                    }
                    MediaItem.VideoItem(-1, map["name"]!!, map["location"]!!, this.collectionId, map["ms"]!!.toLong(), map["fps"]!!.toFloat())
                }
                else -> null
            }

        }

        override fun run() {


            val rows: List<Map<String, String>> = csvReader().readAllWithHeader(inputFile)
            val itemsfromFile = rows.mapNotNull(::fromRow)

            val collectionItems = this@MediaCollectionCommand.items.filter { it.collection == collectionId }.toList()

            val itemsToInsert = itemsfromFile.filter { item ->
                collectionItems.none { it.name == item.name && it.location == item.location && it.itemType == it.itemType }
            }

            itemsToInsert.forEach {
                this@MediaCollectionCommand.items.append(it)
            }

            println("imported ${itemsToInsert.size} of ${rows.size} rows")

        }

    }

    inner class DeleteCollectionCommand : AbstractCollectionCommand("delete") {

        override fun run() {

            //delete items
            val items = this@MediaCollectionCommand.items.filter { it.collection == collectionId }
            items.forEach {
                this@MediaCollectionCommand.items.delete(it)
            }

            //delete collection
            this@MediaCollectionCommand.collections.delete(collectionId)

            println("Collection deleted")

        }


    }
}