package dev.dres.run.score.scorer

import dev.dres.data.model.submissions.DbSubmission
import dev.dres.data.model.template.team.TeamId
import dev.dres.run.score.TaskContext

class CachingTaskScorer(private val innerScorer: TaskScorer) : TaskScorer {

    private var latest: Map<TeamId, Double> = emptyMap()

    override fun computeScores(submissions: Sequence<DbSubmission>, context: TaskContext): Map<TeamId, Double> {
        latest = innerScorer.computeScores(submissions, context)
        return latest
    }

    fun scores(): List<ScoreEntry> = latest.map { ScoreEntry(it.key, null, it.value) }

    fun teamScoreMap() : Map<TeamId, Double> = latest


}