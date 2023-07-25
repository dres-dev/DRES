package dev.dres.api.rest.handler.template

import dev.dres.api.rest.handler.PatchRestHandler
import dev.dres.api.rest.types.template.ApiEvaluationTemplate
import dev.dres.api.rest.types.template.tasks.ApiTargetType
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.api.rest.types.status.SuccessStatus
import dev.dres.data.model.config.Config
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
import kotlinx.dnq.util.getSafe
import org.joda.time.DateTime
import kotlin.time.ExperimentalTime

/**
 * A [AbstractEvaluationTemplateHandler] that can be used to create a new [DbEvaluationTemplate].
 *
 * @author Ralph Gasser
 * @author Luca Rossetto
 * @author Loris Sauter
 * @version 2.1.0
 */
class UpdateEvaluationTemplateHandler(store: TransientEntityStore, val config: Config) :
    AbstractEvaluationTemplateHandler(store), PatchRestHandler<SuccessStatus> {

    override val route: String = "template/{templateId}"

    @OpenApi(
        summary = "Updates an existing evaluation template.",
        path = "/api/v2/template/{templateId}",
        operationId = OpenApiOperation.AUTO_GENERATE,
        pathParams = [OpenApiParam(
            "templateId",
            String::class,
            "The evaluation template ID.",
            required = true,
            allowEmptyValue = false
        )],
        methods = [HttpMethod.PATCH],
        requestBody = OpenApiRequestBody([OpenApiContent(ApiEvaluationTemplate::class)]),
        tags = ["Template"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
            OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)]),
            OpenApiResponse("409", [OpenApiContent(ErrorStatus::class)])
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
            if (existing.modified?.millis != apiValue.modified) {
                throw ErrorStatusException(409, "Evaluation template ${apiValue.id} has been modified in the meantime. Reload and try again!", ctx)
            }

            /* Update core information. */
            existing.name = apiValue.name
            existing.description = apiValue.description
            existing.modified = DateTime.now()

            /* Update task type information. */
            val taskTypes = apiValue.taskTypes.map { it.name }.toTypedArray()
            existing.taskTypes.removeAll(
                DbTaskType.query(DbTaskType::evaluation eq existing and not(DbTaskType::name.containsIn(*taskTypes)))
            )
            for (apiTaskType in apiValue.taskTypes) {
                val taskType = DbTaskType.findOrNew(DbTaskType.query((DbTaskType::name eq apiTaskType.name) and (DbTaskType::evaluation eq existing))) {
                    this.name = apiTaskType.name
                }
                taskType.duration = apiTaskType.duration
                taskType.score = apiTaskType.scoreOption.toDb()
                taskType.target = apiTaskType.targetOption.toDb()
                taskType.hints.clear()
                taskType.hints.addAll(apiTaskType.hintOptions.map { it.toDb() })
                taskType.submission.clear()
                taskType.submission.addAll(apiTaskType.submissionOptions.map { it.toDb() })
                taskType.options.clear()
                taskType.options.addAll(apiTaskType.taskOptions.map { it.toDb() })
                taskType.configurations.clear()
                taskType.configurations.addAll(apiTaskType.configuration.entries.map {
                    DbConfiguredOption.new {
                        this.key = it.key
                        this.value = it.value
                    }
                })

                /* Establish relationship if entry is new. */
                if (taskType.isNew) {
                    existing.taskTypes.add(taskType)
                }
            }

            /* Update task group information. */
            val taskGroups = apiValue.taskGroups.map { it.name }.toTypedArray()
            existing.taskGroups.removeAll(
                DbTaskGroup.query(DbTaskGroup::evaluation eq existing and not(DbTaskGroup::name.containsIn(*taskGroups)))
            )
            for (apiTaskGroup in apiValue.taskGroups) {
                val taskGroup = DbTaskGroup.findOrNew(DbTaskGroup.query((DbTaskGroup::name eq apiTaskGroup.name) and (DbTaskGroup::evaluation eq existing))) {
                    this.name = apiTaskGroup.name
                }

                /* Update task type if it has changed. */
                if (taskGroup.getSafe(DbTaskGroup::type)?.name != apiTaskGroup.name) {
                    taskGroup.type = DbTaskType.query((DbTaskType::name eq apiTaskGroup.type) and (DbTaskType::evaluation eq existing)).firstOrNull()
                        ?: throw ErrorStatusException(404, "Unknown task group ${apiTaskGroup.type} for evaluation ${apiValue.id}.", ctx)
                }

                /* Establish relationship if entry is new. */
                if (taskGroup.isNew) {
                    existing.taskGroups.add(taskGroup)
                }
            }

            /* Update task information: Remove deleted tasks. */
            val taskIds = apiValue.tasks.mapNotNull { it.id }.toTypedArray()
            existing.tasks.removeAll(DbTaskTemplate.query(DbTaskTemplate::evaluation eq existing and not(DbTaskTemplate::id.containsIn(*taskIds))))

            /*  Update task information: Remaining tasks. */
            for (apiTask in apiValue.tasks) {
                val task = if (apiTask.id != null) {
                    existing.tasks.filter { it.id eq apiTask.id }.firstOrNull() ?: throw ErrorStatusException(404, "Unknown task ${apiTask.id} for evaluation ${apiValue.id}.", ctx)
                } else {
                    val t = DbTaskTemplate.new()
                    existing.tasks.add(t)
                    t
                }

                /* Update parameters that do no require lookup. */
                task.name = apiTask.name
                task.duration = apiTask.duration
                task.comment = apiTask.comment

                /* Conditional updating of parameters that do!. */
                if (task.isNew || task.collection.id != apiTask.collectionId) {
                    task.collection = DbMediaCollection.query(DbMediaCollection::id eq apiTask.collectionId).first()
                }

                if (task.isNew || task.taskGroup.name != apiTask.taskGroup) {
                    task.taskGroup = DbTaskGroup.query(DbTaskGroup::name eq apiTask.taskGroup).first()
                }

                /* Update task targets. */
                task.targets.clear()
                for (target in apiTask.targets) {
                    task.targets.add(DbTaskTemplateTarget.new {
                        this.type = target.type.toDb()
                        this.start = target.range?.start?.toTemporalPoint(item?.fps ?: 0.0f)?.toMilliseconds()
                        this.end = target.range?.end?.toTemporalPoint(item?.fps ?: 0.0f)?.toMilliseconds()
                        when (target.type) {
                            ApiTargetType.TEXT -> this.text = target.target
                            else -> this.item = target.target?.let { DbMediaItem.query(DbMediaItem::id eq it).firstOrNull() }
                        }
                    })
                }

                /* Update task hints. */
                task.hints.clear()
                for (hint in apiTask.hints) {
                    task.hints.add(DbHint.new {
                        this.type = hint.type.toDb()
                        this.item = hint.mediaItem?.let { DbMediaItem.query(DbMediaItem::id eq hint.mediaItem).firstOrNull() }
                        this.text = hint.description
                        this.path = hint.path
                        this.start = hint.start
                        this.end = hint.end
                        this.temporalRangeStart = hint.range?.start?.toTemporalPoint(item?.fps ?: 0.0f)?.toMilliseconds()
                        this.temporalRangeEnd = hint.range?.end?.toTemporalPoint(item?.fps ?: 0.0f)?.toMilliseconds()
                    })
                }
            }

            /* Update team information. */
            val teamIds = apiValue.teams.map { it.id }.toTypedArray()
            existing.teams.removeAll(DbTeam.query(DbTeam::evaluation eq existing and not(DbTeam::id.containsIn(*teamIds))))
            for (apiTeam in apiValue.teams) {
                val team = if (apiTeam.id != null) {
                    existing.teams.filter { it.id eq apiTeam.id }.firstOrNull() ?: throw ErrorStatusException(404, "Unknown team ${apiTeam.id} for evaluation ${apiValue.id}.", ctx)
                } else {
                    val t = DbTeam.new()
                    existing.teams.add(t) /* Establish new relationship. */
                    t
                }

                team.name = apiTeam.name ?: throw ErrorStatusException(404, "Team name must be specified.", ctx)
                team.color = apiTeam.color ?: throw ErrorStatusException(404, "Team colour must be specified.", ctx)

                /* Process logo data. */
                val logoData = apiTeam.logoStream()
                if (logoData != null) {
                    team.logo = logoData
                }

                /* Make association with users. */
                val userIds = apiTeam.users.map { it.id }.toTypedArray()
                team.users.removeAll(DbUser.query(not(DbUser::id.containsIn(*userIds))))
                for (userId in userIds) {
                    val user = DbUser.filter { it.id eq userId }.firstOrNull()  ?: throw ErrorStatusException(404, "Unknown user $userId for evaluation ${apiValue.id}.", ctx)
                    if (!team.users.contains(user)) {
                        team.users.add(user)
                    }
                }
            }

            /* Update teamGroup information */
            val teamGroupIds = apiValue.teamGroups.map { it.id }.toTypedArray()
            existing.teamGroups.removeAll(DbTeamGroup.query(DbTeamGroup::evaluation eq existing and not(DbTeamGroup::id.containsIn(*teamGroupIds))))

            for (apiTeamGroup in apiValue.teamGroups) {
                val teamGroup = if (apiTeamGroup.id != null) {
                    existing.teamGroups.filter { it.id eq apiTeamGroup.id }.firstOrNull() ?: throw ErrorStatusException(404, "Unknown team group ${apiTeamGroup.id} for evaluation ${apiValue.id}.", ctx)
                } else {
                    val tg = DbTeamGroup.new()
                    existing.teamGroups.add(tg)
                    tg
                }
                teamGroup.name = apiTeamGroup.name ?: throw ErrorStatusException(404, "Team group name must be specified.", ctx)
                teamGroup.defaultAggregator = apiTeamGroup.aggregation.toDb()

                teamGroup.teams.clear()
                teamGroup.teams.addAll(DbTeam.query(DbTeam::id.containsIn(*apiTeamGroup.teams.map { it.id }.toTypedArray())))

                /* Establish relationship if entry is new. */
                if (teamGroup.isNew) {
                    existing.teamGroups.add(teamGroup)
                }
            }

            /* Update judge information */
            val judgeIds = apiValue.judges.toTypedArray()
            existing.judges.removeAll(DbUser.query(not(DbUser::id.containsIn(*judgeIds))))
            for (userId in judgeIds) {
                val user = DbUser.filter { it.id eq userId }.firstOrNull()  ?: throw ErrorStatusException(404, "Unknown user $userId for evaluation ${apiValue.id}.", ctx)
                if (!existing.judges.contains(user)) {
                    existing.judges.add(user)
                }
            }
        }
        return SuccessStatus("Competition with ID ${apiValue.id} was updated successfully.")
    }
}


