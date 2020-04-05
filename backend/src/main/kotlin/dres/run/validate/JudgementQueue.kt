package dres.run.validate

import dres.data.model.competition.TaskDescription
import dres.data.model.run.Submission
import dres.data.model.run.SubmissionStatus
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger

class JudgementQueue<S : Submission, T: TaskDescription> {

    private val minExclusiveTime = 1000 * 60

    private val openQueue: ConcurrentLinkedQueue<JudgementQueueElement> = ConcurrentLinkedQueue()
    private val waitingQueue: ConcurrentLinkedQueue<Pair<Long, JudgementQueueElement>> = ConcurrentLinkedQueue()

    private val counter = AtomicInteger()

    /**
     * Adds an element to be judged
     */
    fun enqueue(submission: S, taskDescription: T): Deferred<SubmissionStatus> {

        val jqe = JudgementQueueElement(submission, taskDescription)
        openQueue.add(jqe)
        return jqe.status

    }

    /**
     * returns the next element awaiting judgement
     */
    fun next(): JudgementQueueElement? {
        if (openQueue.isNotEmpty()){
            val jqe = openQueue.poll()
            waitingQueue.add((System.currentTimeMillis() + minExclusiveTime) to jqe)
            return jqe
        }
        if ((waitingQueue.peek()?.first ?: Long.MAX_VALUE) < System.currentTimeMillis()){
            val jqe = waitingQueue.poll().second
            waitingQueue.add((System.currentTimeMillis() + minExclusiveTime) to jqe)
            return jqe
        }
        return null
    }

    fun judge(submission: S, taskDescription: T, status: SubmissionStatus) {
        val jqe = waitingQueue.find { it.second.submission == submission && it.second.taskDescription == taskDescription }
        if (jqe == null){
            //no matching element found
            //TODO log somewhere?
            return
        }
        jqe.second.judge(status)
        waitingQueue.remove(jqe)
    }

    fun judge(id: Int, status: SubmissionStatus) {
        val jqe = waitingQueue.find { it.second.id == id }
        if (jqe == null){
            //no matching element found
            //TODO log somewhere?
            return
        }
        jqe.second.judge(status)
        waitingQueue.remove(jqe)
    }

    inner class JudgementQueueElement(val submission: S, val taskDescription: T){

        internal val status = CompletableDeferred<SubmissionStatus>()
        val id = counter.incrementAndGet()

        internal fun judge(status: SubmissionStatus) {
            this.status.complete(status)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as JudgementQueue<*, *>.JudgementQueueElement

            if (submission != other.submission) return false
            if (taskDescription != other.taskDescription) return false

            return true
        }

        override fun hashCode(): Int {
            var result = submission.hashCode()
            result = 31 * result + taskDescription.hashCode()
            return result
        }
    }

}