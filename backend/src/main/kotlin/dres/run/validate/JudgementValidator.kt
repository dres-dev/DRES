package dres.run.validate

import dres.data.model.competition.TaskDescription
import dres.data.model.run.Submission
import dres.data.model.run.SubmissionStatus
import kotlinx.coroutines.Deferred


class JudgementValidator<S : Submission, T: TaskDescription>(private val queue: JudgementQueue<S, T>): SubmissionValidator<S, T> {
    override fun validate(submission: S, task: T): Deferred<SubmissionStatus> = queue.enqueue(submission, task)
}