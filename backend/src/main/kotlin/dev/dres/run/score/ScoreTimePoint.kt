package dev.dres.run.score

import dev.dres.run.score.scoreboard.Score
import java.rmi.server.UID

/**
 * Container class to track Scores over time
 */
data class ScoreTimePoint(val name: String, val team: String, val score: Double, val timestamp: Long = System.currentTimeMillis()) {
    constructor(name: String, score: Score) : this(name, score.teamId, score.score)
}