package dev.dres.run.validation.interfaces

import dev.dres.data.model.submissions.batch.ResultBatch

interface SubmissionBatchValidator {
    fun validate(batch: ResultBatch<*>)
}