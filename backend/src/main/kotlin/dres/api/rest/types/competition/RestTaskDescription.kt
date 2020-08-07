package dres.api.rest.types.competition

import dres.api.rest.types.competition.tasks.RestTaskDescriptionComponent
import dres.api.rest.types.competition.tasks.RestTaskDescriptionTarget
import dres.data.dbo.DAO
import dres.data.model.UID
import dres.data.model.basics.media.MediaItem
import dres.data.model.competition.*
import dres.data.model.competition.TaskDescription
import dres.utilities.extensions.UID

/**
 * The RESTful API equivalent for [TaskDescription].
 *
 * @see TaskDescription
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
        val target: RestTaskDescriptionTarget,
        val components: List<RestTaskDescriptionComponent>

) {

    companion object {
        /**
         * Generates a [RestTaskDescription] from a [TaskDescription] and returns it.
         *
         * @param task The [TaskDescription] to convert.
         */
        fun fromTask(task: TaskDescription) = RestTaskDescription(
            task.id.string,
            task.name,
            task.taskGroup.name,
            task.taskType.name,
            task.duration,
            task.mediaCollectionId.string,
            RestTaskDescriptionTarget.fromTarget(task.target),
            task.hints.map {
                RestTaskDescriptionComponent.fromComponent(it)
            }
        )
    }



    /**
     * Converts this [RestTaskDescription] to the corresponding [TaskDescription] and returns it.
     *
     * @param taskGroups List of [TaskGroup] used to perform lookups.
     * @param taskTypes List of [TaskType] used to perform lookups.
     * @param mediaItems [DAO] used to perform media item lookups.
     */
    fun toTaskDescription(taskGroups: List<TaskGroup>, taskTypes: List<TaskType>, mediaItems: DAO<MediaItem>) = TaskDescription(
        this.id.UID(),
        this.name,
        taskGroups.find { it.name == this.taskGroup }!!,
        taskTypes.find { it.name == this.taskType }!!,
        this.duration,
        this.mediaCollectionId.UID(),
        this.target.toTarget(mediaItems),
        this.components.map { it.toComponent(mediaItems) }
    )
}