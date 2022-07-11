package dev.dres.run.validation.judged

import dev.dres.data.model.submissions.Submission
import dev.dres.data.model.submissions.SubmissionStatus
import dev.dres.run.audit.AuditLogger
import dev.dres.run.exceptions.JudgementTimeoutException
import dev.dres.run.validation.interfaces.JudgementValidator
import dev.dres.run.validation.interfaces.SubmissionJudgementValidator
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
 * @author Luca Rossetto & Ralph Gasser
 * @version 1.0
 */
open class BasicJudgementValidator(knownCorrectRanges: Collection<ItemRange> = emptyList(), knownWrongRanges: Collection<ItemRange> = emptyList()):
    SubmissionJudgementValidator {

    companion object {
        private val counter = AtomicInteger()
        private const val judgementTimeout = 60_000 //ms until a request is re-scheduled
    }

    override val id = "bjv${counter.incrementAndGet()}"

    private val updateLock = ReentrantReadWriteLock()

    /** Internal queue that keeps track of all the [Submission]s in need of a verdict. */
    private val queue: Queue<Submission> = LinkedList()

    private val queuedItemRanges: MutableMap<ItemRange, MutableList<Submission>> = HashMap()

    /** Internal map of all [Submission]s that have been retrieved by a judge and are pending a verdict. */
    private val waiting = HashMap<String, Submission>()

    /** Helper structure to keep track when a request needs to be re-scheduled */
    private val timeouts = mutableListOf<Pair<Long, String>>()

    /** Internal map of already judged [Submission]s, independent of their source. */
    private val cache: MutableMap<ItemRange, SubmissionStatus> = ConcurrentHashMap()

    init {
        knownCorrectRanges.forEach { cache[it] = SubmissionStatus.CORRECT }
        knownWrongRanges.forEach { cache[it] = SubmissionStatus.WRONG }
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

    /** Returns the number of [Submission]s that are currently pending a judgement. */
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
     * Enqueues a [Submission] with the internal judgment queue and updates its [SubmissionStatus]
     * to [SubmissionStatus.INDETERMINATE].
     *
     * @param submission The [Submission] to validate.
     */
    override fun validate(submission: Submission) = updateLock.read {

        //only validate submissions which are not already validated
        if (submission.status != SubmissionStatus.INDETERMINATE){
            return@read
        }

        //check cache first
        val itemRange = ItemRange(submission)
        val cachedStatus = this.cache[itemRange]
        if (cachedStatus != null) {
            submission.status = cachedStatus
        } else if (itemRange !in queuedItemRanges.keys) {
            updateLock.write {
                this.queue.offer(submission)

                submission.status = SubmissionStatus.INDETERMINATE
                queuedItemRanges[itemRange] = mutableListOf(submission)
            }
        } else {
            updateLock.write {
                queuedItemRanges[itemRange]!!.add(submission)
            }
        }
    }

    /**
     * Retrieves and returns the next element that requires a verdict from this [JudgementValidator]'
     * internal queue. If such an element exists, then the [Submission] is returned alongside a
     * unique token, that can be used to update the [Submission]'s [SubmissionStatus].
     *
     * @return Optional [Pair] containing a string token and the [Submission] that should be judged.
     */
    override fun next(queue: String): Pair<String, Submission>? = updateLock.write {
        checkTimeOuts()
        val next = this.queue.poll()
        return if (next != null) {
            val token = UUID.randomUUID().toString()
            this.waiting[token] = next
            this.timeouts.add((System.currentTimeMillis() + judgementTimeout) to token)
            AuditLogger.prepareJudgement(this.id, token, next)
            Pair(token, next)
        } else {
            null
        }
    }

    /**
     * Places a verdict for the [Submission] identified by the given token.
     *
     * @param token The token used to identify the [Submission].
     * @param verdict The verdict of the judge.
     */
    override fun judge(token: String, verdict: SubmissionStatus) {
        processSubmission(token, verdict)?.status = verdict
    }

    internal fun processSubmission(token: String, verdict: SubmissionStatus) : Submission? = updateLock.write {
        val submission = this.waiting[token] ?: throw JudgementTimeoutException("This JudgementValidator does not contain a submission for the token '$token'.") //submission with token not found TODO: this should be logged

        val itemRange = ItemRange(submission)

        //add to cache
        this.cache[itemRange] = verdict

        //remove from waiting map
        this.waiting.remove(token)

        //remove from queue set
        val otherSubmissions = this.queuedItemRanges.remove(itemRange)
        otherSubmissions?.forEach {
            it.status = verdict
        }

        return@write submission
    }

    /**
     * Clears this [JudgementValidator] and all the associated queues and maps.
     */
    fun clear() = updateLock.write {
        this.waiting.clear()
        this.queue.clear()
    }
}
