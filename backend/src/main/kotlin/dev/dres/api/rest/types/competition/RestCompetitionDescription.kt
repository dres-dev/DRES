package dev.dres.api.rest.types.competition

import dev.dres.data.dbo.DAO
import dev.dres.data.model.Config
import dev.dres.data.model.admin.UserId
import dev.dres.data.model.basics.media.MediaItem
import dev.dres.data.model.competition.CompetitionDescription
import dev.dres.data.model.competition.TaskGroup
import dev.dres.data.model.competition.TaskType
import dev.dres.utilities.extensions.UID

/**
 * The RESTful API equivalent for [CompetitionDescription].
 *
 * @see CompetitionDescription
 * @author Luca Rossetto & Ralph Gasser
 * @version 1.0.1
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
        val teamGroups: List<RestTeamGroup>,
        val judges: List<String>,
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
            competition.teamGroups.map { RestTeamGroup(it) },
            competition.judges.map { it.string },
            competition.participantCanView
        )
    }

    /**
     * Converts this [RestCompetitionDescription] to the corresponding [CompetitionDescription] and returns it.
     *
     * @param config The global [Config] object used during conversion.
     * @param mediaItems [DAO] used to perform media item lookups.
     */
    fun toCompetitionDescription(config: Config, mediaItems: DAO<MediaItem>) : CompetitionDescription {

        val teams = this.teams.map { it.toTeam(config) }.toMutableList()

        return CompetitionDescription(
            this.id.UID(),
            this.name,
            this.description,
            this.taskTypes.toMutableList(),
            this.taskGroups.toMutableList(),
            this.tasks.map { it.toTaskDescription(this.taskGroups, this.taskTypes, mediaItems) }.toMutableList(),
            teams,
            this.teamGroups.map { it.toTeamGroup(teams) }.toMutableList(),
            this.judges.map { UserId(it) }.toMutableList(),
            this.participantCanView
        )
    }
}

