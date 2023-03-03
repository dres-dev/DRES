package dev.dres.run.validation

import dev.dres.data.model.media.MediaItem
import dev.dres.data.model.submissions.AnswerSet
import dev.dres.data.model.submissions.DbSubmission
import dev.dres.data.model.submissions.VerdictStatus
import dev.dres.run.validation.interfaces.AnswerSetValidator

/**
 * A [AnswerSetValidator] that checks if the items specified by a [Submission] match the items in the provided set.
 *
 * @author Luca Rossetto
 * @version 1.0.1
 */
class MediaItemsAnswerSetValidator(items: Set<MediaItem>) : AnswerSetValidator {

    /** This type of [AnswerSetValidator] can be executed directly.*/
    override val deferring = false

    private val itemIds = items.map { it.mediaItemId }

    override fun validate(answerSet: AnswerSet) {

        if (answerSet.answers().any { it.item == null || it.item!!.mediaItemId !in this.itemIds }) {
            answerSet.status(VerdictStatus.WRONG)
        } else {
            answerSet.status(VerdictStatus.CORRECT)
        }

    }
}