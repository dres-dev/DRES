package dres.api.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.float
import com.github.ajalt.clikt.parameters.types.long
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import com.jakewharton.picnic.table
import dres.data.dbo.DAO
import dres.data.dbo.DaoIndexer
import dres.data.model.UID
import dres.data.model.basics.media.MediaCollection
import dres.data.model.basics.media.MediaItem
import dres.data.model.basics.media.MediaItemSegment
import dres.data.model.basics.media.MediaItemSegmentList
import dres.data.model.basics.time.TemporalRange
import dres.utilities.FFmpegUtil
import dres.utilities.extensions.UID
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.concurrent.TimeUnit

class MediaCollectionCommand(val collections: DAO<MediaCollection>, val items: DAO<MediaItem>, val itemPathIndex: DaoIndexer<MediaItem, String>, val segments: DAO<MediaItemSegmentList>) :
        NoOpCliktCommand(name = "collection") {

    companion object {
        enum class SortField {
            ID,
            NAME,
            LOCATION,
            DURATION,
            FPS
        }
    }

    init {
        this.subcommands(CreateCollectionCommand(), ListCollectionsCommand(), ShowCollectionCommand(), CheckCollectionCommand(), ScanCollectionCommand(), AddMediaItemCommand(), DeleteItemCommand(), ExportCollectionCommand(), ImportCollectionCommand(), DeleteCollectionCommand(), ImportMediaSegmentsCommand())
    }

    abstract inner class AbstractCollectionCommand(name: String, help: String) : CliktCommand(name = name, help = help) {
        private val collectionNameInput: String? by option("-c", "--collection", help = "Name of the Collection")

        private val collectionIdInput: UID? by option("-i", "--id", help = "Id of the Collection").convert { it.UID() }

        fun actualCollectionId(): UID? = this.collectionIdInput ?: this.collectionNameInput?.let {
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
        val plain by option("-p", "--plain", help = "Plain print: No fancy table presentation for machine readable output").flag(default = false)
        override fun run() {
            println("Available media collections ${this@MediaCollectionCommand.collections.toSet().size}")
            if (plain) {
                this@MediaCollectionCommand.collections.forEach {
                    println(it)
                }
            } else {
                println(
                        table {
                            cellStyle {
                                border = true
                                paddingLeft = 1
                                paddingRight = 1
                            }
                            header {
                                row("id", "name", "description", "basePath")
                            }
                            body {
                                this@MediaCollectionCommand.collections.forEach {
                                    row(it.id, it.name, it.description ?: "", it.basePath)
                                }
                            }
                        }
                )
            }
        }
    }

    inner class ShowCollectionCommand : AbstractCollectionCommand("show", help = "Shows the content of a Collection") {
        val sort by option("-s", "--sort", help = "Chose which sorting to use").enum<SortField>(ignoreCase = true).defaultLazy { SortField.NAME }
        val plain by option("-p", "--plain", help = "Plain formatting. No fancy tables").flag(default = false)

        override fun run() {

            val collectionId = this.actualCollectionId()
            if (collectionId == null) {
                println("Collection not found.")
                return
            }

            val collectionItems = this@MediaCollectionCommand
                    .items.filter { it.collection == collectionId }
                    // First sort reversed by type (i.e. Video before Image), then by name
                    .sortedWith(compareBy<MediaItem> { it.javaClass.name }.reversed().thenBy {
                        when (sort) {
                            SortField.ID -> it.id.string
                            SortField.NAME -> it.name
                            SortField.LOCATION -> it.location
                            /* Small hack as images do not have these properties */
                            SortField.DURATION -> if (it is MediaItem.VideoItem) {
                                it.durationMs
                            } else {
                                it.name
                            }
                            SortField.FPS -> if (it is MediaItem.VideoItem) {
                                it.fps
                            } else {
                                it.name
                            }
                        }
                    })

            if (plain) {
                collectionItems.forEach {
                    println(it)
                }

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
                                collectionItems.forEach {
                                    row {
                                        cell(it.id.string)
                                        cell(it.name)
                                        cell(it.location)
                                        when(it) {
                                            is MediaItem.ImageItem -> {
                                                cell("image") {
                                                    columnSpan = 3
                                                }
                                            }
                                            is MediaItem.VideoItem -> {
                                                cell("video")
                                                cell(it.durationMs)
                                                cell(it.fps)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                )
            }

            println("listed ${collectionItems.size} Media Items")


        }
    }

    inner class CheckCollectionCommand : AbstractCollectionCommand("check", help = "Checks if all the files in a collection are present and accessible") {
        override fun run() {

            val collectionId = this.actualCollectionId()
            if (collectionId == null) {
                println("Collection not found.")
                return
            }

            val collection = this@MediaCollectionCommand.collections[collectionId]!!

            val collectionItems = this@MediaCollectionCommand.items.filter { it.collection == collectionId }

            val baseFile = File(collection.basePath)

            var counter = 0

            collectionItems.forEach {

                val file = File(baseFile, it.location)

                if (file.exists()) {

                    if (file.canRead()) {
                        ++counter
                    } else {
                        println("item ${it.name} at ${file.absolutePath} not readable")
                    }

                } else {
                    println("item ${it.name} at ${file.absolutePath} not found")
                }

            }

            println("successfully checked $counter of ${collectionItems.size} Media Items")


        }
    }

    inner class ScanCollectionCommand : AbstractCollectionCommand("scan", help = "Scans a collection directory and adds found items") {

        val imageTypes by option("-it", "--imageType", help = "Image file types (endings) to be considered in the scan" ).convert { it.toLowerCase() }.multiple()
        val videoTypes by option("-vt", "--videoType", help = "Video file types (endings) to be considered in the scan" ).convert { it.toLowerCase() }.multiple()

        override fun run() {

            val collectionId = this.actualCollectionId()
            if (collectionId == null) {
                println("Collection not found.")
                return
            }

            val collection = this@MediaCollectionCommand.collections[collectionId]!!

            if (imageTypes.isEmpty() && videoTypes.isEmpty()) {
                println("No file types specified.")
                return
            }

            val base = File(collection.basePath)
            val files = base.walkTopDown().filter { it.isFile && (it.extension.toLowerCase() in imageTypes || it.extension in videoTypes) }

            val buffer = mutableListOf<MediaItem>()

            files.forEach {file ->

                println("found ${file.absolutePath}")

                val relativePath = file.relativeTo(base).path

                val existing = this@MediaCollectionCommand.itemPathIndex[relativePath].find { it.collection == collectionId }

                when (file.extension) {
                    in imageTypes -> {

                        if (existing == null) { //add
                            val newItem = MediaItem.ImageItem(UID.EMPTY, file.nameWithoutExtension, relativePath, collection.id)
                            buffer.add(newItem)
                        } else { //skip
                            println("Image ${existing.name} already present")
                        }


                    }
                    in videoTypes -> {

                        println("Analyzing ${file.absolutePath}")

                        val result = FFmpegUtil.analyze(file.toPath()).streams.first()
                        val fps = (result.rFrameRate ?: result.avgFrameRate!!).toFloat()
                        val duration = result.getDuration(TimeUnit.MILLISECONDS).let {
                            if (it != null) {
                                it
                            } else {
                                println("Cannot read duration from file, counting frames")
                                val analysis = FFmpegUtil.analyze(file.toPath(), countFrames = true)
                                val frames = analysis.streams.first().nbReadFrames
                                println("Counted $frames frames")
                                ((frames * 1000) / fps).toLong()
                            }
                        }

                        println("Found frame rate to be $fps frames per seconds and duration $duration ms")

                        if (existing == null) { //add
                            val newItem = MediaItem.VideoItem(UID.EMPTY, file.nameWithoutExtension, relativePath, collection.id, duration, fps)
                            buffer.add(newItem)
                        } else { //skip
                            val newItem = MediaItem.VideoItem(existing.id, existing.name, relativePath, collection.id, duration, fps)
                            this@MediaCollectionCommand.items.update(newItem)
                            println("Updated Video ${newItem.name}")
                        }

                    }
                }

                println()

                if (buffer.size >= 1000){
                    println()
                    print("Storing buffer...")
                    this@MediaCollectionCommand.items.batchAppend(buffer)
                    buffer.clear()
                    println("done")
                }

            }

            println()
            if (buffer.isNotEmpty()) {
                print("Storing buffer...")
                this@MediaCollectionCommand.items.batchAppend(buffer)
                println("done")
            }


        }


    }

    inner class DeleteCollectionCommand : AbstractCollectionCommand("delete", help = "Deletes a Collection") {
        override fun run() {

            val collectionId = this.actualCollectionId()
            if (collectionId == null) {
                println("Collection not found.")
                return
            }
            print("looking up Media Items...")
            val itemIds = this@MediaCollectionCommand.items.filter { it.collection == collectionId }.map { it.id }
            println("done, found ${itemIds.size} Items")

            print("looking up Media Item Segments...")
            val itemIdSet = itemIds.toSet()
            val segmentIds = this@MediaCollectionCommand.segments.filter { itemIdSet.contains(it.mediaItemId) }.map { it.id }
            println("done, found ${segmentIds.size} Segments")

            print("Deleting Media Item Segments...")
            this@MediaCollectionCommand.segments.batchDelete(segmentIds)
            println("done")

            print("Deleting Media Items...")
            this@MediaCollectionCommand.items.batchDelete(itemIds)
            println("done")

            print("Deleting Collection...")
            this@MediaCollectionCommand.collections.delete(collectionId)
            println("done")

        }
    }

    inner class DeleteItemCommand : AbstractCollectionCommand("deleteItem", help = "Deletes a Media Item") {

        private val itemName: String by option("-in", "--itemName", help = "Name of the Item").default("")
        private val itemIdInput: UID? by option("-ii", "--itemId", help = "Id of the Item").convert { it.UID() }

        override fun run() {

            val collectionId = this.actualCollectionId()
            if (collectionId == null && itemIdInput == null) {
                println("Collection not found.")
                return
            }

            if (itemName.isBlank() && itemIdInput == null) {
                println("Item not specified.")
                return
            }

            val itemId = itemIdInput ?: this@MediaCollectionCommand.items.find { it.collection == collectionId && it.name == itemName }?.id

            if (itemId == null) {
                println("Item not found.")
                return
            }

            this@MediaCollectionCommand.items.delete(itemId)
            println("Item '${itemId.string}' deleted")

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
                this@MediaCollectionCommand.items.append(MediaItem.ImageItem(name = name, location = path, collection = collectionId, id = UID.EMPTY))
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
                this@MediaCollectionCommand.items.append(MediaItem.VideoItem(name = name, location = path, collection = collectionId, durationMs = duration, fps = fps, id = UID.EMPTY))
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
            is MediaItem.ImageItem -> listOf("image", item.name, item.location, null, null)
            is MediaItem.VideoItem -> listOf("video", item.name, item.location, item.duration.toMillis().toString(), item.fps.toString())
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

        private fun fromRow(map: Map<String, String>, collectionId: UID): MediaItem? {

            if (!map.containsKey("itemType") || !map.containsKey("name") || !map.containsKey("location")) {
                return null
            }

            return when (map.getValue("itemType")) {
                "image" -> MediaItem.ImageItem(UID.EMPTY, map.getValue("name"), map.getValue("location"), collectionId)
                "video" -> {
                    if (map.containsKey("duration") && map.containsKey("fps")) {
                        return MediaItem.VideoItem(UID.EMPTY, map.getValue("name"), map.getValue("location"), collectionId, map.getValue("duration").toLong(), map.getValue("fps").toFloat())
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
                collectionItems.none { it.name == item.name && it.location == item.location }
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
                    .filter { mediaItemIds.contains(it.mediaItemId) }.flatMap { it.segments.map { s -> it.mediaItemId to s.name } }.toMutableSet()
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
                if (existingSegments.contains(pair)) {
                    return@map null
                }
                existingSegments.add(pair)

                MediaItemSegment(videoId, name, TemporalRange(start, end))
            }.filterNotNull()
            println("done, generated ${segments.size} valid, non-duplicate segments")

            val grouped = segments.groupBy { it.mediaItemId }
            val affected = this@MediaCollectionCommand.segments.filter { grouped.keys.contains(it.mediaItemId) }

            affected.forEach {
                it.segments.addAll(grouped[it.mediaItemId] ?: emptyList())
            }

            val affectedMediaItemIds = affected.map { it.mediaItemId }.toSet()

            val new = grouped.filter { !affectedMediaItemIds.contains(it.key) }.map { MediaItemSegmentList(UID.EMPTY, it.key, it.value.toMutableList()) }


            print("storing segments...")
            affected.forEach {
                this@MediaCollectionCommand.segments.update(it)
            }
            this@MediaCollectionCommand.segments.batchAppend(new)
            println("done")
        }

    }
}