package dres.api.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.float
import com.github.ajalt.clikt.parameters.types.long
import dres.data.dbo.DAO
import dres.data.model.basics.MediaCollection
import dres.data.model.basics.MediaItem
import dres.data.model.basics.MediaItemSegment
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration

class MediaCollectionCommand(val collections: DAO<MediaCollection>, val items: DAO<MediaItem>, val segments: DAO<MediaItemSegment>) :
        NoOpCliktCommand(name = "collection") {

    init {
        this.subcommands(CreateCollectionCommand(), ListCollectionsCommand(), ShowCollectionCommand(), AddMediaItemCommand())
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

    inner class ShowCollectionCommand : CliktCommand(name = "show") {

        private val collectionId: Long by option("-c", "--collection")
                .convert { this@MediaCollectionCommand.collections.find { c -> c.name == it }?.id ?: -1 }
                .required()
                .validate { require(it > -1) {"Collection not found"} }

        override fun run() {
            this@MediaCollectionCommand.items.filter{ it.collection == collectionId}.forEach {
                println(it)
            }
        }

    }

    inner class AddMediaItemCommand : NoOpCliktCommand(name = "add") {

        init {
            this.subcommands(AddImageCommand(), AddVideoCommans())
        }


        inner class AddImageCommand : CliktCommand(name = "image") {

            private val collectionId: Long by option("-c", "--collection")
                    .convert { this@MediaCollectionCommand.collections.find { c -> c.name == it }?.id ?: -1 }
                    .required()
                    .validate { require(it > -1) {"Collection not found"} }

            private val name: String by option("-n", "--name").required()
            private val path: Path by option("-p", "--path")
                    .convert { Paths.get(it) }
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

        inner class AddVideoCommans : CliktCommand(name = "video"){

            private val collectionId: Long by option("-c", "--collection")
                    .convert { this@MediaCollectionCommand.collections.find { c -> c.name == it }?.id ?: -1 }
                    .required()
                    .validate { require(it > -1) {"Collection not found"} }

            private val name: String by option("-n", "--name").required()
            private val path: Path by option("-p", "--path")
                    .convert { Paths.get(it) }
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
                this@MediaCollectionCommand.items.append(MediaItem.VideoItem(name = name, location = path, collection = collectionId, duration = Duration.ofSeconds(duration), fps = fps, id = -1))
                println("item added")
            }

        }



    }
}