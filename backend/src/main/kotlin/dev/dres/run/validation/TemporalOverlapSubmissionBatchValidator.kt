package dev.dres.run.validation

import dev.dres.data.model.competition.VideoSegment
import dev.dres.data.model.submissions.SubmissionStatus
import dev.dres.data.model.submissions.aspects.TemporalAspect
import dev.dres.data.model.submissions.batch.ResultBatch
import dev.dres.run.validation.interfaces.SubmissionBatchValidator
import dev.dres.utilities.TimeUtil

class TemporalOverlapSubmissionBatchValidator(private val targetSegment: VideoSegment) : SubmissionBatchValidator {

    override fun validate(batch: ResultBatch<*>) {

        batch.results.forEach {
            if (it is TemporalAspect){
                it.status = when {
                    it.start > it.end -> SubmissionStatus.WRONG
                    it.item != targetSegment.item -> SubmissionStatus.WRONG
                    else -> {
                        val outer =
                            TimeUtil.toMilliseconds(this.targetSegment.temporalRange, this.targetSegment.item.fps)
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