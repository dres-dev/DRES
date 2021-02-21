package dev.dres.run

import dev.dres.data.model.run.BaseSubmissionBatch
import dev.dres.data.model.run.NonInteractiveTask

interface NonInteractiveRunManager : RunManager {

    fun addSubmissionBatch(batch: BaseSubmissionBatch<*>)

    override fun tasks(): List<NonInteractiveTask>

}