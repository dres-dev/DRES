package dres.data.model.competition

data class Competition(val id: Long, val name: String, val description: String?, val tasks: List<Task>, val teams: List<Team>)