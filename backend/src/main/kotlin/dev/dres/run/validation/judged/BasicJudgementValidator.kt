package dev.dres.run.validation.judged

import dev.dres.api.rest.types.evaluation.submission.ApiAnswerSet
import dev.dres.api.rest.types.evaluation.submission.ApiVerdictStatus
import dev.dres.api.rest.types.template.tasks.ApiTaskTemplate
import dev.dres.api.rest.types.template.tasks.ApiTaskType
import dev.dres.data.model.submissions.*
import dev.dres.run.audit.AuditLogger
import dev.dres.run.exceptions.JudgementTimeoutException
import dev.dres.run.validation.interfaces.JudgementValidator
import dev.dres.run.validation.interfaces.AnswerSetValidator
import jetbrains.exodus.database.TransientEntityStore
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
 * @author Loris Sauter
 *
 * @version 2.1.0
 */
open class BasicJudgementValidator(
    override val taskTemplate: ApiTaskTemplate,
    protected val store: TransientEntityStore,
    private val taskType: ApiTaskType,
    knownCorrectRanges: Collection<ItemRange> = emptyList(),
    knownWrongRanges: Collection<ItemRange> = emptyList(),
) : AnswerSetValidator, JudgementValidator {

    companion object {
        private val counter = AtomicInteger()
        private const val judgementTimeout = 60_000 //ms until a request is re-scheduled

        private const val defaultPriority = 0;

        /**
         * The key used to read the task type configuration order
         * The value expected under this key is LIFO, which results in LIFO ordering of the queue,
         * all other parameters or the absence of one results in the default FIFO behaviour.
         */
        private const val CONFIGURATION_ORDER_KEY = "JUDGEMENT.order"

        /**
         * The key used to read the task type configuration priority.
         * The value is expected to be a number. Higher number represents higher priority.
         * Consumers are expected to respect the {priority}
         */
        private const val CONFIGURATION_PRIORITY_KEY = "JUDGEMENT.priority"


    }

    /** The [BasicJudgementValidator]'s ID is simply an auto-incrementing number. */
    override val id = "bjv${counter.incrementAndGet()}"

    /** A [BasicJudgementValidator] is always deferring. */
    override val deferring: Boolean = true

    /** The priority of this [JudgementValidator], higher priorities are represent by a higher number and consumers are expected to respect this */
    override val priority = this.taskType.configuration[CONFIGURATION_PRIORITY_KEY]?.toInt() ?: defaultPriority

    /** Internal lock on relevant data structures. */
    private val updateLock = ReentrantReadWriteLock()

    /** Internal queue that keeps track of all the [AnswerSetId]s and associated [ItemRange]s that require judgement. */
    private val queue: Deque<Pair<AnswerSetId,ItemRange>> = LinkedList()

    /** Internal map of all [AnswerSetId]s and associated [ItemRange]s that have been retrieved by a judge and are pending a verdict. */
    private val waiting = HashMap<String, Pair<AnswerSetId,ItemRange>>()

    /** Internal queue that keeps track of all the pending [ItemRange]s in need of judgement. */
    private val queuedItemRanges: MutableMap<ItemRange, MutableList<AnswerSetId>> = HashMap()

    /** Helper structure to keep track when a request needs to be re-scheduled */
    private val timeouts = mutableListOf<Pair<Long, String>>()

    /** Internal map of known [ItemRange]s to associated [DbVerdictStatus]. */
    private val cache: MutableMap<ItemRange, DbVerdictStatus> = ConcurrentHashMap()

    /** Internal flag whether to use LIFO */
    private val lifo = when(this.taskType.configuration.getOrDefault(CONFIGURATION_ORDER_KEY, "fifo")) {
        "lifo" -> true
        else -> false
    }

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
     * @param answerSet The [DbAnswerSet] to validate.
     */
    override fun validate(answerSet: DbAnswerSet) = this.updateLock.read {
        this.store.transactional {
            //only validate submissions which are not already validated
            if (answerSet.status != DbVerdictStatus.INDETERMINATE) {
                return@transactional
            }

            //check cache first
            val itemRange = ItemRange(answerSet.answers.first()) //TODO reason about semantics
            val cachedStatus = this.cache[itemRange]
            if (cachedStatus != null) {
                answerSet.status = cachedStatus
            } else if (itemRange !in this.queuedItemRanges.keys) {
                this.updateLock.write {
                    if(this.lifo){
                        this.queue.offerFirst(answerSet.id to itemRange)
                    }else{
                        this.queue.offerLast(answerSet.id to itemRange)
                    }
                    this.queuedItemRanges[itemRange] = mutableListOf(answerSet.id)
                }
            } else {
                this.updateLock.write {
                    this.queuedItemRanges[itemRange]!!.add(answerSet.id)
                }
            }
        }
    }

    /**
     * Retrieves and returns the next element that requires a verdict from this [JudgementValidator]'s internal queue.
     *
     * If such an element exists, then the [ApiAnswerSet] is returned alongside a unique token, that can be used to update
     * the [ApiAnswerSet]'s [VerdictStatus].
     *
     * @return [Pair] containing a string token and the [ApiAnswerSet] that should be judged. Can be null!
     */
    override fun next(): Pair<String, ApiAnswerSet>? = this.updateLock.write {
        checkTimeOuts()
        val next = this.queue.poll() ?: return@write null
        return@write this.store.transactional(true) {
            val answerSet = DbAnswerSet.query(DbAnswerSet::id eq next.first).singleOrNull() ?: return@transactional null
            val token = UUID.randomUUID().toString()
            this.waiting[token] = next
            this.timeouts.add((System.currentTimeMillis() + judgementTimeout) to token)
            AuditLogger.prepareJudgement(answerSet.toApi(), this, token)
            token to answerSet.toApi()
        }
    }

    /**
     * Places a verdict for the [Submission] identified by the given token.
     *
     * @param token The token used to identify the [Submission].
     * @param verdict The verdict of the judge.
     */
    override fun judge(token: String, verdict: ApiVerdictStatus) {
        this.updateLock.write {
            this.judgeInternal(token, verdict)
        }
    }

    /**
     * Internal implementation for re-use of the judgement logic for re-use.
     *
     * @param token The token used to identify the [DbSubmission].
     * @param verdict The verdict of the judge.
     */
    protected fun judgeInternal(token: String, verdict: ApiVerdictStatus): AnswerSetId {
        val next = this.waiting.remove(token)
            ?: throw JudgementTimeoutException("This JudgementValidator does not contain a submission for the token '$token'.") //submission with token not found TODO: this should be logged

        /* Remove from queue set. */
        val otherSubmissions = this.queuedItemRanges.remove(next.second) ?: emptyList()
        this.store.transactional {
            val dbVerdict = verdict.toDb()
            for ((i, answerSetId) in (otherSubmissions + next.first).withIndex()) {
                val answerSet = DbAnswerSet.query(DbAnswerSet::id eq answerSetId).singleOrNull()
                if (answerSet != null) {
                    answerSet.status = dbVerdict
                    if (i == 0) {
                        this.cache[ItemRange(answerSet.answers.first())] = dbVerdict //TODO reason about semantics
                    }
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
