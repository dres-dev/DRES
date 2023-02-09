package dev.dres.run.validation

import dev.dres.data.model.submissions.DbSubmission
import dev.dres.data.model.submissions.DbVerdictStatus
import dev.dres.data.model.submissions.Submission
import dev.dres.data.model.submissions.VerdictStatus
import dev.dres.run.validation.interfaces.SubmissionValidator
import kotlinx.dnq.query.asSequence

/**
 * A [SubmissionValidator] class that allows for the combination of two [SubmissionValidator]s.
 *
 * @author Luca Rossetto
 * @author Ralph Gasser
 * @version 1.1.0
 */
class ChainedSubmissionValidator(private val firstValidator: SubmissionValidator, private val continueStates: Set<VerdictStatus>, private val secondValidator: SubmissionValidator) : SubmissionValidator {

    companion object{
        fun of(continueStates: Set<VerdictStatus>, vararg validator: SubmissionValidator) : ChainedSubmissionValidator {
            return when {
                validator.size < 2 -> throw IllegalArgumentException("Chain needs at least two validators")
                validator.size == 2 -> ChainedSubmissionValidator(validator[0], continueStates, validator[1])
                else -> ChainedSubmissionValidator(validator[0], continueStates, of(continueStates, *validator.sliceArray(1..(validator.size))))
            }
        }
    }

    override val deferring: Boolean
        get() = this.secondValidator.deferring

    init {
        require(!this.firstValidator.deferring) {"First validator cannot be a deferring validation."}
    }

    /**
     * Validates a [DbSubmission] based on two [SubmissionValidator]s.
     *
     * @param submission The [DbSubmission] to validate.
     */
    override fun validate(submission: Submission) {
        this.firstValidator.validate(submission)
        if (submission.answerSets().any { this.continueStates.contains(it.status()) }) {
            this.secondValidator.validate(submission)
        }
    }
}