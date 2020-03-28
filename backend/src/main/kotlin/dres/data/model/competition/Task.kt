package dres.data.model.competition

import dres.data.model.Entity
import kotlinx.serialization.Serializable

@Serializable
data class Task(override var id: Long, val name: String, val type: TaskType, val taskGroup: String, val description: TaskDescription) : Entity //TODO maybe normalise taskGroup into its own entity?