package dev.dres.data.model.competition

data class TeamGroup(
    val uid: TeamGroupId,
    val name: String,
    val teams: List<Team>,
    val aggregation: TeamGroupAggregation
) {
    fun newAggregator() : TeamAggregator = aggregation.aggregationFactory(teams)
}

abstract class TeamAggregator internal constructor(teams: List<Team>) {

    private val teamIds = teams.map { it.uid }.toSet()

    var lastValue: Double = 0.0
        private set

    internal abstract fun computeAggregation(teamScores: Map<TeamId, Double>): Double

    @Synchronized
    fun aggregate(teamScores: Map<TeamId, Double>) : Double{
        this.lastValue = computeAggregation(teamScores.filter { it.key in teamIds })
        return lastValue
    }

}

class MaxTeamAggregator(teams: List<Team>) : TeamAggregator(teams) {
    override fun computeAggregation(teamScores: Map<TeamId, Double>): Double =
        teamScores.map { it.value }.maxOrNull() ?: 0.0
}

class MinTeamAggregator(teams: List<Team>) : TeamAggregator(teams) {
    override fun computeAggregation(teamScores: Map<TeamId, Double>): Double =
        teamScores.map { it.value }.minOrNull() ?: 0.0
}

class MeanTeamAggregator(teams: List<Team>) : TeamAggregator(teams) {
    override fun computeAggregation(teamScores: Map<TeamId, Double>): Double =
        if (teamScores.isEmpty()) 0.0 else teamScores.map { it.value }.sum() / teamScores.size
}

class LastTeamAggregator(teams: List<Team>) : TeamAggregator(teams) {

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

enum class TeamGroupAggregation(internal val aggregationFactory: (List<Team>) -> TeamAggregator) {
    MAX(::MaxTeamAggregator),
    MIN(::MinTeamAggregator),
    MEAN(::MeanTeamAggregator),
    LAST(::LastTeamAggregator)
}