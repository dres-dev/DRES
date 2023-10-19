package dev.dres.data.model.template.team

/**
 * Implementation of different [DbTeamAggregator]s.
 *
 * @author Luca Rossetto
 * @version 1.0.0
 */
sealed class TeamAggregatorImpl constructor(private val teamIds: Set<TeamId>) {

    var lastValue: Double = 0.0
        private set

    internal abstract fun computeAggregation(teamScores: Map<TeamId, Double>): Double

    @Synchronized
    fun aggregate(teamScores: Map<TeamId, Double>) : Double{
        this.lastValue = computeAggregation(teamScores.filter { it.key in teamIds })
        return lastValue
    }

    class Max(teamIds: Set<TeamId>) : TeamAggregatorImpl(teamIds) {
        override fun computeAggregation(teamScores: Map<TeamId, Double>): Double =
            teamScores.map { it.value }.maxOrNull() ?: 0.0
    }

    class Min(teamIds: Set<TeamId>) : TeamAggregatorImpl(teamIds) {
        override fun computeAggregation(teamScores: Map<TeamId, Double>): Double =
            teamScores.map { it.value }.minOrNull() ?: 0.0
    }

    class Mean(teamIds: Set<TeamId>) : TeamAggregatorImpl(teamIds) {
        override fun computeAggregation(teamScores: Map<TeamId, Double>): Double =
            if (teamScores.isEmpty()) 0.0 else teamScores.map { it.value }.sum() / teamScores.size
    }

    class Last(teamIds: Set<TeamId>) : TeamAggregatorImpl(teamIds) {
        private val lastScores = mutableListOf<Pair<TeamId, Double>>()
        override fun computeAggregation(teamScores: Map<TeamId, Double>): Double {
            teamScores.forEach{
                if (lastScores.find { p -> p.first == it.key }?.second != it.value) {
                    lastScores.removeIf { p -> p.first == it.key }
                    lastScores.add(it.key to it.value)
                }
            }
            return lastScores.lastOrNull()?.second ?: 0.0
        }
    }
}






