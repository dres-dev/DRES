package dres.data.model.competition

data class Task(val id: Long, val name: String, val description: TasksDescription, val type: TaskType, val novice: Boolean)