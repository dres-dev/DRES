package dev.dres.data.migration

import dev.dres.api.rest.types.competition.RestTeam
import dev.dres.data.dbo.DAO
import dev.dres.data.dbo.DataAccessLayer
import dev.dres.data.model.Config
import dev.dres.data.model.UID
import dev.dres.data.model.competition.CompetitionDescription
import dev.dres.data.model.competition.Team
import dev.dres.data.model.run.CompetitionRun
import dev.dres.data.model.run.Submission

/**
 * Migration script for migrating the team logos.
 *
 * @author Ralph Gasser
 * @version 1.0
 */
object TeamLogoMigration : Migration {

    /**
     * Performs migration.
     *
     * @param config The global [Config] used for the migration.
     * @param data The [DataAccessLayer].
     */
    override fun migrate(config: Config, data: DataAccessLayer) {
        val migrated = this.migrateCompetitions(config, data.competitions)
        this.migrateRuns(config, migrated, data.runs)
    }

    /**
     * Migrates all [CompetitionDescription]s
     *
     * @param config THe global [Config] used for the migration.
     * @param dao The [DAO] used for data access.
     */
    private fun migrateCompetitions(config: Config, dao: DAO<CompetitionDescription>): Map<String,UID> {
        val updateMap = mutableMapOf<String,UID>()
        for (desc in dao.iterator()) {
            try {
                desc.teams.forEach { it.logoId }
            } catch (e: IllegalArgumentException) {
                println("Migrating team data for competition ${desc.id}...")
                val newTeams = desc.teams.mapIndexed { index, team ->
                    val uid = RestTeam.storeLogo(config, team.logo)
                    updateMap["${desc.id}.$index"] = uid
                    Team(team.name, team.color, uid.string, team.users)
                }.toMutableList()
                dao.update(desc.copy(teams = newTeams))
            }
        }
        return updateMap
    }


    /**
     * Migrates all [CompetitionRun]s.
     *
     * @param config THe global [Config] used for the migration.
     * @param updateMap Map of all migrations that took place as part of the [CompetitionDescription] migration.
     * @param dao The [DAO] used for data access.
     */
    private fun migrateRuns(config: Config, updateMap: Map<String,UID>, dao: DAO<CompetitionRun>) {
        for (run in dao.iterator()) {
            try {
                run.competitionDescription.teams.forEach { it.logoId }
            } catch (e: IllegalArgumentException) {
                println("Migrating team data for run ${run.id}...")
                val newTeams = run.competitionDescription.teams.mapIndexed { index, team ->
                    val uid = if (updateMap.containsKey("${run.competitionDescription.id}.$index")) {
                        updateMap.getValue("${run.competitionDescription.id}.$index")
                    } else {
                        RestTeam.storeLogo(config, team.logo)
                    }
                    Team(team.name, team.color, uid.string, team.users)
                }.toMutableList()

                val newDesc = run.competitionDescription.copy(teams = newTeams)
                val newRun = CompetitionRun(run.id, run.name, newDesc, run.started ?: -1L , run.ended ?: -1L )
                for (t in run.runs) {
                    val task = newRun.TaskRun(t.taskId, t.uid, t.started ?: -1L, t.ended ?: -1L)
                    for (s in t.submissions) {
                        (task.submissions as MutableList<Submission>).add(s)
                    }
                }
                dao.update(newRun)
            }
        }
    }
}