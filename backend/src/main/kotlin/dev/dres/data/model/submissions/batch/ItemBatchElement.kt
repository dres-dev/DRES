package dev.dres.data.model.submissions.batch

import dev.dres.data.model.media.MediaItem
import dev.dres.data.model.submissions.SubmissionStatus

/**
 *
 * @author Luca Rossetto
 * @version 1.0.0
 */
data class ItemBatchElement(override val item: MediaItem): BaseResultBatchElement {
    override var status: SubmissionStatus = SubmissionStatus.INDETERMINATE
}