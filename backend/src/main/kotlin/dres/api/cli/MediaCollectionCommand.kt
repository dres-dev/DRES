package dres.api.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.validate
import dres.data.dbo.DAO
import dres.data.model.basics.MediaCollection
import dres.data.model.basics.MediaItem
import dres.data.model.basics.MediaItemSegment

class MediaCollectionCommand(val collections: DAO<MediaCollection>, val items: DAO<MediaItem>, val segments: DAO<MediaItemSegment>) :
        NoOpCliktCommand(name = "collection") {

    init {
        this.subcommands(CreateCollectionCommand(), ListCollectionsCommand())
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
}