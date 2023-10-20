package dev.dres.run.validation

import dev.dres.data.model.media.MediaItem
import dev.dres.data.model.media.MediaItemId
import dev.dres.data.model.submissions.*
import dev.dres.run.validation.interfaces.AnswerSetValidator
import kotlinx.dnq.query.iterator

/**
 * A [AnswerSetValidator] that checks if the items specified by a [Submission] match the items in the provided set.
 *
 * @author Luca Rossetto
 * @author Ralph Gasser
 * @version 2.0.0
 */
class MediaItemsAnswerSetValidator(items: Set<MediaItem>) : AnswerSetValidator {

    /** This type of [AnswerSetValidator] can be executed directly.*/
    override val deferring = false

    /** List of [MediaItemId]s that are considered valid. */
    private val itemIds = items.map { it.mediaItemId }

    /**
     * Validates the [DbAnswerSet] and updates its [DbVerdictStatus].
     *
     * Usually requires an ongoing transaction.
     *
     * @param answerSet The [DbAnswerSet] to validate.
     */
    override fun validate(answerSet: DbAnswerSet) {
        /* Basically, we assume that the DBAnswerSet is wrong. */
        answerSet.status = DbVerdictStatus.WRONG

        /* Now we check all the answers. */
        for (answer in answerSet.answers) {
            /* Perform sanity checks. */
            val item = answer.item
            if (answer.type != DbAnswerType.ITEM || item == null) {
                answerSet.status = DbVerdictStatus.WRONG
                return
            }

            /* Perform item validation. */
            if (item.id !in this.itemIds) {
                return
            }
        }

        /* If code reaches this point, the [DbAnswerSet] is correct. */
        answerSet.status = DbVerdictStatus.CORRECT
    }
}