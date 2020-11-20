package dev.dres.run.score

import dev.dres.run.score.scoreboard.Score

/**
 * Container class to track Scores over time
 */
data class ScoreTimePoint(val name: String, val team: Int, val score: Double, val timestamp: Long = System.currentTimeMillis()) {
    constructor(name: String, score: Score) : this(name, score.teamId, score.score)
}