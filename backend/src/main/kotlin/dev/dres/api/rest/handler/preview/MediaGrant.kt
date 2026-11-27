package dev.dres.api.rest.handler.preview

import dev.dres.api.rest.handler.users.SessionToken
import dev.dres.data.model.run.interfaces.EvaluationId
import dev.dres.data.model.template.task.TaskTemplateId
import java.nio.file.Path

/**
 * A short-lived, unguessable capability that grants exactly one viewer session access to exactly one
 * media payload.
 *
 * Grants exist because the previous delivery path embedded base64 media directly in hint and target
 * responses. That made every payload indistinguishable from every other retrieval: once a client had
 * the bytes there was nothing to scope, revoke, or attribute, and any endpoint streaming a whole
 * original could be walked item by item.
 *
 * A grant instead names one payload, is bound to the session that requested the hint or target, has a
 * bounded lifetime, and a bounded redemption budget. [payload] is resolved to an on-disk file at mint
 * time wherever possible (see [MediaGrantPayload.Preview]), which is what makes the viewer's very
 * first fetch immediate instead of waiting for a render.
 *
 * @author DRES
 * @version 1.0.0
 */
class MediaGrant(
    /** Unguessable identifier, used as the URL path segment. */
    val id: String,
    /** The session this grant was minted for. */
    val session: SessionToken,
    /** The evaluation this grant belongs to. */
    val evaluationId: EvaluationId,
    /** The task this grant belongs to, if known. */
    val taskTemplateId: TaskTemplateId?,
    /** The payload this grant designates. */
    val payload: MediaGrantPayload,
    /** Creation time in milliseconds. */
    val issuedAt: Long,
    /** Expiry time in milliseconds. */
    val expiresAt: Long,
    budget: Int
) {

    /** Remaining redemptions. Mutated only under the registry's per-grant discipline. */
    @Volatile
    var remainingBudget: Int = budget
        private set

    /** True if this grant has reached its lifetime at [now]. */
    fun isExpired(now: Long): Boolean = now >= this.expiresAt

    /** Consumes one redemption, returning false when the budget is exhausted. */
    fun redeem(): Boolean {
        if (this.remainingBudget <= 0) return false
        this.remainingBudget -= 1
        return true
    }

    /** Remaining lifetime in seconds, never negative. Used for the `Cache-Control` header. */
    fun remainingSeconds(now: Long = System.currentTimeMillis()): Long =
        ((this.expiresAt - now) / 1000L).coerceAtLeast(0L)

    override fun toString(): String =
        "MediaGrant(id=$id, evaluation=$evaluationId, task=$taskTemplateId, payload=$payload, budget=$remainingBudget)"
}

/**
 * The payload a [MediaGrant] designates.
 *
 * [Preview] is the common case: a cache file that was produced while the hint or target was being
 * built, so redemption is a plain file stream with nothing to render.
 */
sealed class MediaGrantPayload {

    /**
     * An already rendered cache file, e.g. a preview segment or a preview image.
     *
     * @param path Absolute path of the cached file.
     */
    data class Preview(val path: Path) : MediaGrantPayload() {
        override fun toString(): String = "Preview(${path.fileName})"
    }

    /**
     * A whole, unmodified media item from a collection. Only issued to callers that are already
     * entitled to the ground truth (targets after a task has ended).
     *
     * @param itemId Identifier of the collection item.
     */
    data class Item(val itemId: String) : MediaGrantPayload() {
        override fun toString(): String = "Item($itemId)"
    }

    /**
     * A segment of a collection item, rendered on demand.
     *
     * @param itemId Identifier of the collection item.
     * @param start Segment start in milliseconds.
     * @param end Segment end in milliseconds.
     */
    data class ItemSegment(val itemId: String, val start: Long, val end: Long) : MediaGrantPayload() {
        override fun toString(): String = "ItemSegment($itemId, $start-$end)"
    }

    /**
     * A whole file below the configured external media root.
     *
     * @param file Path relative to the external root.
     */
    data class ExternalFile(val file: String) : MediaGrantPayload() {
        override fun toString(): String = "ExternalFile($file)"
    }

    /**
     * A segment of a file below the configured external media root, rendered on demand.
     *
     * @param file Path relative to the external root.
     * @param start Segment start in milliseconds.
     * @param end Segment end in milliseconds.
     */
    data class ExternalSegment(val file: String, val start: Long, val end: Long) : MediaGrantPayload() {
        override fun toString(): String = "ExternalSegment($file, $start-$end)"
    }
}
