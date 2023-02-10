package dev.dres.run.validation

import dev.dres.data.model.media.DbMediaItem
import dev.dres.data.model.submissions.DbSubmission
import dev.dres.data.model.submissions.DbVerdictStatus
import dev.dres.data.model.submissions.Submission
import dev.dres.data.model.submissions.VerdictStatus
import dev.dres.run.validation.interfaces.SubmissionValidator
import kotlinx.dnq.query.asSequence

/**
 * A [SubmissionValidator] that checks if the items specified by a [DbSubmission] match the items in the provided set.
 *
 * @author Luca Rossetto
 * @version 1.0.1
 */
class MediaItemsSubmissionValidator(private val items : Set<DbMediaItem>) : SubmissionValidator {

    /** This type of [SubmissionValidator] can be executed directly.*/
    override val deferring = false

    /**
     * Performs the validation.
     *
     * @param submission The [DbSubmission] to validate.
     */
    override fun validate(submission: Submission) {
        submission.answerSets().forEach { answerSet ->

            if (answerSet.answers().any {  it.item == null || it.item !in this.items} ) {
                answerSet.status(VerdictStatus.WRONG)
            } else {
                answerSet.status(VerdictStatus.CORRECT)
            }
        }
    }
}