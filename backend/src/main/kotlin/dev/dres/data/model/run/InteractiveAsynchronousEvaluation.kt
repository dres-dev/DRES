package dev.dres.data.model.run

import dev.dres.api.rest.types.template.tasks.ApiTaskTemplate
import dev.dres.data.model.template.DbEvaluationTemplate
import dev.dres.data.model.template.task.DbTaskTemplate
import dev.dres.data.model.template.team.TeamId
import dev.dres.data.model.run.InteractiveAsynchronousEvaluation.IATaskRun
import dev.dres.data.model.run.interfaces.Run
import dev.dres.data.model.template.task.TaskTemplateId
import dev.dres.data.model.template.task.options.DbConfiguredOption
import dev.dres.data.model.template.task.options.DbScoreOption
import dev.dres.data.model.template.task.options.DbTaskOption
import dev.dres.run.exceptions.IllegalTeamIdException
import dev.dres.run.filter.basics.AcceptAllSubmissionFilter
import dev.dres.run.filter.basics.SubmissionFilter
import dev.dres.run.filter.basics.CombiningSubmissionFilter
import dev.dres.run.score.scoreboard.MaxNormalizingScoreBoard
import dev.dres.run.score.scoreboard.Scoreboard
import dev.dres.run.score.scorer.AvsTaskScorer
import dev.dres.run.score.scorer.CachingTaskScorer
import dev.dres.run.score.scorer.KisTaskScorer
import dev.dres.run.score.scorer.LegacyAvsTaskScorer
import dev.dres.run.transformer.MapToSegmentTransformer
import dev.dres.run.transformer.SubmissionTaskMatchTransformer
import dev.dres.run.transformer.basics.SubmissionTransformer
import dev.dres.run.transformer.basics.CombiningSubmissionTransformer
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.*
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Represents a concrete, interactive and asynchronous [Run] of a [DbEvaluationTemplate].
 *
 * [InteractiveAsynchronousEvaluation]s can be started and ended, and they can be used to create new [IATaskRun]s and access the current [IATaskRun].
 *
 */
class InteractiveAsynchronousEvaluation(store: TransientEntityStore, evaluation: DbEvaluation) :
    AbstractEvaluation(store, evaluation) {

    private val LOGGER = LoggerFactory.getLogger(InteractiveAsynchronousEvaluation::class.java)

    private val permutation: Map<TeamId, List<Int>>

    init {
        this.permutation = evaluation.permutation()!!
    }

    /** A [List] of all active [IATaskRun]s.*/
    override val taskRuns: List<IATaskRun>
        get() = this.tasksMap.values.flatten()

    /** A [ConcurrentHashMap] that maps a list of [IATaskRun]s to the [TeamId]s they belong to.*/
    private val tasksMap = ConcurrentHashMap<TeamId, MutableList<IATaskRun>>()

    /** Tracks the current [TaskTemplateId] per [TeamId]. */
    private val navigationMap: MutableMap<TeamId, ApiTaskTemplate> = HashMap()

    /** List of [Scoreboard]s maintained by this [NonInteractiveEvaluation]. */
    override val scoreboards: List<Scoreboard>

    init {
        /* Load all ongoing tasks. */
        this.dbEvaluation.tasks.asSequence().forEach {
            try{
                IATaskRun(it)
            } catch (e: Exception) {
                LOGGER.error("could not load task: ${e.message}")
            }
        }

        /* Prepare the evaluation scoreboards. */
        val teams = this.template.teams.asSequence().map { it.teamId }.toList()
        this.scoreboards = this.template.taskGroups.asSequence().map { group ->
            MaxNormalizingScoreBoard(group.name, this, teams, { task -> task.taskGroup == group.name }, group.name)
        }.toList()
    }

    fun goTo(teamId: TeamId, index: Int) {
        this.navigationMap[teamId] = this.template.tasks[this.permutation[teamId]!![index]]
    }

    fun currentTaskTemplate(teamId: TeamId): ApiTaskTemplate {
        return navigationMap[teamId] ?: throw IllegalTeamIdException(teamId)
    }

    init {
        val numberOfTasks = this.template.tasks.size
        require(numberOfTasks > 0) { "Cannot create a run from a competition that doesn't have any tasks. " }
        this.template.teams.forEach {
            this.tasksMap[it.id!!] = ArrayList(numberOfTasks)
            goTo(it.id, 0)
        }
    }

    /**
     * Returns the current [IATaskRun] for the given [TeamId].
     *
     * @param teamId The [TeamId] to lookup.
     */
    fun currentTaskForTeam(teamId: TeamId): IATaskRun? { //FIXME
        val currentTaskTemplateId = this.navigationMap[teamId]!!.id
        return this.tasksForTeam(teamId).findLast {
            it.template.id == currentTaskTemplateId
        }
    }

    /**
     * Returns all [IATaskRun]s for the given [TeamId].
     *
     * @param teamId The [TeamId] to lookup.
     * @return List []
     */
    fun tasksForTeam(teamId: TeamId) =
        this.tasksMap[teamId] ?: throw IllegalArgumentException("Given $teamId is unknown to this competition $id.")

    /**
     * Generates and returns a [String] representation for this [InteractiveAsynchronousEvaluation].
     */
    override fun toString(): String = "InteractiveAsynchronousCompetition(id=$id, name=${name})"

    /**
     * A [AbstractInteractiveTask] that takes place as part of the [InteractiveAsynchronousEvaluation].
     */
    inner class IATaskRun internal constructor(task: DbTask) : AbstractInteractiveTask(store, task) {

        init {
            /* Sanity check. */
            require(task.team != null) { "The task of an interactive asynchronous evaluation must be assigned to a single team." }
        }

//        /**
//         * Constructor used to generate an [IATaskRun] from a [DbTaskTemplate].
//         *
//         * @param t [DbTaskTemplate] to generate [IATaskRun] from.
//         * @param teamId The [TeamId] this [IATaskRun] is created for.
//         */
//        constructor(t: DbTaskTemplate, teamId: TeamId) : this(DbTask.new {
//            status = DbTaskStatus.CREATED
//            evaluation = this@InteractiveAsynchronousEvaluation.dbEvaluation
//            template = t
//            team = this@InteractiveAsynchronousEvaluation.dbEvaluation.template.teams.filter { it.teamId eq teamId }
//                .singleOrNull()
//                ?: throw IllegalArgumentException("Cannot start a new task run for team with ID ${teamId}. Team is not registered for competition.")
//        })

        /** The [InteractiveAsynchronousEvaluation] this [IATaskRun] belongs to.*/
        override val evaluationRun: InteractiveAsynchronousEvaluation
            get() = this@InteractiveAsynchronousEvaluation

        /** The position of this [IATaskRun] within the [InteractiveAsynchronousEvaluation]. */
        override val position: Int
            get() = this@InteractiveAsynchronousEvaluation.tasksMap[this.dbTask.team!!.id]?.indexOf(this) ?: -1

        /** The [SubmissionFilter] instance used by this [IATaskRun]. */
        override val filter: SubmissionFilter

        override val transformer: SubmissionTransformer

        /** The [CachingTaskScorer] instance used by this [InteractiveAsynchronousEvaluation].*/
        override val scorer: CachingTaskScorer

        /** The total duration in milliseconds of this task. Usually determined by the [DbTaskTemplate] but can be adjusted! */
        override var duration: Long = this.template.duration

        val teamId = this.dbTask.team!!.id

        /** The [List] of [TeamId]s working on this [IATaskRun]. */
        override val teams: List<TeamId> = listOf(teamId)


        init {
            this@InteractiveAsynchronousEvaluation.tasksMap.compute(this.dbTask.team!!.id) { _, v ->
                val list = v ?: LinkedList<IATaskRun>()
                check(list.isEmpty() || list.last().hasEnded) { "Cannot create a new task. Another task is currently running." } //FIXME crashes on restart
                list.add(this)
                list
            }

            /* Initialize submission filter. */
            this.filter = store.transactional {
                if (task.template.taskGroup.type.submission.isEmpty) {
                    AcceptAllSubmissionFilter
                } else {
                    CombiningSubmissionFilter(
                        task.template.taskGroup.type.submission.asSequence().map { option ->
                            val parameters =
                                task.template.taskGroup.type.configurations.query(DbConfiguredOption::key eq option.description)
                                    .asSequence().map { it.key to it.value }.toMap()
                            option.newFilter(parameters)
                        }.toList()
                    )
                }
            }

            this.transformer = store.transactional {
                if (task.template.taskGroup.type.options.asSequence().any { it == DbTaskOption.MAP_TO_SEGMENT }) {
                    CombiningSubmissionTransformer(
                        listOf(
                            SubmissionTaskMatchTransformer(this.taskId),
                            MapToSegmentTransformer()
                        )
                    )
                } else {
                    SubmissionTaskMatchTransformer(this.taskId)
                }
            }

            /* Initialize task scorer. */
            this.scorer = store.transactional {
                CachingTaskScorer(
                    when (val scoreOption = task.template.taskGroup.type.score) {
                        DbScoreOption.KIS -> KisTaskScorer(
                            this,
                            task.template.taskGroup.type.configurations.query(DbConfiguredOption::key eq scoreOption.description)
                                .asSequence().map { it.key to it.value }.toMap(),
                            store
                        )

                        DbScoreOption.AVS -> AvsTaskScorer(this, store)
                        DbScoreOption.LEGACY_AVS -> LegacyAvsTaskScorer(this, store)
                        else -> throw IllegalStateException("The task score option $scoreOption is currently not supported.")
                    }
                )
            }
        }
    }
}
