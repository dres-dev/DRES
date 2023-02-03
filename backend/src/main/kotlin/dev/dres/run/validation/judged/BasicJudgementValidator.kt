package dev.dres.run.validation.judged

import dev.dres.data.model.submissions.DbSubmission
import dev.dres.data.model.submissions.DbAnswerSet
import dev.dres.data.model.submissions.DbVerdictStatus
import dev.dres.run.audit.AuditLogger
import dev.dres.run.exceptions.JudgementTimeoutException
import dev.dres.run.validation.interfaces.JudgementValidator
import dev.dres.run.validation.interfaces.SubmissionValidator
import kotlinx.dnq.query.asSequence
import kotlinx.dnq.query.first
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * A validator class that checks, if a submission is correct based on a manual judgement by a user.
 *
 *
 * @author Luca Rossetto
 * @author Ralph Gasser
 * @version 1.1.0
 */
open class BasicJudgementValidator(knownCorrectRanges: Collection<ItemRange> = emptyList(), knownWrongRanges: Collection<ItemRange> = emptyList()): SubmissionValidator, JudgementValidator {

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

    /** Internal queue that keeps track of all the [DbAnswerSet]s in need of judgement. */
    private val queue: Queue<DbAnswerSet> = LinkedList()

    /** Internal queue that keeps track of all the [DbAnswerSet]s in need of judgement. */
    private val queuedItemRanges: MutableMap<ItemRange, MutableList<DbAnswerSet>> = HashMap()

    /** Internal map of all [DbAnswerSet]s that have been retrieved by a judge and are pending a verdict. */
    private val waiting = HashMap<String, DbAnswerSet>()

    /** Helper structure to keep track when a request needs to be re-scheduled */
    private val timeouts = mutableListOf<Pair<Long, String>>()

    /** Internal map of already judged [DbSubmission]s, independent of their source. */
    private val cache: MutableMap<ItemRange, DbVerdictStatus> = ConcurrentHashMap()

    init {
        knownCorrectRanges.forEach { cache[it] = DbVerdictStatus.CORRECT }
        knownWrongRanges.forEach { cache[it] = DbVerdictStatus.WRONG }
    }

    private fun checkTimeOuts() = updateLock.write {
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

    /** Returns the number of [DbSubmission]s that are currently pending a judgement. */
    override val pending: Int
        get() = updateLock.read { this.queue.size + this.waiting.size }

    override val open: Int
        get() = updateLock.read {
            checkTimeOuts()
            return this.queue.size
        }

    override val hasOpen: Boolean
        get() = updateLock.read {
            checkTimeOuts()
            return this.queue.isNotEmpty()
        }

    /**
     * Enqueues a [DbSubmission] with the internal judgment queue and updates its [DbVerdictStatus]
     * to [DbVerdictStatus.INDETERMINATE].
     *
     * @param submission The [DbSubmission] to validate.
     */
    override fun validate(submission: DbSubmission) = this.updateLock.read {
        for (verdict in submission.verdicts.asSequence()) {
            //only validate submissions which are not already validated
            if (verdict.status != DbVerdictStatus.INDETERMINATE){
                continue
            }

            //check cache first
            val itemRange = ItemRange(submission.verdicts.first())
            val cachedStatus = this.cache[itemRange]
            if (cachedStatus != null) {
                verdict.status = cachedStatus
            } else if (itemRange !in queuedItemRanges.keys) {
                updateLock.write {
                    this.queue.offer(verdict)
                    verdict.status = DbVerdictStatus.INDETERMINATE
                    this.queuedItemRanges[itemRange] = mutableListOf(verdict)
                }
            } else {
                this.updateLock.write {
                    this.queuedItemRanges[itemRange]!!.add(verdict)
                }
            }
        }
    }

    /**
     * Retrieves and returns the next element that requires a verdict from this [JudgementValidator]'
     * internal queue. If such an element exists, then the [DbSubmission] is returned alongside a
     * unique token, that can be used to update the [DbSubmission]'s [DbVerdictStatus].
     *
     * @return Optional [Pair] containing a string token and the [DbSubmission] that should be judged.
     */
    override fun next(queue: String): Pair<String, DbAnswerSet>? = updateLock.write {
        checkTimeOuts()
        val next = this.queue.poll()
        return if (next != null) {
            val token = UUID.randomUUID().toString()
            this.waiting[token] = next
            this.timeouts.add((System.currentTimeMillis() + judgementTimeout) to token)
            AuditLogger.prepareJudgement(next, this, token)
            Pair(token, next)
        } else {
            null
        }
    }

    /**
     * Places a verdict for the [DbSubmission] identified by the given token.
     *
     * @param token The token used to identify the [DbSubmission].
     * @param verdict The verdict of the judge.
     */
    override fun judge(token: String, verdict: DbVerdictStatus) {
        processSubmission(token, verdict).status = verdict
    }

    /**
     *
     */
    fun processSubmission(token: String, status: DbVerdictStatus) : DbAnswerSet = this.updateLock.write {
        val verdict = this.waiting[token]
            ?: throw JudgementTimeoutException("This JudgementValidator does not contain a submission for the token '$token'.") //submission with token not found TODO: this should be logged
        val itemRange = ItemRange(verdict)

        //add to cache
        this.cache[itemRange] = status

        //remove from waiting map
        this.waiting.remove(token)

        //remove from queue set
        val otherSubmissions = this.queuedItemRanges.remove(itemRange)
        otherSubmissions?.forEach { it.status = status }

        return@write verdict
    }

    /**
     * Clears this [JudgementValidator] and all the associated queues and maps.
     */
    fun clear() = this.updateLock.write {
        this.waiting.clear()
        this.queue.clear()
    }
}
