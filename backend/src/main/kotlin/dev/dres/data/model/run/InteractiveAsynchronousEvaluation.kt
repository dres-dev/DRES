package dev.dres.data.model.run

import dev.dres.data.model.template.DbEvaluationTemplate
import dev.dres.data.model.template.task.DbTaskTemplate
import dev.dres.data.model.template.team.TeamId
import dev.dres.data.model.run.InteractiveAsynchronousEvaluation.IATaskRun
import dev.dres.data.model.run.interfaces.Run
import dev.dres.data.model.submissions.DbSubmission
import dev.dres.data.model.submissions.Submission
import dev.dres.run.audit.DbAuditLogger
import dev.dres.run.exceptions.IllegalTeamIdException
import dev.dres.run.filter.SubmissionFilter
import kotlinx.dnq.query.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Represents a concrete, interactive and asynchronous [Run] of a [DbEvaluationTemplate].
 *
 * [InteractiveAsynchronousEvaluation]s can be started and ended, and they can be used to create new [IATaskRun]s and access the current [IATaskRun].
 *
 */
class InteractiveAsynchronousEvaluation(evaluation: DbEvaluation, private val permutation: Map<TeamId, List<Int>>) : AbstractEvaluation(evaluation) {

    companion object {
        fun generatePermutation(description: DbEvaluationTemplate, shuffle: Boolean): Map<TeamId, List<Int>> =
            if (shuffle) {
                description.teams.asSequence().associate { it.id to makeLoop(description.tasks.size()) }
            } else {
                description.teams.asSequence().associate { it.id to description.tasks.asSequence().toList().indices.toList() }
            }

        /**
         * generates a sequence of tasks that loops through all tasks exactly once
         */
        private fun makeLoop(length: Int): List<Int> {
            if (length <= 0) {
                return emptyList()
            }
            val positions = (0 until length).shuffled()
            val array = IntArray(length) { -1 }

            fun recursionStep(open: List<Int>, idx: Int): Boolean {

                //nothing left to do
                if (open.isEmpty()) {
                    return true
                }

                //invalid state, need to backtrack
                if (array[idx] != -1) {
                    return false
                }

                //for all remaining options...
                for (nextPosition in open) {
                    //...assign the next one...
                    array[idx] = nextPosition
                    //...and continue recursively
                    if (recursionStep(
                            (open - nextPosition), //without the last assigned value
                            (nextPosition + 1) % array.size
                        ) //at the index after the last assigned position
                    ) {
                        //assignment succeeded
                        return true
                    }
                }

                //there was no valid assignment in the given options, need to back track
                array[idx] = -1
                return false
            }

            if (!recursionStep(positions, 0)) {
                error("Error during generation of task sequence")
            }

            return array.toList()
        }
    }

    /**
     * Internal constructor to create an [InteractiveAsynchronousEvaluation] from an [DbEvaluationTemplate].
     * Requires a transaction context!
     *
     * @param name The name of the new [InteractiveSynchronousEvaluation]
     * @param shuffle Flag indicating if [IATaskRun]s should be shuffled.
     * @param template The [DbEvaluationTemplate]
     */
    constructor(name: String, shuffle: Boolean, template: DbEvaluationTemplate) : this(DbEvaluation.new {
        this.type = DbEvaluationType.INTERACTIVE_ASYNCHRONOUS
        this.name = name
        this.template = template
        this.shuffleTasks = shuffle
        this.started = System.currentTimeMillis()
    }, generatePermutation(template, shuffle))

    /** A [List] of all active [IATaskRun]s.*/
    override val tasks: List<IATaskRun>
        get() = this.tasksMap.values.flatten()

    /** A [ConcurrentHashMap] that maps a list of [IATaskRun]s to the [TeamId]s they belong to.*/
    private val tasksMap = ConcurrentHashMap<TeamId, MutableList<IATaskRun>>()

    /** Tracks the current [DbTaskTemplate] per [TeamId]. */
    private val navigationMap: MutableMap<TeamId, DbTaskTemplate> = HashMap()

    init {
        /* TODO: Reconstruct TaskRuns from stored data. */
    }

    fun goTo(teamId: TeamId, index: Int) {
        this.navigationMap[teamId] = this.description.tasks.drop(this.permutation[teamId]!![index]).single()
    }

    fun currentTaskDescription(teamId: TeamId): DbTaskTemplate =
        navigationMap[teamId] ?: throw IllegalTeamIdException(teamId)

    init {
        val numberOfTasks = this.description.tasks.size()
        require(numberOfTasks > 0) { "Cannot create a run from a competition that doesn't have any tasks. " }
        this.description.teams.asSequence().forEach {
            this.tasksMap[it.id] = ArrayList(numberOfTasks)
            goTo(it.id, 0)
        }
    }

    /**
     * Returns the current [IATaskRun] for the given [TeamId].
     *
     * @param teamId The [TeamId] to lookup.
     */
    fun currentTaskForTeam(teamId: TeamId): IATaskRun? {
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
    fun tasksForTeam(teamId: TeamId)
        = this.tasksMap[teamId] ?: throw IllegalArgumentException("Given $teamId is unknown to this competition $id.")

    /**
     * Generates and returns a [String] representation for this [InteractiveAsynchronousEvaluation].
     */
    override fun toString(): String = "InteractiveAsynchronousCompetition(id=$id, name=${name})"

    /**
     * A [AbstractInteractiveTask] that takes place as part of the [InteractiveAsynchronousEvaluation].
     */
    inner class IATaskRun internal constructor(task: DbTask, val teamId: TeamId) : AbstractInteractiveTask(task) {

        /**
         * Constructor used to generate an [IATaskRun] from a [DbTaskTemplate].
         *
         * @param template [DbTaskTemplate] to generate [IATaskRun] from.
         * @param teamId The [TeamId] this [IATaskRun] is created for.
         */
        internal constructor(template: DbTaskTemplate, teamId: TeamId) : this(DbTask.new {
            this.evaluation = this@InteractiveAsynchronousEvaluation.evaluation
            this.template = template
            this.team = this@InteractiveAsynchronousEvaluation.evaluation.template.teams.filter { it.teamId eq teamId }.singleOrNull()
                ?: throw IllegalArgumentException("Cannot start a new task run for team with ID ${teamId}. Team is not registered for competition.")
        }, teamId)

        /** The [InteractiveAsynchronousEvaluation] this [IATaskRun] belongs to.*/
        override val competition: InteractiveAsynchronousEvaluation
            get() = this@InteractiveAsynchronousEvaluation

        /** The position of this [IATaskRun] within the [InteractiveAsynchronousEvaluation]. */
        override val position: Int
            get() = this@InteractiveAsynchronousEvaluation.tasksMap[this.teamId]?.indexOf(this) ?: -1

        /** The [SubmissionFilter] instance used by this [IATaskRun]. */
        override val filter: SubmissionFilter = this.template.newFilter()

        /** The [TeamTaskScorer] instance used by this [InteractiveAsynchronousEvaluation].*/
        override val scorer = this.template.newScorer()
            ?: throw IllegalArgumentException("specified scorer is not of type TeamTaskScorer")

        /** The total duration in milliseconds of this task. Usually determined by the [DbTaskTemplate] but can be adjusted! */
        override var duration: Long = this.template.duration

        init {
            this@InteractiveAsynchronousEvaluation.tasksMap.compute(this.teamId) { _, v ->
                val list = v ?: LinkedList<IATaskRun>()
                check(list.isEmpty() || list.last().hasEnded) { "Cannot create a new task. Another task is currently running." }
                list.add(this)
                list
            }
        }

        /**
         * Adds a [DbSubmission] to this [InteractiveAsynchronousEvaluation.IATaskRun].
         *
         * @param submission The [DbSubmission] to add.
         * @throws IllegalArgumentException If [DbSubmission] could not be added for any reason.
         */
        @Synchronized
        override fun postSubmission(submission: Submission) {
            check(this.isRunning) { "Task run '${this@InteractiveAsynchronousEvaluation.name}.${this.position}' is currently not running. This is a programmer's error!" }
            check(this.teamId == submission.teamId) { "Team ${submission.teamId} is not eligible to submit to this task. This is a programmer's error!" }

            /* Execute submission filters. */
            this.filter.acceptOrThrow(submission)

            /* At this point, the submission is considered valid and is persisted */
            val dbSubmission: DbSubmission = submission.toDb()

            /* Process Submission. */
            this.validator.validate(submission)
            DbAuditLogger.validateSubmission(submission, this.validator)
        }
    }
}
