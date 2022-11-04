package dev.dres.data.model.submissions.batch

/**
 *
 * @author Luca Rossetto
 * @version 1.0.0
 */
data class TemporalSubmissionBatch(
    override val teamId: EvaluationId,
    override val memberId: EvaluationId,
    override val uid: EvaluationId,
    override val results: List<BaseResultBatch<TemporalBatchElement>>,
) : SubmissionBatch<ResultBatch<TemporalBatchElement>>