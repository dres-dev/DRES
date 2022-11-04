package dev.dres.data.model.run

import com.fasterxml.jackson.annotation.JsonIgnore
import dev.dres.data.model.template.EvaluationTemplate
import dev.dres.data.model.template.task.TaskTemplate
import dev.dres.data.model.template.TaskDescriptionId
import dev.dres.data.model.template.team.TeamId
import dev.dres.data.model.run.InteractiveAsynchronousEvaluation.Task
import dev.dres.data.model.run.interfaces.Run
import dev.dres.data.model.run.interfaces.TaskId
import dev.dres.data.model.submissions.Submission
import dev.dres.run.audit.AuditLogger
import dev.dres.run.exceptions.IllegalTeamIdException
import dev.dres.run.filter.SubmissionFilter
import dev.dres.run.score.interfaces.TeamTaskScorer
import dev.dres.run.validation.interfaces.SubmissionValidator
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Represents a concrete, interactive and asynchronous [Run] of a [EvaluationTemplate].
 *
 * [InteractiveAsynchronousEvaluation]s can be started and ended, and they can be used to create new [Task]s and access the current [Task].
 *
 */
class InteractiveAsynchronousEvaluation(evaluation: Evaluation, val permutation: Map<TeamId, List<Int>>) : AbstractEvaluation(evaluation) {

    companion object {
        fun generatePermutation(
            description: EvaluationTemplate,
            shuffle: Boolean
        ): Map<TeamId, List<Int>> =
            if (shuffle) {
                description.teams.associate { it.uid to makeLoop(description.tasks.size) }
            } else {
                description.teams.associate { it.uid to description.tasks.indices.toList() }
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

    constructor(
        id: EvaluationId,
        name: String,
        evaluationTemplate: EvaluationTemplate,
        properties: RunProperties
    ) : this(
        id,
        name,
        evaluationTemplate,
        properties,
        generatePermutation(evaluationTemplate, properties.shuffleTasks)
    )

    internal constructor(
        id: EvaluationId,
        name: String,
        evaluationTemplate: EvaluationTemplate,
        runProperties: RunProperties,
        started: Long,
        ended: Long,
        permutation: Map<TeamId, List<Int>>
    ) : this(id, name, evaluationTemplate, runProperties, permutation) {
        this.started = if (started == -1L) {
            null
        } else {
            started
        }
        this.ended = if (ended == -1L) {
            null
        } else {
            ended
        }
    }

    /** A [ConcurrentHashMap] that maps a list of [Task]s to the [TeamId]s they belong to.*/
    private val tasksMap = ConcurrentHashMap<TeamId, MutableList<Task>>()

    /** A [List] of all active [Task]s.*/
    override val tasks: List<Task>
        get() = this.tasksMap.values.flatten()

    /** Tracks the current [TaskTemplate] per [TeamId]. */
    private val navigationMap: MutableMap<TeamId, TaskTemplate> = HashMap()

    fun goTo(teamId: TeamId, index: Int) {
        navigationMap[teamId] = this.description.tasks[
                permutation[teamId]!![index]
        ]
    }

    fun currentTaskDescription(teamId: TeamId): TaskTemplate =
        navigationMap[teamId] ?: throw IllegalTeamIdException(teamId)

    init {
        require(description.tasks.size > 0) { "Cannot create a run from a competition that doesn't have any tasks. " }
        this.description.teams.forEach {
            this.tasksMap[it.uid] = ArrayList(this.description.tasks.size)
            goTo(it.uid, 0)
        }
    }

    /**
     * When a run is deserialized, the pointers for individual teams need to be recalculated in order to be able to resume where they left of
     */
    fun reconstructNavigationMap() {
        this.description.teams.forEach {
            val tasks = this.tasksMap[it.uid]
            if (tasks != null && tasks.isNotEmpty()) {
                val lastTask = tasks.last()
                val taskIndex = this.description.tasks.indexOf(lastTask.template)

                if (lastTask.ended != null) {
                    this.navigationMap[it.uid] =
                        this.description.tasks[if (lastTask.descriptionId == description.tasks.last().id) description.tasks.size - 1 else taskIndex + 1]
                } else {
                    this.navigationMap[it.uid] = this.description.tasks[taskIndex]
                }

            }
        }
    }

    /**
     * Returns the current [Task] for the given [TeamId].
     *
     * @param teamId The [TeamId] to lookup.
     */
    fun currentTaskForTeam(teamId: TeamId): Task? {

        val currentTaskDescriptionId = navigationMap[teamId]!!.id

        return this.tasksForTeam(teamId).findLast {
            it.descriptionId == currentTaskDescriptionId
        }

    }

    /**
     * Returns all [Task]s for the given [TeamId].
     *
     * @param teamId The [TeamId] to lookup.
     * @return List []
     */
    fun tasksForTeam(teamId: TeamId) =
        this.tasksMap[teamId]
            ?: throw IllegalArgumentException("Given $teamId is unknown to this competition $id.")

    /**
     * Generates and returns a [String] representation for this [InteractiveAsynchronousEvaluation].
     */
    override fun toString(): String = "InteractiveAsynchronousCompetition(id=$id, name=${name})"

    /**
     * A [AbstractInteractiveTask] that takes place as part of the [InteractiveAsynchronousEvaluation].
     *
     * @author Ralph Gasser
     * @version 1.0.0
     */
    inner class Task internal constructor(
        override val uid: TaskId = EvaluationId(),
        val teamId: TeamId,
        val descriptionId: TaskDescriptionId
    ) : AbstractInteractiveTask() {

        internal constructor(
            uid: TaskId,
            teamId: TeamId,
            taskId: TaskDescriptionId,
            started: Long,
            ended: Long
        ) : this(
            uid,
            teamId,
            taskId
        ) {
            this.started = if (started == -1L) {
                null
            } else {
                started
            }
            this.ended = if (ended == -1L) {
                null
            } else {
                ended
            }
        }

        /** The [InteractiveAsynchronousEvaluation] this [Task] belongs to.*/
        override val competition: InteractiveAsynchronousEvaluation
            @JsonIgnore get() = this@InteractiveAsynchronousEvaluation

        /** The position of this [Task] within the [InteractiveAsynchronousEvaluation]. */
        override val position: Int
            get() = this@InteractiveAsynchronousEvaluation.tasksMap[this.teamId]?.indexOf(this)
                ?: -1

        @Transient
        override val template: TaskTemplate =
            this@InteractiveAsynchronousEvaluation.description.tasks.find { it.id == this.descriptionId }
                ?: throw IllegalArgumentException("Task with taskId ${this.descriptionId} not found.")

        @Transient
        override val filter: SubmissionFilter = this.template.newFilter()

        @Transient
        override val scorer: TeamTaskScorer = this.template.newScorer() as? TeamTaskScorer
            ?: throw IllegalArgumentException("specified scorer is not of type TeamTaskScorer")

        @Transient
        override val validator: SubmissionValidator = this.newValidator()

        /** The total duration in milliseconds of this task. Usually determined by the [TaskTemplate] but can be adjusted! */
        override var duration: Long = this.template.duration


        init {
            check(this@InteractiveAsynchronousEvaluation.description.teams.any { it.uid == this.teamId }) {
                "Cannot start a new task run for team with ID ${this.teamId}. Team is not registered for competition."
            }
            this@InteractiveAsynchronousEvaluation.tasksMap.compute(this.teamId) { _, v ->
                val list = v ?: LinkedList<Task>()
                check(list.isEmpty() || list.last().hasEnded) { "Cannot create a new task. Another task is currently running." }
                list.add(this)
                list
            }
        }

        /**
         * Adds a [Submission] to this [InteractiveAsynchronousEvaluation.Task].
         *
         * @param submission The [Submission] to add.
         * @throws IllegalArgumentException If [Submission] could not be added for any reason.
         */
        @Synchronized
        override fun postSubmission(submission: Submission) {
            check(this.isRunning) { "Task run '${this@InteractiveAsynchronousEvaluation.name}.${this.position}' is currently not running. This is a programmer's error!" }
            check(this.teamId == submission.teamId) { "Team ${submission.teamId} is not eligible to submit to this task. This is a programmer's error!" }

            /* Execute submission filters. */
            this.filter.acceptOrThrow(submission)

            /* Process Submission. */
            this.submissions.add(submission)
            this.validator.validate(submission)
            AuditLogger.validateSubmission(submission, this.validator)
        }
    }
}
