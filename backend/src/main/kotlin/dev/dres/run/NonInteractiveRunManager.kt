package dev.dres.run

import dev.dres.data.model.run.NonInteractiveTask
import dev.dres.data.model.run.SubmissionBatch

interface NonInteractiveRunManager : RunManager {

    fun addSubmissionBatch(batch: SubmissionBatch<*>)

    override fun tasks(): List<NonInteractiveTask>

}