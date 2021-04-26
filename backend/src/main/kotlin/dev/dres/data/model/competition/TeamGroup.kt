package dev.dres.data.model.competition

data class TeamGroup(
    val uid: TeamGroupId,
    val name: String,
    val teams: List<Team>,
    val aggregation: TeamGroupAggregation
) {
    private val aggregator by lazy { aggregation.aggregationFactory(teams) }

    fun aggregate(teamScores: Map<TeamId, Double>) = aggregator.aggregate(teamScores)

}

abstract class TeamAggregator(teams: List<Team>) {

    private val teamIds = teams.map { it.uid }.toSet()

    internal abstract fun computeAggregation(teamScores: Map<TeamId, Double>): Double
    fun aggregate(teamScores: Map<TeamId, Double>) = computeAggregation(teamScores.filter { it.key in teamIds })

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

enum class TeamGroupAggregation(val aggregationFactory: (List<Team>) -> TeamAggregator) {
    MAX(::MaxTeamAggregator),
    MIN(::MinTeamAggregator),
    MEAN(::MeanTeamAggregator),
    LAST(::LastTeamAggregator)
}