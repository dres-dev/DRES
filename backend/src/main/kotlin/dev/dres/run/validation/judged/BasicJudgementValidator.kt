package dev.dres.run.validation.judged

import dev.dres.data.model.submissions.*
import dev.dres.run.audit.DbAuditLogger
import dev.dres.run.exceptions.JudgementTimeoutException
import dev.dres.run.validation.interfaces.JudgementValidator
import dev.dres.run.validation.interfaces.AnswerSetValidator
import kotlinx.dnq.query.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * An implementation of the [JudgementValidator] that checks, if a submission is correct based on a manual judgement by a user.
 *
 * @author Luca Rossetto
 * @author Ralph Gasser
 * @version 2.0.0
 */
open class BasicJudgementValidator(knownCorrectRanges: Collection<ItemRange> = emptyList(), knownWrongRanges: Collection<ItemRange> = emptyList()) : AnswerSetValidator, JudgementValidator {

    companion object {
        private val counter = AtomicInteger()
        private const val judgementTimeout = 60_000 //ms until a request is re-scheduled
    }

    /** The [BasicJudgementValidator]'s ID is simply an auto-incrementing number. */
    override val id = "bjv${counter.incrementAndGet()}"

    /** A [BasicJudgementValidator] is always deferring. */
    override val deferring: Boolean = true

    /** Internal lock on relevant data structures. */
    private val updateLock = ReentrantReadWriteLock()

    /** Internal queue that keeps track of all the [AnswerSetId]s and associated [ItemRange]s that require judgement. */
    private val queue: Queue<Pair<AnswerSetId,ItemRange>> = LinkedList()

    /** Internal map of all [AnswerSetId]s and associated [ItemRange]s that have been retrieved by a judge and are pending a verdict. */
    private val waiting = HashMap<String, Pair<AnswerSetId,ItemRange>>()

    /** Internal queue that keeps track of all the pending [ItemRange]s in need of judgement. */
    private val queuedItemRanges: MutableMap<ItemRange, MutableList<AnswerSetId>> = HashMap()

    /** Helper structure to keep track when a request needs to be re-scheduled */
    private val timeouts = mutableListOf<Pair<Long, String>>()

    /** Internal map of known [ItemRange]s to associated [DbVerdictStatus]. */
    private val cache: MutableMap<ItemRange, DbVerdictStatus> = ConcurrentHashMap()

    init {
        knownCorrectRanges.forEach { this.cache[it] = DbVerdictStatus.CORRECT }
        knownWrongRanges.forEach { this.cache[it] = DbVerdictStatus.WRONG }
    }

    /** Returns the number of [DbSubmission]s that are currently pending a judgement. */
    override val pending: Int
        get() = updateLock.read { this.queue.size + this.waiting.size }

    /** Returns the number of [DbAnswerSet]s pending judgement. */
    override val open: Int
        get() = this.updateLock.read {
            checkTimeOuts()
            return this.queue.size
        }

    /** True, if there are [DbAnswerSet]s pending judgement. */
    override val hasOpen: Boolean
        get() = updateLock.read {
            checkTimeOuts()
            return this.queue.isNotEmpty()
        }

    /**
     * Validates the [DbAnswerSet]. For the [BasicJudgementValidator] this means that the [DbAnswerSet] is enqueued for judgement.
     *
     * Usually requires an ongoing transaction.
     *
     * @param answerSet The [DbAnswerSet] to validate.
     */
    override fun validate(answerSet: DbAnswerSet) = this.updateLock.read {
        //only validate submissions which are not already validated
        if (answerSet.status != DbVerdictStatus.INDETERMINATE) {
            return@read
        }

        //check cache first
        val itemRange = ItemRange(answerSet.answers.first()) //TODO reason about semantics
        val cachedStatus = this.cache[itemRange]
        if (cachedStatus != null) {
            answerSet.status = cachedStatus
        } else if (itemRange !in this.queuedItemRanges.keys) {
            this.updateLock.write {
                this.queue.offer(answerSet.id to itemRange)
                this.queuedItemRanges[itemRange] = mutableListOf(answerSet.id)
            }
        } else {
            this.updateLock.write {
                this.queuedItemRanges[itemRange]!!.add(answerSet.id)
            }
        }
    }

    /**
     * Retrieves and returns the next element that requires a verdict from this [JudgementValidator]'s internal queue.
     *
     * If such an element exists, then the [DbAnswerSet] is returned alongside a unique token, that can be used to update
     * the [DbAnswerSet]'s [DbVerdictStatus].
     *
     * @return [Pair] containing a string token and the [DbSubmission] that should be judged. Can be null!
     */
    override fun next(): Pair<String, DbAnswerSet>? = this.updateLock.write {
        checkTimeOuts()
        val next = this.queue.poll() ?: return@write null
        val answerSet = DbAnswerSet.query(DbAnswerSet::id eq next.first).singleOrNull() ?: return@write null
        val token = UUID.randomUUID().toString()
        this.waiting[token] = next
        this.timeouts.add((System.currentTimeMillis() + judgementTimeout) to token)
        DbAuditLogger.prepareJudgement(answerSet, this, token)
        token to answerSet
    }

    /**
     * Places a verdict for the [DbSubmission] identified by the given token.
     *
     * Requires an ongoing transaction!
     *
     * @param token The token used to identify the [DbSubmission].
     * @param verdict The verdict of the judge.
     */
    override fun judge(token: String, verdict: DbVerdictStatus) = this.updateLock.write {
        this.judgeInternal(token, verdict)
        Unit
    }

    /**
     * Internal implementation for re-use of the judgement logic for re-use.
     *
     * @param token The token used to identify the [DbSubmission].
     * @param verdict The verdict of the judge.
     */
    protected fun judgeInternal(token: String, verdict: DbVerdictStatus): AnswerSetId {
        val next = this.waiting.remove(token)
            ?: throw JudgementTimeoutException("This JudgementValidator does not contain a submission for the token '$token'.") //submission with token not found TODO: this should be logged

        /* Remove from queue set. */
        val otherSubmissions = this.queuedItemRanges.remove(next.second) ?: emptyList()
        for ((i, answerSetId) in (otherSubmissions + next.first).withIndex()) {
            val answerSet = DbAnswerSet.query(DbAnswerSet::id eq answerSetId).singleOrNull()
            if (answerSet != null) {
                answerSet.status = verdict
                if (i == 0) {
                    this.cache[ItemRange(answerSet.answers.first())] = verdict //TODO reason about semantics
                }
            }
        }
        return next.first
    }


    /**
     * Clears this [JudgementValidator] and all the associated queues and maps.
     */
    fun clear() = this.updateLock.write {
        this.waiting.clear()
        this.queue.clear()
    }

    /**
     *
     */
    private fun checkTimeOuts() = this.updateLock.write {
        val now = System.currentTimeMillis()
        val due = timeouts.filter { it.first <= now }
        due.forEach {
            val submission = waiting.remove(it.second)
            if (submission != null) {
                queue.offer(submission)
            }
        }
        timeouts.removeAll(due)
    }
}
