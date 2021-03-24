package dev.dres.data.model.submissions.batch

import dev.dres.data.model.UID

/**
 *
 * @author Luca Rossetto
 * @version 1.0.0
 */
data class BaseSubmissionBatch(
    override val uid: UID,
    override val teamId: UID,
    override val memberId: UID,
    override val results: Collection<ResultBatch<BaseResultBatchElement>>
) : SubmissionBatch<ResultBatch<BaseResultBatchElement>>
