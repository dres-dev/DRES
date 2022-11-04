package dev.dres.data.model.submissions.batch

/**
 *
 * @author Luca Rossetto
 * @version 1.0.0
 */
data class BaseSubmissionBatch(
    override val uid: EvaluationId,
    override val teamId: EvaluationId,
    override val memberId: EvaluationId,
    override val results: Collection<ResultBatch<BaseResultBatchElement>>
) : SubmissionBatch<ResultBatch<BaseResultBatchElement>>
