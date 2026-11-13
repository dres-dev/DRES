package dev.dres.mgmt

import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import com.github.kokorin.jaffree.StreamType
import com.github.kokorin.jaffree.ffprobe.FFprobe
import com.github.kokorin.jaffree.ffprobe.FFprobeResult
import dev.dres.api.rest.types.collection.*
import dev.dres.data.model.media.CollectionId
import dev.dres.data.model.media.DbMediaCollection
import dev.dres.data.model.media.DbMediaItem
import dev.dres.data.model.run.DbEvaluation
import dev.dres.data.model.submissions.DbAnswer
import dev.dres.data.model.template.DbEvaluationTemplate
import dev.dres.data.model.template.task.DbHint
import dev.dres.data.model.template.task.DbTaskTemplateTarget
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.*
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ThreadFactory
import java.util.concurrent.Future
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.relativeTo

/**
 * Maintenance operations on media collections that are shared by the CLI and the REST API:
 * scanning the collection folder, checking files, exporting the item list as CSV and deleting items in bulk.
 */
object MediaCollectionMaintenance {

    private val logger = LoggerFactory.getLogger(this.javaClass)

    private lateinit var store: TransientEntityStore

    /** Provides the folder that contains the ffprobe binary. */
    private lateinit var ffmpegPath: () -> Path?

    /** Number of new items that are written to the database per transaction during a scan. */
    private const val SCAN_CHUNK_SIZE = 200

    /** Number of items that are deleted per transaction. */
    private const val DELETE_CHUNK_SIZE = 500

    /** Maximum number of issues returned to clients (the total is always reported). */
    private const val MAX_SCAN_ISSUES = 200
    private const val MAX_REPORTED_ISSUES = 500

    @Synchronized
    fun init(store: TransientEntityStore, ffmpegPath: () -> Path?) {
        if (this::store.isInitialized && this.store !== store) {
            check(scanExecutor.isShutdown || scans.values.none {
                it.state == ApiCollectionScanState.RUNNING || it.state == ApiCollectionScanState.CANCELLING
            }) { "Cannot reinitialize collection maintenance while scans are running." }
            scans.clear()
        }
        if (scanExecutor.isShutdown) scanExecutor = createScanExecutor()
        stopped = false
        this.store = store
        this.ffmpegPath = ffmpegPath
    }

    /* ---------------------------------------------------------------- scan */

    /** Mutable progress of a running scan; read through [status]. */
    private class ScanJob(val collectionId: CollectionId) {
        @Volatile var state = ApiCollectionScanState.RUNNING
        @Volatile var filesFound = 0
        @Volatile var filesProcessed = 0
        @Volatile var itemsAdded = 0
        @Volatile var itemsSkipped = 0
        @Volatile var message: String? = null
        private val issues: MutableList<ApiCollectionIssue> = mutableListOf()
        @Volatile var issueCount = 0
        val started = System.currentTimeMillis()
        @Volatile var future: Future<*>? = null
        @Volatile var cancellationRequested = false
        private var workerStarted = false
        @Volatile var finished: Long? = null

        @Synchronized fun requestCancellation(message: String): Boolean {
            if (state != ApiCollectionScanState.RUNNING && state != ApiCollectionScanState.CANCELLING) return false
            cancellationRequested = true
            this.message = message
            if (workerStarted) state = ApiCollectionScanState.CANCELLING else markCancelled(message)
            return true
        }

        @Synchronized fun startWorker(): Boolean {
            if (cancellationRequested || state != ApiCollectionScanState.RUNNING) {
                markCancelled("Scan cancelled before it started.")
                return false
            }
            workerStarted = true
            return true
        }

        @Synchronized fun finishWorker() {
            workerStarted = false
            if (cancellationRequested || state == ApiCollectionScanState.CANCELLING) markCancelled("Scan cancelled.")
            if (finished == null) finished = System.currentTimeMillis()
        }

        @Synchronized fun completeSuccessfully() {
            if (cancellationRequested || state == ApiCollectionScanState.CANCELLING) {
                markCancelled("Scan cancelled.")
            } else {
                state = ApiCollectionScanState.DONE
                finished = System.currentTimeMillis()
            }
        }

        @Synchronized fun completeWithFailure(error: Exception) {
            if (cancellationRequested || Thread.currentThread().isInterrupted) {
                markCancelled("Scan cancelled.")
            } else {
                message = "Scan failed: ${error.message}"
                state = ApiCollectionScanState.FAILED
                finished = System.currentTimeMillis()
            }
        }

        @Synchronized private fun markCancelled(message: String) {
            cancellationRequested = true
            this.message = message
            state = ApiCollectionScanState.CANCELLED
            finished = System.currentTimeMillis()
        }

        @Synchronized fun addIssue(issue: ApiCollectionIssue) {
            issueCount++
            if (issues.size < MAX_SCAN_ISSUES) issues.add(issue)
        }

        @Synchronized fun issuesSnapshot(): List<ApiCollectionIssue> = issues.toList()

        fun status() = ApiCollectionScanStatus(
            collectionId = collectionId,
            state = state,
            filesFound = filesFound,
            filesProcessed = filesProcessed,
            itemsAdded = itemsAdded,
            itemsSkipped = itemsSkipped,
            issues = issuesSnapshot(),
            issueCount = issueCount,
            message = message,
            started = started,
            finished = finished
        )
    }

    /** Retain completed scan status for a bounded period to avoid an unbounded per-collection history. */
    private val scans = ConcurrentHashMap<CollectionId, ScanJob>()
    private val scanRetentionMs = 60 * 60 * 1000L
    private const val MAX_SCAN_HISTORY = 512
    @Volatile private var scanExecutor = createScanExecutor()
    @Volatile private var stopped = false

    private fun createScanExecutor() = ThreadPoolExecutor(
        2, 2, 0, TimeUnit.MILLISECONDS, ArrayBlockingQueue(16), ThreadFactory { runnable ->
            Thread(runnable, "collection-scan").apply { isDaemon = true }
        }, ThreadPoolExecutor.AbortPolicy()
    )

    /** Cancels the active scan for a collection; returns its last known status if no scan is active. */
    @Synchronized
    fun cancelScan(collectionId: CollectionId): ApiCollectionScanStatus {
        val job = scans[collectionId] ?: return scanStatus(collectionId)
        if (job.requestCancellation("Scan cancellation requested.")) job.future?.cancel(true)
        return job.status()
    }

    /** Returns the status of the most recent scan of the given collection. */
    fun scanStatus(collectionId: CollectionId): ApiCollectionScanStatus {
        val job = this.scans[collectionId]
        if (job != null && job.state != ApiCollectionScanState.RUNNING && job.state != ApiCollectionScanState.CANCELLING &&
            (job.finished ?: Long.MAX_VALUE) < System.currentTimeMillis() - scanRetentionMs
        ) {
            this.scans.remove(collectionId, job)
        }
        return this.scans[collectionId]?.status() ?: ApiCollectionScanStatus(
            collectionId, ApiCollectionScanState.NONE, 0, 0, 0, 0, emptyList(), 0, null, null, null
        )
    }

    private fun trimScanHistory() {
        val completed = scans.values.filter {
            it.state != ApiCollectionScanState.RUNNING && it.state != ApiCollectionScanState.CANCELLING
        }
            .sortedBy { it.finished ?: Long.MIN_VALUE }
        val oldestAllowed = System.currentTimeMillis() - scanRetentionMs
        completed.filter { (it.finished ?: Long.MAX_VALUE) < oldestAllowed }
            .forEach { scans.remove(it.collectionId, it) }
        val excess = scans.size - MAX_SCAN_HISTORY
        if (excess > 0) completed.take(excess).forEach { scans.remove(it.collectionId, it) }
    }

    /** Stops accepting scan requests and cancels running and queued scans during application shutdown. */
    @Synchronized
    fun stop() {
        stopped = true
        val executor = scanExecutor
        val queued = executor.shutdownNow()
        scans.values.forEach { job ->
            if (job.requestCancellation("Scan cancelled during shutdown.")) job.future?.cancel(true)
        }
        queued.forEach { task ->
            (task as? Future<*>)?.cancel(false)
        }
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                logger.warn("Collection scan workers did not stop within 5 seconds during shutdown.")
            }
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            logger.warn("Interrupted while waiting for collection scan workers to stop.")
        }
    }

    /**
     * Starts scanning the folder of the given collection in the background and adds every file with one of the given
     * endings that is not yet part of the collection. Progress can be followed with [scanStatus].
     *
     * @throws IllegalArgumentException If the collection does not exist, its folder cannot be read or no endings are given.
     * @throws IllegalStateException If a scan of this collection is already running.
     */
    @Synchronized
    fun startScan(collectionId: CollectionId, videoTypes: Collection<String>, imageTypes: Collection<String>): ApiCollectionScanStatus {
        check(!stopped && !scanExecutor.isShutdown) { "The collection scan service is stopped." }
        val videos = normaliseTypes(videoTypes)
        val images = normaliseTypes(imageTypes)
        require(videos.isNotEmpty() || images.isNotEmpty()) { "No file types specified." }
        val base = checkedBasePath(collectionId)

        val now = System.currentTimeMillis()
        scans.entries.removeIf { (_, scan) ->
            scan.state != ApiCollectionScanState.RUNNING && scan.state != ApiCollectionScanState.CANCELLING &&
                (scan.finished ?: now) < now - scanRetentionMs
        }
        val job = ScanJob(collectionId)
        val previous = this.scans.compute(collectionId) { _, current ->
            if (current?.state == ApiCollectionScanState.RUNNING || current?.state == ApiCollectionScanState.CANCELLING) current else job
        }
        check(previous === job) { "A scan of this collection is already running." }
        trimScanHistory()

        try {
            job.future = scanExecutor.submit {
                if (!job.startWorker()) return@submit
                try {
                    runScan(job, base, videos, images) { }
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    job.requestCancellation("Scan cancelled.")
                } catch (e: Exception) {
                    if (!job.cancellationRequested && !Thread.currentThread().isInterrupted) {
                        logger.error("Scan of collection $collectionId failed.", e)
                    }
                    job.completeWithFailure(e)
                } finally {
                    job.finishWorker()
                }
            }
            if (job.cancellationRequested) job.future?.cancel(true)
        } catch (e: java.util.concurrent.RejectedExecutionException) {
            scans.remove(collectionId, job)
            throw IllegalStateException("The collection scan service is busy. Try again later.", e)
        }

        return job.status()
    }

    /**
     * Scans the folder of the given collection in the calling thread (used by the CLI).
     *
     * @param log Receives a line for every analysed file.
     */
    fun scan(collectionId: CollectionId, videoTypes: Collection<String>, imageTypes: Collection<String>, log: (String) -> Unit): ApiCollectionScanStatus {
        val videos = normaliseTypes(videoTypes)
        val images = normaliseTypes(imageTypes)
        require(videos.isNotEmpty() || images.isNotEmpty()) { "No file types specified." }
        val job = ScanJob(collectionId)
        runScan(job, checkedBasePath(collectionId), videos, images, log)
        job.finished = System.currentTimeMillis()
        return job.status()
    }

    private fun normaliseTypes(types: Collection<String>) =
        types.map { it.trim().trimStart('.').lowercase() }.filter { it.isNotEmpty() }.toSet()

    /** Returns the base folder of the collection after making sure it is a readable directory. */
    private fun checkedBasePath(collectionId: CollectionId): Path {
        val collection = MediaCollectionManager.getCollection(collectionId)
            ?: throw IllegalArgumentException("Collection $collectionId not found.")
        val base = Paths.get(collection.basePath ?: throw IllegalArgumentException("Collection has no base path."))
        require(Files.exists(base)) { "The collection folder '$base' does not exist." }
        require(Files.isDirectory(base)) { "'$base' is not a folder." }
        require(Files.isReadable(base)) { "The collection folder '$base' is not readable." }
        return try {
            base.toRealPath()
        } catch (e: java.io.IOException) {
            throw IllegalArgumentException("The collection folder '$base' cannot be resolved.", e)
        }
    }

    private fun runScan(job: ScanJob, base: Path, videoTypes: Set<String>, imageTypes: Set<String>, log: (String) -> Unit) {
        val collectionId = job.collectionId
        checkScanActive(job)
        val files = Files.walk(base).use { stream ->
            stream.filter { path ->
                checkScanActive(job)
                if (Thread.currentThread().isInterrupted) return@filter false
                if (path == base || !Files.isRegularFile(path)) return@filter false
                val extension = path.extension.lowercase()
                (extension in videoTypes || extension in imageTypes) &&
                    runCatching { path.toRealPath().startsWith(base) }.getOrDefault(false)
            }.sorted().toList()
        }
        job.filesFound = files.size

        /* Files that are already part of the collection are skipped without analysing them again. */
        val existing = this.store.transactional(true) {
            val collection = DbMediaCollection.query(DbMediaCollection::id eq collectionId).first()
            collection.items.asSequence().map { it.location }.toHashSet()
        }

        files.chunked(SCAN_CHUNK_SIZE).forEach { chunk ->
            checkScanActive(job)
            val items = chunk.mapNotNull { file ->
                checkScanActive(job)
                val location = file.relativeTo(base).toString()
                val item = if (location in existing) {
                    job.itemsSkipped++
                    null
                } else try {
                    when (file.extension.lowercase()) {
                        in imageTypes -> {
                            log("Found image $file.")
                            ApiMediaItem(mediaItemId = "", name = file.fileName.nameWithoutExtension, type = ApiMediaType.IMAGE,
                                collectionId = collectionId, location = location)
                        }
                        else -> {
                            log("Found video $file; analysing...")
                            val (durationMs, fps) = analyseVideo(file)
                            checkScanActive(job)
                            log("Frame rate $fps fps, duration $durationMs ms.")
                            ApiMediaItem(mediaItemId = "", name = file.fileName.nameWithoutExtension, type = ApiMediaType.VIDEO,
                                collectionId = collectionId, location = location, durationMs = durationMs, fps = fps)
                        }
                    }
                } catch (e: Exception) {
                    if (job.cancellationRequested || Thread.currentThread().isInterrupted || e is InterruptedException) {
                        throw InterruptedException("Collection scan was cancelled.").also { it.initCause(e) }
                    }
                    logger.warn("Could not analyse $file during scan of collection $collectionId.", e)
                    log("Could not analyse $file: ${e.message}. Skipping.")
                    job.addIssue(ApiCollectionIssue(file.fileName.nameWithoutExtension, location, e.message ?: e.javaClass.simpleName))
                    null
                }
                job.filesProcessed++
                item
            }
            if (items.isNotEmpty()) {
                checkScanActive(job)
                val added = MediaCollectionManager.addMediaItems(collectionId, items)
                job.itemsAdded += added
                job.itemsSkipped += items.size - added
            }
        }

        checkScanActive(job)
        job.message = "Added ${job.itemsAdded} new items, skipped ${job.itemsSkipped} existing or duplicate items" +
                if (job.issueCount > 0) ", ${job.issueCount} files could not be analysed." else "."
        job.completeSuccessfully()
    }

    private fun checkScanActive(job: ScanJob) {
        if (job.cancellationRequested || Thread.currentThread().isInterrupted) {
            throw InterruptedException("Collection scan was cancelled.")
        }
    }

    private fun ffprobe(file: Path, countFrames: Boolean): FFprobeResult =
        FFprobe.atPath(this.ffmpegPath() ?: throw IllegalStateException("FFmpeg is not available."))
            .setInput(file)
            .setShowStreams(true)
            .setCountFrames(countFrames)
            .setSelectStreams(StreamType.VIDEO)
            .execute()

    /** Returns duration in milliseconds and frame rate of a video; counts frames if the duration is not stored in the file. */
    private fun analyseVideo(file: Path): Pair<Long, Float> {
        val stream = ffprobe(file, false).streams.firstOrNull() ?: throw IllegalStateException("No video stream found.")
        val fps = (stream.rFrameRate ?: stream.avgFrameRate ?: throw IllegalStateException("Frame rate unknown.")).toFloat()
        val duration = stream.duration?.let { (it.toDouble() * 1000.0).toLong() } ?: run {
            val frames = ffprobe(file, true).streams.first().nbReadFrames
            ((frames * 1000) / fps).toLong()
        }
        return duration to fps
    }

    /* ---------------------------------------------------------------- check */

    /** Checks that the file of every item of the collection exists and is readable; null if the collection does not exist. */
    fun check(collectionId: CollectionId): ApiCollectionCheckResult? {
        val populated = MediaCollectionManager.getPopulatedCollection(collectionId) ?: return null
        val base = populated.collection.basePath?.let { runCatching { Paths.get(it).toRealPath() }.getOrNull() }
        val issues = mutableListOf<ApiCollectionIssue>()
        var issueCount = 0
        fun report(issue: ApiCollectionIssue) {
            issueCount++
            if (issues.size < MAX_REPORTED_ISSUES) issues.add(issue)
        }
        for (item in populated.items) {
            val candidate = runCatching { Paths.get(item.location) }.getOrNull()
            val path = if (base != null && candidate != null && !candidate.isAbsolute) base.resolve(candidate).normalize() else null
            when {
                base == null -> report(ApiCollectionIssue(item.name, item.location, "Collection folder is missing or unavailable."))
                path == null -> report(ApiCollectionIssue(item.name, item.location, "Invalid or absolute path."))
                !path.startsWith(base) -> report(ApiCollectionIssue(item.name, item.location, "Invalid path outside collection folder."))
                !Files.exists(path) -> report(ApiCollectionIssue(item.name, item.location, "File not found: $path"))
                !runCatching { path.toRealPath().startsWith(base) }.getOrDefault(false) -> report(ApiCollectionIssue(item.name, item.location, "File resolves outside collection folder."))
                !Files.isRegularFile(path) -> report(ApiCollectionIssue(item.name, item.location, "Path is not a regular file."))
                !Files.isReadable(path) -> report(ApiCollectionIssue(item.name, item.location, "File not readable: $path"))
            }
        }
        return ApiCollectionCheckResult(
            total = populated.items.size,
            ok = populated.items.size - issueCount,
            issues = issues.sortedBy { it.location },
            issueCount = issueCount
        )
    }

    /* ---------------------------------------------------------------- export */

    /** Header of the CSV export; the same format is read by the CLI command `collection import`. */
    private val CSV_HEADER = listOf("itemType", "name", "location", "duration", "fps")

    /** Returns the items of the collection as CSV; null if the collection does not exist. */
    fun exportCsv(collectionId: CollectionId): String? {
        val populated = MediaCollectionManager.getPopulatedCollection(collectionId) ?: return null
        val out = ByteArrayOutputStream()
        csvWriter().open(out) {
            writeRow(CSV_HEADER)
            populated.items.sortedBy { it.location }.forEach {
                writeRow(listOf(it.type, it.name, it.location, it.durationMs?.toString(), it.fps?.toString()))
            }
        }
        return out.toString(Charsets.UTF_8)
    }

    /* ---------------------------------------------------------------- delete */

    /**
     * Deletes the items of a collection that match [selector] (by default all of them).
     *
     * Items that are still used as task targets or hints in a template, or that were submitted in an evaluation,
     * cannot be deleted. They are kept and reported together with what uses them.
     *
     * @return The result, or null if the collection does not exist.
     */
    fun deleteItems(collectionId: CollectionId, selector: (id: String, name: String) -> Boolean = { _, _ -> true }): ApiCollectionItemsDeletion? {
        val candidates = this.store.transactional(true) {
            val collection = DbMediaCollection.query(DbMediaCollection::id eq collectionId).firstOrNull()
                ?: return@transactional null
            collection.items.asSequence().filter { selector(it.id, it.name) }
                .map { Triple(it.id, it.name, it.location) }.toList()
        } ?: return null

        val blocked = mutableListOf<ApiCollectionIssue>()
        var blockedCount = 0
        var deleted = 0

        candidates.chunked(DELETE_CHUNK_SIZE).forEach { chunk ->
            val result = this.store.transactional {
                var removed = 0
                val retained = mutableListOf<ApiCollectionIssue>()
                chunk.forEach { (id, name, location) ->
                    val item = DbMediaItem.query(DbMediaItem::id eq id).firstOrNull() ?: return@forEach
                    val usages = usagesOf(item)
                    if (usages.isEmpty()) {
                        item.delete()
                        removed++
                    } else {
                        retained.add(ApiCollectionIssue(name, location, usages.joinToString("; ")))
                    }
                }
                removed to retained
            }
            deleted += result.first
            blockedCount += result.second.size
            blocked.addAll(result.second.take(MAX_REPORTED_ISSUES - blocked.size))
        }

        return ApiCollectionItemsDeletion(
            deleted = deleted,
            blocked = blocked.sortedBy { it.location },
            blockedCount = blockedCount
        )
    }

    /** Describes everything that still references the given item. Requires a transaction. */
    private fun usagesOf(item: DbMediaItem): List<String> {
        val usages = linkedSetOf<String>()
        DbTaskTemplateTarget.query(DbTaskTemplateTarget::item eq item).asSequence().forEach {
            usages.add("target of task '${it.task.name}' in ${describe(it.task.evaluation)}")
        }
        DbHint.query(DbHint::item eq item).asSequence().forEach {
            usages.add("hint of task '${it.task.name}' in ${describe(it.task.evaluation)}")
        }
        DbAnswer.query(DbAnswer::item eq item).asSequence().forEach {
            usages.add("submitted in evaluation '${it.answerSet.task.evaluation.name}'")
        }
        return usages.toList()
    }

    /** Names a template; template copies that belong to an evaluation are named after that evaluation. */
    private fun describe(template: DbEvaluationTemplate): String =
        if (template.instance) {
            val evaluation = DbEvaluation.query(DbEvaluation::template eq template).firstOrNull()
            "evaluation '${evaluation?.name ?: template.name}'"
        } else {
            "template '${template.name}'"
        }
}
