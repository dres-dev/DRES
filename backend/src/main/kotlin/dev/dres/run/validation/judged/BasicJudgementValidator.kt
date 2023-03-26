package dev.dres.run.validation.judged

import dev.dres.data.model.submissions.*
import dev.dres.run.RunExecutor
import dev.dres.run.audit.DbAuditLogger
import dev.dres.run.exceptions.JudgementTimeoutException
import dev.dres.run.validation.interfaces.JudgementValidator
import dev.dres.run.validation.interfaces.AnswerSetValidator
import kotlinx.dnq.query.eq
import kotlinx.dnq.query.filter
import kotlinx.dnq.query.firstOrNull
import kotlinx.dnq.query.query
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
open class BasicJudgementValidator(
    knownCorrectRanges: Collection<ItemRange> = emptyList(),
    knownWrongRanges: Collection<ItemRange> = emptyList()
) : AnswerSetValidator, JudgementValidator {

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
    private val queue: Queue<AnswerSetId> = LinkedList()

    /** Internal queue that keeps track of all the [DbAnswerSet]s in need of judgement. */
    private val queuedItemRanges: MutableMap<ItemRange, MutableList<AnswerSet>> = HashMap()

    /** Internal map of all [AnswerSetId]s that have been retrieved by a judge and are pending a verdict. */
    private val waiting = HashMap<String, AnswerSetId>()

    /** Helper structure to keep track when a request needs to be re-scheduled */
    private val timeouts = mutableListOf<Pair<Long, String>>()

    /** Internal map of already judged [DbSubmission]s, independent of their source. */
    private val cache: MutableMap<ItemRange, VerdictStatus> = ConcurrentHashMap()

    init {
        knownCorrectRanges.forEach { cache[it] = VerdictStatus.CORRECT }
        knownWrongRanges.forEach { cache[it] = VerdictStatus.WRONG }
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

    override fun validate(answerSet: AnswerSet) = this.updateLock.read {

        //only validate submissions which are not already validated
        if (answerSet.status() != VerdictStatus.INDETERMINATE) {
            return@read
        }

        //check cache first
        val itemRange = ItemRange(answerSet.answers().first()) //TODO reason about semantics
        val cachedStatus = this.cache[itemRange]
        if (cachedStatus != null) {
            answerSet.status(cachedStatus)
        } else if (itemRange !in queuedItemRanges.keys) {
            updateLock.write {
                this.queue.offer(answerSet.id)
                answerSet.status(VerdictStatus.INDETERMINATE)
                this.queuedItemRanges[itemRange] = mutableListOf(answerSet)
            }
        } else {
            this.updateLock.write {
                this.queuedItemRanges[itemRange]!!.add(answerSet)
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
        val nextAnswerSetId = this.queue.poll() ?: return@write null
        val next = DbAnswerSet.query(DbAnswerSet::id eq nextAnswerSetId).firstOrNull() ?: return@write null
        val token = UUID.randomUUID().toString()
        this.waiting[token] = nextAnswerSetId
        this.timeouts.add((System.currentTimeMillis() + judgementTimeout) to token)
        DbAuditLogger.prepareJudgement(next, this, token)
        Pair(token, next)

    }

    /**
     * Places a verdict for the [DbSubmission] identified by the given token.
     *
     * @param token The token used to identify the [DbSubmission].
     * @param verdict The verdict of the judge.
     */
    override fun judge(token: String, verdict: VerdictStatus) {
        processSubmission(token, verdict).status(verdict)
    }

    /**
     *
     */
    fun processSubmission(token: String, status: VerdictStatus): DbAnswerSet = this.updateLock.write {

        val nextAnswerSetId = this.waiting[token]
            ?: throw JudgementTimeoutException("This JudgementValidator does not contain a submission for the token '$token'.") //submission with token not found TODO: this should be logged

        val answerSet = DbAnswerSet.query(DbAnswerSet::id eq nextAnswerSetId).firstOrNull() ?: throw IllegalStateException("DbAnswerSet with id '$nextAnswerSetId' not found")

        val itemRange = ItemRange(answerSet.answers().first()) //TODO reason about semantics

        //add to cache
        this.cache[itemRange] = status

        //remove from waiting map
        this.waiting.remove(token)

        //remove from queue set
        val otherSubmissions = this.queuedItemRanges.remove(itemRange)
        otherSubmissions?.forEach { it.status(status) }

        //trigger score update
        RunExecutor.managerForId(answerSet.task.evaluation.evaluationId)?.reScore(answerSet.taskId)


        return@write answerSet
    }

    /**
     * Clears this [JudgementValidator] and all the associated queues and maps.
     */
    fun clear() = this.updateLock.write {
        this.waiting.clear()
        this.queue.clear()
    }
}
