package dev.dres.run.validation

import dev.dres.data.model.submissions.Submission
import dev.dres.data.model.submissions.SubmissionStatus
import dev.dres.run.validation.interfaces.SubmissionValidator

class ChainedSubmissionValidator(private val firstValidator: SubmissionValidator, private val continueStates: Set<SubmissionStatus>, private val secondValidator: SubmissionValidator) : SubmissionValidator {

    companion object{
        fun of(continueStates: Set<SubmissionStatus>, vararg validator: SubmissionValidator) : ChainedSubmissionValidator {
            return when {
                validator.size < 2 -> throw IllegalArgumentException("Chain needs at least two validators")
                validator.size == 2 -> ChainedSubmissionValidator(validator[0], continueStates, validator[1])
                else -> ChainedSubmissionValidator(validator[0], continueStates, of(continueStates, *validator.sliceArray(1..(validator.size))))
            }
        }
    }

    init {
        require(!firstValidator.deferring) {"first validator cannot defer validation"}
    }

    override fun validate(submission: Submission) {
        firstValidator.validate(submission)
        if (continueStates.contains(submission.status)){
            secondValidator.validate(submission)
        }
    }

    override val deferring: Boolean
        get() = secondValidator.deferring
}