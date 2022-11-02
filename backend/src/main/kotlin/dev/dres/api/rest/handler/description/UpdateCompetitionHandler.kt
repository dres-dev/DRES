package dev.dres.api.rest.handler.description

import dev.dres.api.rest.handler.PatchRestHandler
import dev.dres.api.rest.types.competition.ApiCompetitionDescription
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.api.rest.types.status.SuccessStatus
import dev.dres.data.model.Config
import dev.dres.data.model.admin.User
import dev.dres.data.model.competition.CompetitionDescription
import dev.dres.data.model.competition.task.*
import dev.dres.data.model.competition.task.options.ConfiguredOption
import dev.dres.data.model.competition.team.Team
import dev.dres.data.model.competition.team.TeamGroup
import dev.dres.data.model.media.MediaItem
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.creator.findOrNew
import kotlinx.dnq.query.*
import java.util.*

/**
 * A [AbstractCompetitionDescriptionHandler] that can be used to create a new [CompetitionDescription].
 *
 * @author Ralph Gasser
 * @author Luca Rossetto
 * @author Loris Sauter
 * @version 2.0.0
 */
class UpdateCompetitionHandler(store: TransientEntityStore, val config: Config) : AbstractCompetitionDescriptionHandler(store), PatchRestHandler<SuccessStatus> {

    override val route: String = "competition"

    @OpenApi(
            summary = "Updates an existing competition.",
            path = "/api/v1/competition",
            methods = [HttpMethod.PATCH],
            requestBody = OpenApiRequestBody([OpenApiContent(ApiCompetitionDescription::class)]),
            tags = ["Competition"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
                OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doPatch(ctx: Context): SuccessStatus {
        val apiValue = try {
            ctx.bodyAsClass(ApiCompetitionDescription::class.java)
        } catch (e: BadRequestResponse) {
            throw ErrorStatusException(400, "Invalid parameters. This is a programmers error!", ctx)
        }

        /* Store change. */
        this.store.transactional {
            val existing = this.competitionById(apiValue.id, ctx)

            /* Update core information. */
            existing.name = apiValue.name
            existing.description = apiValue.description

            /* Update task type information. */
            val taskTypes = apiValue.taskTypes.map { it.name }.toTypedArray()
            existing.taskTypes.removeAll(TaskType.query(TaskType::competition eq existing and not(TaskType::name.containsIn(*taskTypes))))
            for (type in apiValue.taskTypes) {
                val t = TaskType.findOrNew {
                    (TaskType::name eq type.name) and (TaskType::competition eq existing)
                }
                t.name = type.name
                t.duration = type.duration
                t.score = type.scoreOption.option
                t.targets.clear()
                t.targets.addAll(type.targetOptions.map { it.option })
                t.hints.clear()
                t.hints.addAll(type.hintOptions.map { it.option })
                t.submission.clear()
                t.submission.addAll(type.submissionOptions.map { it.option })
                t.options.clear()
                t.options.addAll(type.taskOptions.map { it.option })
                t.configurations.clear()
                t.configurations.addAll(type.configuration.entries.map {
                    ConfiguredOption.new {
                        this.key = it.key
                        this.value = it.value
                    }
                })
            }

            /* Update task group information. */
            val taskGroups = apiValue.taskGroups.map { it.name }.toTypedArray()
            existing.taskGroups.removeAll(TaskGroup.query(TaskGroup::competition eq existing and not(TaskGroup::name.containsIn(*taskGroups))))
            for (group in apiValue.taskGroups) {
                val g = TaskGroup.findOrNew {
                    (TaskGroup::name eq type.name) and (TaskGroup::competition eq existing)
                }
                g.name = group.name
                g.type = TaskType.query((TaskType::name eq group.name) and (TaskGroup::competition eq existing)).first()

                /* Update task information. */
                val taskIds = group.tasks.mapNotNull { it.id }.toTypedArray()
                g.tasks.removeAll(TaskDescription.query(TaskDescription::taskGroup eq g and not(TaskDescription::id.containsIn(*taskIds))))
                for (task in group.tasks) {
                    val t = if (task.id != null) {
                        g.tasks.filter { it.id eq task.id }.first()
                    } else {
                        val desc = TaskDescription.new { this.id = UUID.randomUUID().toString() }
                        g.tasks.add(desc)
                        desc
                    }
                    t.name = task.name
                    t.duration = task.duration

                    /* Update task targets. */
                    t.targets.clear()
                    for (target in task.targets) {
                        val item = MediaItem.query(MediaItem::id eq target.target).first()
                        t.targets.add(TaskDescriptionTarget.new {
                            this.item = item
                            this.type = target.type.type
                            this.start = target.range?.start?.toTemporalPoint(item.fps ?: 0.0f)?.toMilliseconds()
                            this.end = target.range?.end?.toTemporalPoint(item.fps?: 0.0f)?.toMilliseconds()
                        })
                    }

                    /* Update task hints. */
                    t.hints.clear()
                    for (hint in task.hints) {
                        val item = MediaItem.query(MediaItem::id eq hint.mediaItem).firstOrNull()
                        t.hints.add(Hint.new {
                            this.type = hint.type.type
                            this.item = item
                            this.text = hint.description
                            this.path = hint.path
                            this.start = hint.start
                            this.end = hint.end
                            this.temporalRangeStart = hint.range?.start?.toTemporalPoint(item?.fps ?: 0.0f)?.toMilliseconds()
                            this.temporalRangeEnd = hint.range?.end?.toTemporalPoint(item?.fps?: 0.0f)?.toMilliseconds()
                        })
                    }
                }
            }

            /* Update team information. */
            val teamIds = apiValue.teams.map { it.teamId }.toTypedArray()
            existing.teams.removeAll(Team.query(Team::competition eq existing and not(Team::id.containsIn(*teamIds))))
            for (team in apiValue.teams) {
                val t = Team.findOrNew {
                    (Team::name eq team.name) and (Team::competition eq existing)
                }
                t.name = team.name
                t.color = team.color
                t.logoId = team.logoId
                t.users.clear()
                t.users.addAll(User.query(User::id.containsIn(*team.users.map { it.id }.toTypedArray())))
            }

            /* Update teamGroup information */
            val teamGroupIds = apiValue.teamGroups.map { it.id }.toTypedArray()
            existing.teamsGroups.removeAll(TeamGroup.query(TeamGroup::competition eq existing and not(TeamGroup::id.containsIn(*teamGroupIds))))
            for (teamGroup in apiValue.teamGroups) {
                val t = TeamGroup.findOrNew {
                    (Team::name eq teamGroup.name) and (Team::competition eq existing)
                }
                t.name = teamGroup.name
                t.teams.clear()
                t.teams.addAll(Team.query(Team::id.containsIn(*teamGroup.teams.map { it.teamId }.toTypedArray())))
            }

            /* Update judge information */
            existing.judges.clear()
            existing.judges.addAll(User.query(User::id.containsIn(*apiValue.judges.toTypedArray())))
        }
        return SuccessStatus("Competition with ID ${apiValue.id} was updated successfully.")
    }
}


