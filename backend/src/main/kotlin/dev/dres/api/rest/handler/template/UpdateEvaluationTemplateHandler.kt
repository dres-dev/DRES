package dev.dres.api.rest.handler.template

import dev.dres.api.rest.handler.PatchRestHandler
import dev.dres.api.rest.types.competition.ApiEvaluationTemplate
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.api.rest.types.status.SuccessStatus
import dev.dres.data.model.Config
import dev.dres.data.model.admin.DbUser
import dev.dres.data.model.media.DbMediaCollection
import dev.dres.data.model.template.DbEvaluationTemplate
import dev.dres.data.model.template.task.*
import dev.dres.data.model.template.task.options.DbConfiguredOption
import dev.dres.data.model.template.team.DbTeam
import dev.dres.data.model.template.team.DbTeamGroup
import dev.dres.data.model.media.DbMediaItem
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.creator.findOrNew
import kotlinx.dnq.query.*

/**
 * A [AbstractEvaluationTemplateHandler] that can be used to create a new [DbEvaluationTemplate].
 *
 * @author Ralph Gasser
 * @author Luca Rossetto
 * @author Loris Sauter
 * @version 2.0.0
 */
class UpdateEvaluationTemplateHandler(store: TransientEntityStore, val config: Config) : AbstractEvaluationTemplateHandler(store), PatchRestHandler<SuccessStatus> {

    override val route: String = "template/{templateId}"

    @OpenApi(
            summary = "Updates an existing evaluation template.",
            path = "/api/v2/template/{templateId}",
            operationId = OpenApiOperation.AUTO_GENERATE,
            pathParams = [OpenApiParam("templateId", String::class, "The evaluation template ID.", required = true, allowEmptyValue = false)],
            methods = [HttpMethod.PATCH],
            requestBody = OpenApiRequestBody([OpenApiContent(ApiEvaluationTemplate::class)]),
            tags = ["Template"],
            responses = [
                OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
                OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
                OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
            ]
    )
    override fun doPatch(ctx: Context): SuccessStatus {
        val apiValue = try {
            ctx.bodyAsClass(ApiEvaluationTemplate::class.java)
        } catch (e: BadRequestResponse) {
            throw ErrorStatusException(400, "Invalid parameters. This is a programmers error!", ctx)
        }

        /* Store change. */
        this.store.transactional {
            val existing = this.evaluationTemplateById(apiValue.id, ctx)

            /* Update core information. */
            existing.name = apiValue.name
            existing.description = apiValue.description

            /* Update task type information. */
            val taskTypes = apiValue.taskTypes.map { it.name }.toTypedArray()
            existing.taskTypes.removeAll(DbTaskType.query(DbTaskType::evaluation eq existing and not(DbTaskType::name.containsIn(*taskTypes))))
            for (type in apiValue.taskTypes) {
                val t = DbTaskType.findOrNew(DbTaskType.query(  (DbTaskType::name eq type.name) and (DbTaskType::evaluation eq existing))) {
                  this.name = type.name
                }
                t.duration = type.duration
                t.score = type.scoreOption.toDb()
                t.target = type.targetOption.toDb()
                t.hints.clear()
                t.hints.addAll(type.hintOptions.map { it.toDb() })
                t.submission.clear()
                t.submission.addAll(type.submissionOptions.map { it.toDb() })
                t.options.clear()
                t.options.addAll(type.taskOptions.map { it.toDb() })
                t.configurations.clear()
                t.configurations.addAll(type.configuration.entries.map {
                    DbConfiguredOption.new {
                        this.key = it.key
                        this.value = it.value
                    }
                })

                /* Establish relationship if entry is new. */
                if (t.isNew) {
                    existing.taskTypes.add(t)
                }
            }

            /* Update task group information. */
            val taskGroups = apiValue.taskGroups.map { it.name }.toTypedArray()
            existing.taskGroups.removeAll(DbTaskGroup.query(DbTaskGroup::evaluation eq existing and not(DbTaskGroup::name.containsIn(*taskGroups))))
            for (group in apiValue.taskGroups) {
                val g = DbTaskGroup.findOrNew(DbTaskGroup.query((DbTaskGroup::name eq group.name) and (DbTaskGroup::evaluation eq existing))) {
                    this.name = group.name
                }
                g.type = DbTaskType.query((DbTaskType::name eq group.type) and (DbTaskType::evaluation eq existing)).firstOrNull() ?: throw ErrorStatusException(404, "Unknown task group ${group.type} for evaluation ${apiValue.id}.", ctx)

                /* Establish relationship if entry is new. */
                if (g.isNew) {
                    existing.taskGroups.add(g)
                }
            }

            /* Update task information. */
            val taskIds = apiValue.tasks.mapNotNull { it.id }.toTypedArray()
            existing.tasks.removeAll(DbTaskTemplate.query(DbTaskTemplate::evaluation eq existing and not(DbTaskTemplate::id.containsIn(*taskIds))))
            for (task in apiValue.tasks) {
                val t = if (task.id != null) {
                    existing.tasks.filter { it.id eq task.id }.firstOrNull() ?: throw ErrorStatusException(404, "Unknown task ${task.id} for evaluation ${apiValue.id}.", ctx)
                } else {
                    DbTaskTemplate.new()
                }
                t.name = task.name
                t.duration = task.duration
                t.collection = DbMediaCollection.query(DbMediaCollection::id eq task.collectionId).first()
                t.taskGroup = DbTaskGroup.query(DbTaskGroup::name eq task.taskGroup).first()

                /* Update task targets. */
                t.targets.clear()
                for (target in task.targets) {
                    val item = DbMediaItem.query(DbMediaItem::id eq target.target).first()
                    t.targets.add(DbTaskTemplateTarget.new {
                        this.item = item
                        this.type = target.type.toDb()
                        this.start = target.range?.start?.toTemporalPoint(item.fps ?: 0.0f)?.toMilliseconds()
                        this.end = target.range?.end?.toTemporalPoint(item.fps ?: 0.0f)?.toMilliseconds()
                    })
                }

                /* Update task hints. */
                t.hints.clear()
                for (hint in task.hints) {
                    val item = DbMediaItem.query(DbMediaItem::id eq hint.mediaItem).firstOrNull()
                    t.hints.add(DbHint.new {
                        this.type = hint.type.toDb()
                        this.item = item
                        this.text = hint.description
                        this.path = hint.path
                        this.start = hint.start
                        this.end = hint.end
                        this.temporalRangeStart = hint.range?.start?.toTemporalPoint(item?.fps ?: 0.0f)?.toMilliseconds()
                        this.temporalRangeEnd = hint.range?.end?.toTemporalPoint(item?.fps ?: 0.0f)?.toMilliseconds()
                    })
                }

                /* Establish relationship if entry is new. */
                if (t.isNew) {
                    existing.tasks.add(t)
                }
            }

            /* Update team information. */
            val teamIds = apiValue.teams.map { it.id }.toTypedArray()
            existing.teams.removeAll(DbTeam.query(DbTeam::evaluation eq existing and not(DbTeam::id.containsIn(*teamIds))))
            for (team in apiValue.teams) {
                val t = if (team.id != null) {
                    existing.teams.filter { it.id eq team.id }.firstOrNull() ?: throw ErrorStatusException(404, "Unknown team ${team.id} for evaluation ${apiValue.id}.", ctx)
                } else {
                    DbTeam.new()
                }

                t.name = team.name ?: throw ErrorStatusException(404, "Team name must be specified.", ctx)
                t.color = team.color ?: throw ErrorStatusException(404, "Team colour must be specified.", ctx)

                /* Process logo data. */
                val logoData = team.logoStream()
                if (logoData != null) {
                    t.logo = logoData
                }

                t.users.clear()
                t.users.addAll(DbUser.query(DbUser::id.containsIn(*team.users.map { it.id }.toTypedArray())))

                /* Establish relationship if entry is new. */
                if (t.isNew) {
                    existing.teams.add(t)
                }
            }

            /* Update teamGroup information */
            val teamGroupIds = apiValue.teamGroups.map { it.id }.toTypedArray()
            existing.teamGroups.removeAll(DbTeamGroup.query(DbTeamGroup::evaluation eq existing and not(DbTeamGroup::id.containsIn(*teamGroupIds))))
            for (teamGroup in apiValue.teamGroups) {
                val t = if (teamGroup.id != null) {
                    existing.teamGroups.filter { it.id eq teamGroup.id }.firstOrNull() ?: throw ErrorStatusException(404, "Unknown team groum ${teamGroup.id} for evaluation ${apiValue.id}.", ctx)
                } else {
                    DbTeamGroup.new()
                }

                t.teams.clear()
                t.teams.addAll(DbTeam.query(DbTeam::id.containsIn(*teamGroup.teams.map { it.id }.toTypedArray())))

                /* Establish relationship if entry is new. */
                if (t.isNew) {
                    existing.teamGroups.add(t)
                }
            }

            /* Update judge information */
            existing.judges.clear()
            existing.judges.addAll(DbUser.query(DbUser::id.containsIn(*apiValue.judges.toTypedArray())))
        }
        return SuccessStatus("Competition with ID ${apiValue.id} was updated successfully.")
    }

}


