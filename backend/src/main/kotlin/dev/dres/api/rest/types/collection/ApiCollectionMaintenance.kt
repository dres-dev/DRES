package dev.dres.api.rest.types.collection

import kotlinx.serialization.Serializable

/** A problem with a single file or media item, reported by collection scans, checks, CSV export, and bulk deletions. */
@Serializable
data class ApiCollectionIssue(val name: String, val location: String, val problem: String)

/** Parameters of a collection scan: the file endings (without dot) that are added as videos or images. */
@Serializable
data class ApiCollectionScanRequest(val videoTypes: List<String> = emptyList(), val imageTypes: List<String> = emptyList())

/** State of the most recent scan of a collection. */
enum class ApiCollectionScanState { NONE, RUNNING, CANCELLING, DONE, FAILED, CANCELLED }

/** Progress and result of the most recent scan of a collection. */
@Serializable
data class ApiCollectionScanStatus(
    val collectionId: String,
    val state: ApiCollectionScanState,
    /** Number of files with a matching ending that were found in the collection folder. */
    val filesFound: Int,
    /** Number of these files that have been processed so far. */
    val filesProcessed: Int,
    /** Number of new media items that were added. */
    val itemsAdded: Int,
    /** Number of files skipped because an item with the same location or collection-unique name already exists. */
    val itemsSkipped: Int,
    /** Files that could not be analysed (at most 200 are listed, see [issueCount] for the total). */
    val issues: List<ApiCollectionIssue>,
    val issueCount: Int,
    val message: String?,
    val started: Long?,
    val finished: Long?
)

/** Result of checking whether all files of a collection exist and are readable. */
@Serializable
data class ApiCollectionCheckResult(
    val total: Int,
    val ok: Int,
    /** Missing or unreadable files (at most 500 are listed, see [issueCount] for the total). */
    val issues: List<ApiCollectionIssue>,
    val issueCount: Int
)

/** Result of deleting the items of a collection. */
@Serializable
data class ApiCollectionItemsDeletion(
    val deleted: Int,
    /** Items that were kept because templates or submissions still use them (at most 500 are listed). */
    val blocked: List<ApiCollectionIssue>,
    val blockedCount: Int
)
