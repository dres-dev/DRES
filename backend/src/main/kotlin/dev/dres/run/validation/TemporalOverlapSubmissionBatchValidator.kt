package dev.dres.run.validation

import dev.dres.data.model.submissions.Submission
import dev.dres.data.model.submissions.SubmissionStatus
import dev.dres.data.model.submissions.aspects.TemporalAspect
import dev.dres.data.model.submissions.batch.ResultBatch
import dev.dres.run.validation.interfaces.SubmissionBatchValidator

/**
 * A [SubmissionBatchValidator] class that checks, if a submission is correct based on the target segment and the
 * temporal overlap of the [Submission] with the provided [TransientMediaSegment].
 *
 * @author Luca Rossetto & Ralph Gasser
 * @version 1.1.0
 */
class TemporalOverlapSubmissionBatchValidator(private val targetSegment: TransientMediaSegment) : SubmissionBatchValidator {

    override fun validate(batch: ResultBatch<*>) {
        batch.results.forEach {
            if (it is TemporalAspect){
                it.status = when {
                    it.start > it.end -> SubmissionStatus.WRONG
                    it.item != targetSegment.first -> SubmissionStatus.WRONG
                    else -> {
                        val outer =
                            this.targetSegment.second.toMilliseconds()
                        if ((outer.first <= it.start && outer.second >= it.start) || (outer.first <= it.end && outer.second >= it.end)) {
                            SubmissionStatus.CORRECT
                        } else {
                            SubmissionStatus.WRONG
                        }
                    }
                }
            } else {
                it.status = SubmissionStatus.WRONG
            }
        }
    }
}