package dev.dres.run.score.interfaces

import dev.dres.data.model.competition.TeamId

interface TeamTaskScorer : TaskScorer {

    fun teamScoreMap() : Map<TeamId, Double>

}