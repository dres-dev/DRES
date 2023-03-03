package dev.dres.run.validation

import dev.dres.data.model.submissions.AnswerSet
import dev.dres.data.model.submissions.DbSubmission
import dev.dres.data.model.submissions.VerdictStatus
import dev.dres.run.validation.interfaces.AnswerSetValidator

/**
 * A [AnswerSetValidator] class that allows for the combination of two [AnswerSetValidator]s.
 *
 * @author Luca Rossetto
 * @author Ralph Gasser
 * @version 1.1.0
 */
class ChainedAnswerSetValidator(private val firstValidator: AnswerSetValidator, private val continueStates: Set<VerdictStatus>, private val secondValidator: AnswerSetValidator) : AnswerSetValidator {

    companion object{
        fun of(continueStates: Set<VerdictStatus>, vararg validator: AnswerSetValidator) : ChainedAnswerSetValidator {
            return when {
                validator.size < 2 -> throw IllegalArgumentException("Chain needs at least two validators")
                validator.size == 2 -> ChainedAnswerSetValidator(validator[0], continueStates, validator[1])
                else -> ChainedAnswerSetValidator(validator[0], continueStates, of(continueStates, *validator.sliceArray(1..(validator.size))))
            }
        }
    }

    override val deferring: Boolean
        get() = this.secondValidator.deferring

    init {
        require(!this.firstValidator.deferring) {"First validator cannot be a deferring validation."}
    }

    /**
     * Validates a [DbSubmission] based on two [AnswerSetValidator]s.
     *
     * @param submission The [DbSubmission] to validate.
     */
    override fun validate(answerSet: AnswerSet) {
        this.firstValidator.validate(answerSet)
        if (this.continueStates.contains(answerSet.status())) {
            this.secondValidator.validate(answerSet)
        }
    }
}