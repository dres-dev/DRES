package dev.dres.run.validation.interfaces

import dev.dres.data.model.run.ResultBatch

interface SubmissionBatchValidator {

    fun validate(batch: ResultBatch<*>)

}