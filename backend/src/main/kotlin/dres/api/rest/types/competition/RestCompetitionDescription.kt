package dres.api.rest.types.competition

import dres.data.dbo.DAO
import dres.data.model.basics.media.MediaItem
import dres.data.model.competition.CompetitionDescription
import dres.data.model.competition.TaskGroup
import dres.data.model.competition.TaskType
import dres.data.model.competition.Team
import dres.utilities.extensions.UID

/**
 * The RESTful API equivalent for [CompetitionDescription].
 *
 * @see CompetitionDescription
 * @author Luca Rossetto & Ralph Gasser
 * @version 1.0
 *
 */
data class RestCompetitionDescription(
        val id: String,
        val name: String,
        val description: String?,
        val taskTypes: List<TaskType>,
        val taskGroups: List<TaskGroup>,
        val tasks: List<RestTaskDescription>,
        val teams: List<RestTeam>,
        val participantCanView: Boolean
) {


    companion object {
        /**
         * Generates a [RestCompetitionDescription] from a [CompetitionDescription] and returns it.
         *
         * @param competition The [CompetitionDescription] to convert.
         */
        fun fromCompetition(competition: CompetitionDescription) = RestCompetitionDescription(
            competition.id.string,
            competition.name,
            competition.description,
            competition.taskTypes,
            competition.taskGroups,
            competition.tasks.map { RestTaskDescription.fromTask(it) },
            competition.teams.map { RestTeam(it) },
            competition.participantCanView
        )
    }

    /**
     * Converts this [RestCompetitionDescription] to the corresponding [CompetitionDescription] and returns it.
     *
     * @param mediaItems [DAO] used to perform media item lookups.
     */
    fun toCompetitionDescription(mediaItems: DAO<MediaItem>) = CompetitionDescription(
        this.id.UID(),
        this.name,
        this.description,
        this.taskTypes.toMutableList(),
        this.taskGroups.toMutableList(),
        this.tasks.map { it.toTaskDescription(this.taskGroups, this.taskTypes, mediaItems) }.toMutableList(),
        this.teams.map { Team(it) }.toMutableList(),
        this.participantCanView
    )
}

