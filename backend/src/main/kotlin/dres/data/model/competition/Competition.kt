package dres.data.model.competition

import dres.data.model.Entity

data class Competition(override var id: Long, val name: String, val description: String?, val tasks: List<Task>, val teams: List<Team>) : Entity