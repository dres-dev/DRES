package dres.run.validation.judged

import dres.data.model.basics.media.MediaItem
import dres.data.model.run.Submission
import dres.data.model.run.SubmissionStatus
import dres.run.validation.interfaces.JudgementValidator
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.collections.HashMap

/**
 * A validator class that checks, if a submission is correct based on a manual judgement by a user.
 *
 * TODO: Track these in the RunExecutor
 *
 * @author Luca Rossetto & Ralph Gasser
 * @version 1.0
 */
class BasicJudgementValidator(override val callback: ((Submission) -> Unit)? = null): JudgementValidator { //TODO better name

    companion object {
        private val counter = AtomicInteger()
        private val judgementTimeout = 30_000 //ms until a request is re-scheduled
    }

    override val id = "bjv${counter.incrementAndGet()}"

    /** Helper class to store submission information independent of source */
    private data class ItemRange(val item: MediaItem, val start: Long, val end: Long){
        constructor(submission: Submission): this(submission.item, submission.start ?: 0, submission.end ?: 0)
    }

    /** Internal queue that keeps track of all the [Submission]s in need of a verdict. */
    private val queue: Queue<Submission> = LinkedList()

    /** Internal map of all [Submission]s that have been retrieved by a judge and are pending a verdict. */
    private val waiting = HashMap<String, Submission>()

    /** Helper structure to keep track when a request needs to be re-scheduled */
    private val timeouts = mutableListOf<Pair<Long, String>>()

    /** Internal map of already judged [Submission]s, independent of their source. */
    private val cache: MutableMap<ItemRange, SubmissionStatus> = ConcurrentHashMap()

    @Synchronized
    fun checkTimeOuts() {
        val now = System.currentTimeMillis()
        var due = timeouts.filter { it.first <= now }
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
        @Synchronized
        get() = this.queue.size + this.waiting.size

    override val open: Int
        @Synchronized
        get() {
            checkTimeOuts()
            return this.queue.size
        }

    override val hasOpen: Boolean
        @Synchronized
        get(){
            checkTimeOuts()
            return this.queue.isNotEmpty()
        }

    /**
     * Enqueues a [Submission] with the internal judgment queue and updates its [SubmissionStatus]
     * to [SubmissionStatus.INDETERMINATE].
     *
     * @param submission The [Submission] to validate.
     * @return [SubmissionStatus] of the [Submission]
     */
    @Synchronized
    override fun validate(submission: Submission) {
        //check cache first
        val cachedStatus = this.cache[ItemRange(submission)]
        if (cachedStatus != null) {
            submission.status = cachedStatus
        } else {
            this.queue.offer(submission)
            submission.status = SubmissionStatus.INDETERMINATE
        }
    }

    /**
     * Retrieves and returns the next element that requires a verdict from this [JudgementValidator]'
     * internal queue. If such an element exists, then the [Submission] is returned alongside a
     * unique token, that can be used to update the [Submission]'s [SubmissionStatus].
     *
     * @return Optional [Pair] containing a string token and the [Submission] that should be judged.
     */
    @Synchronized
    override fun next(queue: String): Pair<String, Submission>? {
        checkTimeOuts()
        val next = this.queue.poll()
        return if (next != null) {
            val token = UUID.randomUUID().toString()
            this.waiting[token] = next
            this.timeouts.add((System.currentTimeMillis() + judgementTimeout) to token)
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
    @Synchronized
    override fun judge(token: String, verdict: SubmissionStatus) {
        require(this.waiting.containsKey(token)) { "This JudgementValidator does not contain a submission for the token '$token'." }
        val submission = this.waiting[token] ?: return //submission with token not found TODO: this should be logged
        submission.status = verdict

        //add to cache
        this.cache[ItemRange(submission)] = verdict

        /* Invoke callback if any. */
        this.callback?.invoke(submission)

        //remove from waiting map
        this.waiting.remove(token)
    }

    /**
     * Clears this [JudgementValidator] and all the associated queues and maps.
     */
    @Synchronized
    fun clear() {
        this.waiting.clear()
        this.queue.clear()
    }
}