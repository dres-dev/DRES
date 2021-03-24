package dev.dres.data.model.submissions.batch

import dev.dres.data.model.UID

/**
 *
 * @author Luca Rossetto
 * @version 1.0.0
 */
data class TemporalSubmissionBatch(
    override val teamId: UID,
    override val memberId: UID,
    override val uid: UID,
    override val results: List<BaseResultBatch<TemporalBatchElement>>,
) : SubmissionBatch<ResultBatch<TemporalBatchElement>>