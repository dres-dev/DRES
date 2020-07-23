package dres.api.rest.types

import dres.data.model.competition.interfaces.TaskDescription


class RestTaskDescription(
        val name: String,
        val taskGroup: String,
        val taskType: String,
        val duration: Long,
        val defaultMediaCollectionId: Long,
        val components: List<RestTaskDescriptionComponent>,
        val target: RestTaskDescriptionTarget) {

    constructor(description: TaskDescription) : this(
            description.name,
            description.taskGroup.name,
            description.taskType.name,
            description.duration,
            description.defaultMediaCollectionId,
            description.components.map { RestTaskDescriptionComponent(it) },
            RestTaskDescriptionTarget(description.target)
    )
}