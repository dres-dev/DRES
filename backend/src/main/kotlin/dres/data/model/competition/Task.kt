package dres.data.model.competition

import dres.data.model.Entity

data class Task(override var id: Long, val name: String, val type: TaskType, val novice: Boolean, val description: TaskDescription) : Entity