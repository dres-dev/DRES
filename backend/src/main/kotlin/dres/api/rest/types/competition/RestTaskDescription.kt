package dres.api.rest.types.competition

import dres.api.rest.types.competition.tasks.RestTaskDescriptionComponent
import dres.api.rest.types.competition.tasks.RestTaskDescriptionTarget
import dres.data.model.UID
import dres.data.model.competition.TaskDescriptionTarget
import dres.data.model.competition.interfaces.TaskDescription

/**
 * The RESTful API equivalent for [TaskDescription].
 *
 * @author Luca Rossetto & Ralph Gasser
 * @version 1.0
 */
class RestTaskDescription(
        val id: String = UID().string,
        val name: String,
        val taskGroup: String,
        val taskType: String,
        val duration: Long,
        val mediaCollectionId: String,
        val components: List<RestTaskDescriptionComponent>,
        val target: RestTaskDescriptionTarget
) {

    constructor(description: TaskDescription) : this(
            description.id.string,
            description.name,
            description.taskGroup.name,
            description.taskType.name,
            description.duration,
            description.mediaCollectionId.string,
            description.components.map { RestTaskDescriptionComponent(it) },
            RestTaskDescriptionTarget.fromTarget(description.target)
    )
}