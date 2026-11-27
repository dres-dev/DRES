package dev.dres.api.rest.handler.preview

import dev.dres.api.rest.handler.users.SessionToken
import dev.dres.data.model.run.interfaces.EvaluationId
import dev.dres.data.model.template.task.TaskTemplateId
import org.slf4j.LoggerFactory
import java.security.SecureRandom
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap

/**
 * Mints, redeems, and revokes [MediaGrant]s.
 *
 * The registry is the single place that decides whether a caller may read a specific media payload.
 * Minting only happens on request paths that have already performed the expensive authorization (a
 * hint or target request that passed the run manager's gatekeeper, for a caller entitled to it), so
 * redemption is a cheap, constant-time lookup: does the grant exist, is it still alive, does it
 * belong to the calling session, and does it still have budget?
 *
 * Grants are indexed by evaluation and task so they can be revoked in bulk when a task or run ends.
 * That bounds how long a copied URL stays useful.
 *
 * @author DRES
 * @version 1.0.0
 */
object MediaGrantRegistry {

    private val logger = LoggerFactory.getLogger(this.javaClass)

    /** Cryptographically strong source for grant identifiers. */
    private val random = SecureRandom()

    /** Active grants, keyed by unguessable identifier. */
    private val grants = ConcurrentHashMap<String, MediaGrant>()

    /** Grant ids per evaluation, so run end revokes with a targeted sweep. */
    private val grantsByEvaluation = ConcurrentHashMap<EvaluationId, MutableSet<String>>()

    /** Grant ids per (evaluation, task), so task end revokes only that task's payloads. */
    private val grantsByTask = ConcurrentHashMap<Pair<EvaluationId, TaskTemplateId>, MutableSet<String>>()

    /**
     * Default lifetime of a grant in milliseconds.
     *
     * This value is a **backstop**, not the mechanism that bounds exposure. A hint can be replayed for
     * the whole duration of its task, so the lifetime has to comfortably exceed the longest sensible
     * task; otherwise playback would die mid-task with a 410. The real bound is revocation: grants are
     * dropped for a task when it ends ([revokeTask]) and for a run when it ends ([revokeEvaluation]),
     * so a URL is only useful for as long as the run it belongs to.
     */
    const val DEFAULT_TTL_MS = 6L * 60L * 60L * 1000L

    /**
     * Default redemption budget per grant.
     *
     * One media element is fetched more than once in practice: seeking issues additional range requests,
     * a decoder reset re-fetches the header, and a looping hint may be re-read when the browser's cache
     * entry expires. A tight budget would therefore break playback, so the budget is generous; what
     * actually prevents bulk retrieval is that a grant names exactly one payload and is revoked with its
     * task.
     */
    const val DEFAULT_BUDGET = 200

    /** Upper bound on concurrent grants, so a hostile client cannot exhaust memory. */
    private const val MAX_GRANTS = 50_000

    /**
     * Registers a new grant.
     *
     * @param session The session the grant is bound to.
     * @param evaluationId The evaluation the payload belongs to.
     * @param taskTemplateId The task the payload belongs to, if known.
     * @param payload The [MediaGrantPayload] the grant designates.
     * @param ttlMs Lifetime in milliseconds.
     * @param budget Number of redemptions.
     * @return The freshly minted [MediaGrant].
     * @throws IllegalStateException If too many grants are active.
     */
    fun mint(
        session: SessionToken,
        evaluationId: EvaluationId,
        taskTemplateId: TaskTemplateId?,
        payload: MediaGrantPayload,
        ttlMs: Long = DEFAULT_TTL_MS,
        budget: Int = DEFAULT_BUDGET
    ): MediaGrant {
        val now = System.currentTimeMillis()
        this.evictExpired(now)
        if (this.grants.size >= MAX_GRANTS) {
            this.evictOldest(this.grants.size - MAX_GRANTS + 1)
            if (this.grants.size >= MAX_GRANTS) {
                logger.warn("Refusing to mint a media grant: {} grants are already active.", this.grants.size)
                throw IllegalStateException("Too many active media grants.")
            }
        }

        val grant = MediaGrant(
            id = newId(),
            session = session,
            evaluationId = evaluationId,
            taskTemplateId = taskTemplateId,
            payload = payload,
            issuedAt = now,
            expiresAt = now + ttlMs,
            budget = budget
        )
        this.grants[grant.id] = grant
        this.grantsByEvaluation.computeIfAbsent(evaluationId) { ConcurrentHashMap.newKeySet() }.add(grant.id)
        if (taskTemplateId != null) {
            this.grantsByTask.computeIfAbsent(evaluationId to taskTemplateId) { ConcurrentHashMap.newKeySet() }.add(grant.id)
        }
        logger.debug("Minted {}", grant)
        return grant
    }

    /**
     * Redeems a grant on behalf of [session].
     *
     * @param id The grant identifier taken from the request path.
     * @param session The calling session, which must be the one the grant was minted for.
     * @param now Current time, for expiry checks.
     * @return The [MediaGrant] if it is valid for this caller and still has budget, null otherwise.
     */
    fun redeem(id: String, session: SessionToken?, now: Long = System.currentTimeMillis()): MediaGrant? {
        val grant = this.grants[id] ?: return null
        if (session == null || grant.session != session) {
            /* Do not log the identifier of a grant someone else presented: it is a live capability for
             * the session that owns it, and this log line would leak it. */
            logger.warn("Rejected a media grant presented by a session it was not minted for.")
            return null
        }
        if (grant.isExpired(now)) {
            this.revoke(id)
            return null
        }
        if (!grant.redeem()) {
            logger.warn("Rejected media grant {}: budget exhausted.", id)
            return null
        }
        return grant
    }

    /** Returns the grant with [id] if it exists, without redeeming it. Intended for tests. */
    fun get(id: String): MediaGrant? = this.grants[id]

    /** Number of currently registered grants. Intended for tests. */
    val size: Int
        get() = this.grants.size

    /** Revokes every grant belonging to [evaluationId]. */
    fun revokeEvaluation(evaluationId: EvaluationId) {
        val ids = this.grantsByEvaluation.remove(evaluationId) ?: return
        ids.forEach { this.grants.remove(it) }
        this.grantsByTask.keys.removeIf { it.first == evaluationId }
        if (ids.isNotEmpty()) logger.debug("Revoked {} media grants of evaluation {}", ids.size, evaluationId)
    }

    /** Revokes every grant belonging to [taskTemplateId] within [evaluationId]. */
    fun revokeTask(evaluationId: EvaluationId, taskTemplateId: TaskTemplateId) {
        val ids = this.grantsByTask.remove(evaluationId to taskTemplateId) ?: return
        ids.forEach { this.grants.remove(it) }
        this.grantsByEvaluation[evaluationId]?.removeAll(ids)
        if (ids.isNotEmpty()) logger.debug("Revoked {} media grants of task {} in evaluation {}", ids.size, taskTemplateId, evaluationId)
    }

    /** Revokes a single grant. */
    fun revoke(id: String) {
        this.grants.remove(id)
        this.grantsByEvaluation.values.forEach { it.remove(id) }
        this.grantsByTask.values.forEach { it.remove(id) }
    }

    /** Drops all state. Intended for tests. */
    fun clear() {
        this.grants.clear()
        this.grantsByEvaluation.clear()
        this.grantsByTask.clear()
    }

    /** Removes grants whose lifetime has passed. */
    private fun evictExpired(now: Long) {
        val expired = this.grants.values.filter { it.isExpired(now) }.map { it.id }
        if (expired.isEmpty()) return
        expired.forEach { this.revoke(it) }
        logger.debug("Evicted {} expired media grants", expired.size)
    }

    /** Removes the [count] oldest grants, used only when the registry is at its hard cap. */
    private fun evictOldest(count: Int) {
        this.grants.values.sortedBy { it.issuedAt }.take(count).forEach { this.revoke(it.id) }
    }

    /** Builds a new unguessable identifier: 32 bytes of CSPRNG output, url-safe and unpadded. */
    private fun newId(): String {
        val bytes = ByteArray(32)
        this.random.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
}
