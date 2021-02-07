package dev.dres.run

import dev.dres.data.model.run.BaseSubmissionBatch

interface NonInteractiveRunManager : RunManager {

    fun addSubmissionBatch(batch: BaseSubmissionBatch<*>)

}