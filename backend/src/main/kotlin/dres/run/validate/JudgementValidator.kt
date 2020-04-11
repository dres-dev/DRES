package dres.run.validate

import dres.data.model.competition.TaskDescription
import dres.data.model.run.Submission
import dres.data.model.run.SubmissionStatus


class JudgementValidator<S : Submission, T: TaskDescription>(private val queue: JudgementQueue<S, T>): SubmissionValidator<S, T> {
    override suspend fun validate(submission: S, task: T): SubmissionStatus = queue.enqueue(submission, task).await()
}