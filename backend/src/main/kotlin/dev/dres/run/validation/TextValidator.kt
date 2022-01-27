package dev.dres.run.validation

import dev.dres.data.model.submissions.Submission
import dev.dres.data.model.submissions.SubmissionStatus
import dev.dres.data.model.submissions.aspects.TextAspect
import dev.dres.run.validation.interfaces.SubmissionValidator

class TextValidator(targets: List<String>) : SubmissionValidator {

    private val regex = targets.map { it.toRegex() }

    override fun validate(submission: Submission) {

        if (submission !is TextAspect) {
            submission.status = SubmissionStatus.WRONG
            return
        }

        if (regex.any { it.matches(submission.text) }) {
            submission.status = SubmissionStatus.CORRECT
        } else {
            submission.status = SubmissionStatus.WRONG
        }
    }

    override val deferring = false
}