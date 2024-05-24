package dev.dres.mgmt

import dev.dres.DRES
import dev.dres.api.rest.types.template.ApiEvaluationTemplate
import dev.dres.api.rest.types.template.ApiEvaluationTemplateOverview
import dev.dres.api.rest.types.template.tasks.ApiTargetType
import dev.dres.data.model.admin.DbUser
import dev.dres.data.model.media.DbMediaCollection
import dev.dres.data.model.media.DbMediaItem
import dev.dres.data.model.template.DbEvaluationTemplate
import dev.dres.data.model.template.TemplateId
import dev.dres.data.model.template.task.*
import dev.dres.data.model.template.task.options.DbConfiguredOption
import dev.dres.data.model.template.team.DbTeam
import dev.dres.data.model.template.team.DbTeamGroup
import dev.dres.data.model.template.team.TeamId
import dev.dres.mgmt.cache.CacheManager
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.creator.findOrNew
import kotlinx.dnq.query.*
import kotlinx.dnq.util.getSafe
import org.joda.time.DateTime
import java.io.InputStream
import java.nio.file.Files
import java.util.Base64
import java.util.concurrent.TimeUnit

object TemplateManager {

    private lateinit var store: TransientEntityStore

    fun init(store: TransientEntityStore) {
        this.store = store
    }

    fun createEvaluationTemplate(name: String, description: String): ApiEvaluationTemplate = this.store.transactional {
        val template = DbEvaluationTemplate.new {
            this.instance = false
            this.name = name
            this.description = description
            this.created = DateTime.now()
            this.modified = DateTime.now()
        }
        template.toApi()
    }

    fun deleteTemplate(templateId: TemplateId): ApiEvaluationTemplate? = this.store.transactional {
        val template =
            DbEvaluationTemplate.query((DbEvaluationTemplate::id) eq templateId and (DbEvaluationTemplate::instance eq false))
                .firstOrNull() ?: return@transactional null
        val api = template.toApi()
        template.delete()
        api
    }

    fun getTemplateOverview(): List<ApiEvaluationTemplateOverview> = this.store.transactional(true) {
        DbEvaluationTemplate.query(DbEvaluationTemplate::instance eq false).asSequence().map {
            ApiEvaluationTemplateOverview(it.id, it.name, it.description, it.tasks.size(), it.teams.size())
        }.toList()
    }

    fun getTemplate(templateId: TemplateId): ApiEvaluationTemplate? = this.store.transactional (true){  getDbTemplate(templateId)?.toApi() }

    private fun getDbTemplate(templateId: TemplateId): DbEvaluationTemplate? = this.store.transactional(true) {
        DbEvaluationTemplate.query((DbEvaluationTemplate::id) eq templateId).firstOrNull()
    }

    fun getTeamLogo(teamId: TeamId) : InputStream? = this.store.transactional(true) {
        DbTeam.query(DbTeam::id eq teamId).firstOrNull()?.logo
    }

    /**
     * Writes the state of an [ApiEvaluationTemplate].
     * Requires a transaction context.
     */
    fun updateTemplate(apiEvaluationTemplate: ApiEvaluationTemplate) {
        val dbEvaluationTemplate =
            this.store.transactional(true){
            DbEvaluationTemplate.query((DbEvaluationTemplate::id) eq apiEvaluationTemplate.id and (DbEvaluationTemplate::instance eq false))
                .firstOrNull() ?: throw IllegalArgumentException("No template with id '${apiEvaluationTemplate.id}'")
        }
        updateTemplate(dbEvaluationTemplate, apiEvaluationTemplate)
    }

    @Throws(IllegalArgumentException::class)
    fun updateTemplate(dbEvaluationTemplate: DbEvaluationTemplate, apiEvaluationTemplate: ApiEvaluationTemplate) = this.store.transactional {


        /* Update core information. */
        dbEvaluationTemplate.name = apiEvaluationTemplate.name
        dbEvaluationTemplate.description = apiEvaluationTemplate.description
        dbEvaluationTemplate.modified = DateTime.now()

        /* Update task type information. */
        val taskTypes = apiEvaluationTemplate.taskTypes.map { it.name }.toTypedArray()
        dbEvaluationTemplate.taskTypes.removeAll(
            DbTaskType.query(DbTaskType::evaluation eq dbEvaluationTemplate and not(DbTaskType::name.containsIn(*taskTypes)))
        )
        for (apiTaskType in apiEvaluationTemplate.taskTypes) {
            val taskType =
                DbTaskType.findOrNew(DbTaskType.query((DbTaskType::name eq apiTaskType.name) and (DbTaskType::evaluation eq dbEvaluationTemplate))) {
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
                if(!it.key.contains(".") || it.key.split(".").size != 2){
                    throw IllegalArgumentException("Type Configurations must be in the form of DOMAIN.KEY, but ${it.key} given")
                }
                DbConfiguredOption.new {
                    this.key = it.key
                    this.value = it.value
                }
            })

            /* Establish relationship if entry is new. */
            if (taskType.isNew) {
                dbEvaluationTemplate.taskTypes.add(taskType)
            }
        }

        /* Update task group information. */
        val taskGroups = apiEvaluationTemplate.taskGroups.map { it.name }.toTypedArray()
        dbEvaluationTemplate.taskGroups.removeAll(
            DbTaskGroup.query(DbTaskGroup::evaluation eq dbEvaluationTemplate and not(DbTaskGroup::name.containsIn(*taskGroups)))
        )
        for (apiTaskGroup in apiEvaluationTemplate.taskGroups) {
            val taskGroup =
                DbTaskGroup.findOrNew(DbTaskGroup.query((DbTaskGroup::name eq apiTaskGroup.name) and (DbTaskGroup::evaluation eq dbEvaluationTemplate))) {
                    this.name = apiTaskGroup.name
                }

            /* Update task type if it has changed. */
            if (taskGroup.getSafe(DbTaskGroup::type)?.name != apiTaskGroup.name) {
                taskGroup.type =
                    DbTaskType.query((DbTaskType::name eq apiTaskGroup.type) and (DbTaskType::evaluation eq dbEvaluationTemplate))
                        .firstOrNull()
                        ?: throw IllegalArgumentException("Unknown task group ${apiTaskGroup.type} for evaluation ${apiEvaluationTemplate.id}.")
            }

            /* Establish relationship if entry is new. */
            if (taskGroup.isNew) {
                dbEvaluationTemplate.taskGroups.add(taskGroup)
            }
        }

        /* Update task information: Remove deleted tasks. */
        val taskIds = apiEvaluationTemplate.tasks.mapNotNull { it.id }.toTypedArray()
        val taskTemplatesToDeleteQuery = DbTaskTemplate.query(
            DbTaskTemplate::evaluation eq dbEvaluationTemplate and not(
                DbTaskTemplate::id.containsIn(*taskIds)
            )
        )
        val hintsToDelIds = taskTemplatesToDeleteQuery.toList().map {
            it.hints.toList().map { hint -> hint.entityId }
        }.flatten().toTypedArray()
        val targetsToDelIds = taskTemplatesToDeleteQuery.toList().map{
            it.targets.toList().map{target -> target.entityId}
        }.flatten().toTypedArray()

        dbEvaluationTemplate.tasks.removeAll(
            taskTemplatesToDeleteQuery
        )
        /*
        DbTaskTemplate has children relationships with both, DbHint and DbTaskTarget.
        Despite being written in the documentation, for some reason the .removeAll above does not
        delete the children, hence we have to take care of it ourselves.
        https://jetbrains.github.io/xodus-dnq/properties.html
         */
        DbHint.all().toList().filter{hintsToDelIds.contains(it.entityId)}.forEach { it.delete() }
        DbTaskTemplateTarget.all().toList().filter{targetsToDelIds.contains(it.entityId)}.forEach{it.delete()}

        /*  Update task information: Remaining tasks. */
        apiEvaluationTemplate.tasks.forEachIndexed { idx, apiTask ->
            val task = if (apiTask.id != null) {
                dbEvaluationTemplate.tasks.filter { it.id eq apiTask.id }.firstOrNull()
                    ?: throw IllegalArgumentException("Unknown task ${apiTask.id} for evaluation ${apiEvaluationTemplate.id}.")
            } else {
                val t = DbTaskTemplate.new()
                dbEvaluationTemplate.tasks.add(t)
                t
            }

            /* Update parameters that do no require lookup. */
            task.name = apiTask.name
            task.duration = apiTask.duration
            task.comment = apiTask.comment
            task.idx = idx

            /* Conditional updating of parameters that do!. */
            if (task.isNew || task.collection.id != apiTask.collectionId) {
                try {
                    task.collection =
                        DbMediaCollection.query(DbMediaCollection::id eq apiTask.collectionId)
                            .first()
                }catch(ex: NoSuchElementException){
                    throw IllegalArgumentException("The provided collection could not be found. CollectionId: ${apiTask.collectionId}")
                }
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
                        else -> this.item =
                            target.target?.let { DbMediaItem.query(DbMediaItem::id eq it).firstOrNull() }
                    }
                })
            }

            /* Update task hints. */
            task.hints.clear()
            for (hint in apiTask.hints) {
                task.hints.add(DbHint.new {
                    this.type = hint.type.toDb()
                    this.item =
                        hint.item?.let { DbMediaItem.query(DbMediaItem::id eq hint.item.mediaItemId).firstOrNull() }
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
        val teamIds = apiEvaluationTemplate.teams.map { it.id }.toTypedArray()
        dbEvaluationTemplate.teams.removeAll(
            DbTeam.query(
                DbTeam::evaluation eq dbEvaluationTemplate and not(
                    DbTeam::id.containsIn(
                        *teamIds
                    )
                )
            )
        )
        for (apiTeam in apiEvaluationTemplate.teams) {
            val team = if (apiTeam.id != null) {
                dbEvaluationTemplate.teams.filter { it.id eq apiTeam.id }.firstOrNull()
                    ?: throw IllegalArgumentException("Unknown team ${apiTeam.id} for evaluation ${apiEvaluationTemplate.id}.")
            } else {
                val t = DbTeam.new()
                dbEvaluationTemplate.teams.add(t) /* Establish new relationship. */
                t
            }

            team.name = apiTeam.name ?: throw IllegalArgumentException("Team name must be specified.")
            team.color = apiTeam.color ?: throw IllegalArgumentException("Team colour must be specified.")

            /* Process logo data. */
            val logoData = apiTeam.logoStream()
            if (logoData != null) {
                team.logo = logoData
            }

            /* Make association with users. */
            val userIds = apiTeam.users.map { it.id }.toTypedArray()
            team.users.removeAll(DbUser.query(not(DbUser::id.containsIn(*userIds))))
            for (userId in userIds) {
                val user = DbUser.filter { it.id eq userId }.firstOrNull()
                    ?: throw IllegalArgumentException("Unknown user $userId for evaluation ${apiEvaluationTemplate.id}.")
                if (!team.users.contains(user)) {
                    team.users.add(user)
                }
            }
        }

        /* Update teamGroup information */
        val teamGroupIds = apiEvaluationTemplate.teamGroups.map { it.id }.toTypedArray()
        dbEvaluationTemplate.teamGroups.removeAll(
            DbTeamGroup.query(
                DbTeamGroup::evaluation eq dbEvaluationTemplate and not(
                    DbTeamGroup::id.containsIn(*teamGroupIds)
                )
            )
        )

        for (apiTeamGroup in apiEvaluationTemplate.teamGroups) {
            val teamGroup = if (apiTeamGroup.id != null) {
                dbEvaluationTemplate.teamGroups.filter { it.id eq apiTeamGroup.id }.firstOrNull()
                    ?: throw IllegalArgumentException("Unknown team group ${apiTeamGroup.id} for evaluation ${apiEvaluationTemplate.id}.")
            } else {
                val tg = DbTeamGroup.new()
                dbEvaluationTemplate.teamGroups.add(tg)
                tg
            }
            teamGroup.name = apiTeamGroup.name ?: throw IllegalArgumentException("Team group name must be specified.")
            teamGroup.defaultAggregator = apiTeamGroup.aggregation.toDb()

            teamGroup.teams.clear()
            teamGroup.teams.addAll(DbTeam.query(DbTeam::id.containsIn(*apiTeamGroup.teams.map { it.id }
                .toTypedArray())))

            /* Establish relationship if entry is new. */
            if (teamGroup.isNew) {
                dbEvaluationTemplate.teamGroups.add(teamGroup)
            }
        }

        /* Update judge information */
        val judgeIds = apiEvaluationTemplate.judges.toTypedArray()
        dbEvaluationTemplate.judges.removeAll(DbUser.query(not(DbUser::id.containsIn(*judgeIds))))
        for (userId in judgeIds) {
            val user = DbUser.filter { it.id eq userId }.firstOrNull()
                ?: throw IllegalArgumentException("Unknown judge user $userId for evaluation ${apiEvaluationTemplate.id}.")
            if (!dbEvaluationTemplate.judges.contains(user)) {
                dbEvaluationTemplate.judges.add(user)
            }
        }

        /* Update viewer information */
        val viewerIds = apiEvaluationTemplate.viewers.toTypedArray()
        dbEvaluationTemplate.viewers.removeAll(DbUser.query(not(DbUser::id.containsIn(*viewerIds))))
        for (userId in viewerIds){
            val user = DbUser.filter{it.id eq userId}.firstOrNull()
                ?: throw IllegalArgumentException("Unknown viewer user $userId for evaluation ${apiEvaluationTemplate.id}.")
            if( !(dbEvaluationTemplate.viewers.contains(user))){
                dbEvaluationTemplate.viewers.add(user)
            }
        }
    }

    /**
     * Creates a copy of an existing [DbEvaluationTemplate]
     */
    @Throws(IllegalArgumentException::class)
    fun copyTemplate(templateId: TemplateId): ApiEvaluationTemplate {
        val existing = getDbTemplate(templateId) ?: throw IllegalArgumentException("Template not found with id $templateId")

        val copy = this.store.transactional { copyTemplate(existing) }

        return getTemplate(copy)!!
    }

    /**
     * Creates a copy of an existing [DbEvaluationTemplate]
     */
    fun copyTemplate(dbEvaluationTemplate: DbEvaluationTemplate): TemplateId {

        val apiTemplate = dbEvaluationTemplate.toApi()
        val copy = apiTemplate.copy(
            name = "${apiTemplate.name} (copy)",
            tasks = apiTemplate.tasks.map {
                it.copy(id = null)
            },
            teams = apiTemplate.teams.map {
                it.copy(id = null, logoData = Base64.getEncoder().encodeToString(getTeamLogo(it.teamId)!!.readAllBytes()))
            }
        )

        val newTemplate = DbEvaluationTemplate.new()

        updateTemplate(newTemplate, copy)

        return newTemplate.templateId

    }

    fun prepareTemplate(templateId: TemplateId, cache: CacheManager) = this.store.transactional (true) {

        val template = getDbTemplate(templateId) ?: throw IllegalArgumentException("Template with id '$templateId' not found")

        val segmentTasks = template.getAllVideos()
        val await = segmentTasks.map {source ->

            when(source) {
                is DbEvaluationTemplate.VideoSource.ItemSource -> {
                    val item = source.item
                    val path = item.pathToOriginal()
                    if (!Files.exists(path)) {
                        throw IllegalStateException("Required media file $path not found for item ${item.name}.")
                    }

                    cache.asyncPreviewVideo(item, source.range.start.toMilliseconds(), source.range.end.toMilliseconds())
                }
                is DbEvaluationTemplate.VideoSource.PathSource -> {
                    val path = DRES.EXTERNAL_ROOT.resolve(source.path)
                    if (!Files.exists(path)) {
                        throw IllegalStateException("Required media file $path not found.")
                    }

                    cache.asyncPreviewVideo(path, source.range.start.toMilliseconds(), source.range.end.toMilliseconds())
                }
            }
        }

        println("start rendering ${segmentTasks.size} videos")

        await.all {
            try {
                val result = it.get(3, TimeUnit.MINUTES)
                println("completed rendering of $result")
                true
            } catch (e: Throwable) {
                throw IllegalStateException("Required media file could not be prepared: ${e.message}")
            }
        }


    }

}
