package dev.dres.run.score.interfaces

import dev.dres.data.model.submissions.batch.ResultBatch

interface ResultBatchTaskScorer : TaskScorer {

    fun computeScores(batch: ResultBatch<*>) : Double

}