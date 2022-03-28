package dev.dres.data.model.run

import dev.dres.data.model.UID
import dev.dres.data.model.competition.CompetitionDescription
import dev.dres.data.model.competition.TaskDescription
import dev.dres.data.model.competition.TaskDescriptionId
import dev.dres.data.model.competition.TeamId
import dev.dres.data.model.run.InteractiveAsynchronousCompetition.Task
import dev.dres.data.model.run.interfaces.Competition
import dev.dres.data.model.run.interfaces.CompetitionId
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
 * Represents a concrete, interactive and asynchronous [Run] of a [CompetitionDescription].
 *
 * [InteractiveAsynchronousCompetition]s can be started and ended and they can be used to create new [Task]s and access the current [Task].
 *
 * @author Ralph Gasser
 * @param 1.0.0
 */
class InteractiveAsynchronousCompetition(override var id: CompetitionId, override val name: String, override val description: CompetitionDescription, val permutation: Map<TeamId, List<Int>>): AbstractRun(), Competition {

    companion object {
        fun generatePermutation(description: CompetitionDescription) : Map<TeamId, List<Int>> = if (description.shuffleTasks || true) {
            description.teams.associate { it.uid to description.tasks.indices.toList().shuffled() }
        } else {
            description.teams.associate { it.uid to description.tasks.indices.toList() }
        }
    }

    constructor(id: CompetitionId, name: String, competitionDescription: CompetitionDescription) : this(id, name, competitionDescription, generatePermutation(competitionDescription))

    internal constructor(id: CompetitionId, name: String, competitionDescription: CompetitionDescription, started: Long, ended: Long, permutation: Map<TeamId, List<Int>>) : this(id, name, competitionDescription, permutation) {
        this.started = if (started == -1L) { null } else { started }
        this.ended = if (ended == -1L) { null } else { ended }
    }

    /** A [ConcurrentHashMap] that maps a list of [Task]s to the [TeamId]s they belong to.*/
    private val tasksMap = ConcurrentHashMap<TeamId,MutableList<Task>>()

    /** A [List] of all active [Task]s.*/
    override val tasks: List<Task>
        get() = this.tasksMap.values.flatten()

    /** Tracks the current [TaskDescription] per [TeamId]. */
    private val navigationMap: MutableMap<TeamId, TaskDescription> = HashMap()

    fun goTo(teamId: TeamId, index: Int) {
        navigationMap[teamId] = this.description.tasks[
                permutation[teamId]!![index]
        ]
    }

    fun currentTaskDescription(teamId: TeamId): TaskDescription = navigationMap[teamId] ?: throw IllegalTeamIdException(teamId)

    init {
        require(description.tasks.size > 0) { "Cannot create a run from a competition that doesn't have any tasks. "}
        require(description.teams.size > 0) { "Cannot create a run from a competition that doesn't have any teams. "}
        this.description.teams.forEach {
            this.tasksMap[it.uid] = ArrayList(this.description.tasks.size)
            this.navigationMap[it.uid] = this.description.tasks[0]
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
                val taskIndex = this.description.tasks.indexOf(lastTask.description)

                if (lastTask.ended != null) {
                    this.navigationMap[it.uid] = this.description.tasks[if (lastTask.descriptionId == description.tasks.last().id) description.tasks.size - 1 else taskIndex + 1]
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
    fun tasksForTeam(teamId: TeamId) = this.tasksMap[teamId] ?: throw IllegalArgumentException("Given $teamId is unknown to this competition $id.")

    /**
     * Generates and returns a [String] representation for this [InteractiveAsynchronousCompetition].
     */
    override fun toString(): String = "InteractiveAsynchronousCompetition(id=$id, name=${name})"

    /**
     * A [AbstractInteractiveTask] that takes place as part of the [InteractiveAsynchronousCompetition].
     *
     * @author Ralph Gasser
     * @version 1.0.0
     */
    inner class Task internal constructor (override val uid: TaskId = UID(), val teamId: TeamId, val descriptionId: TaskDescriptionId): AbstractInteractiveTask() {

        internal constructor(uid: TaskId, teamId: TeamId, taskId: TaskDescriptionId, started: Long, ended: Long): this(uid, teamId, taskId) {
            this.started =  if (started == -1L) { null } else { started }
            this.ended = if (ended == -1L) { null } else { ended }
        }

        /** The [InteractiveAsynchronousCompetition] this [Task] belongs to.*/
        override val competition: InteractiveAsynchronousCompetition
            get() = this@InteractiveAsynchronousCompetition

        /** The position of this [Task] within the [InteractiveAsynchronousCompetition]. */
        override val position: Int
            get() = this@InteractiveAsynchronousCompetition.tasksMap[this.teamId]?.indexOf(this) ?: -1

        @Transient
        override val description: TaskDescription = this@InteractiveAsynchronousCompetition.description.tasks.find { it.id == this.descriptionId }
            ?: throw IllegalArgumentException("Task with taskId ${this.descriptionId} not found.")

        @Transient
        override val filter: SubmissionFilter = this.description.newFilter()

        @Transient
        override val scorer: TeamTaskScorer = this.description.newScorer() as? TeamTaskScorer ?: throw IllegalArgumentException("specified scorer is not of type TeamTaskScorer")

        @Transient
        override val validator: SubmissionValidator = this.newValidator()

        /** The total duration in milliseconds of this task. Usually determined by the [TaskDescription] but can be adjusted! */
        override var duration: Long = this.description.duration


        init {
            check(this@InteractiveAsynchronousCompetition.description.teams.any { it.uid == this.teamId }) {
                "Cannot start a new task run for team with ID ${this.teamId}. Team is not registered for competition."
            }
            this@InteractiveAsynchronousCompetition.tasksMap.compute(this.teamId) { _, v ->
                val list = v ?: LinkedList<Task>()
                check(list.isEmpty() || list.last().hasEnded) { "Cannot create a new task. Another task is currently running." }
                list.add(this)
                list
            }
        }

        /**
         * Adds a [Submission] to this [InteractiveAsynchronousCompetition.Task].
         *
         * @param submission The [Submission] to add.
         * @throws IllegalArgumentException If [Submission] could not be added for any reason.
         */
        @Synchronized
        override fun addSubmission(submission: Submission) {
            check(this.isRunning) { "Task run '${this@InteractiveAsynchronousCompetition.name}.${this.position}' is currently not running. This is a programmer's error!" }
            check(this.teamId == submission.teamId) { "Team ${submission.teamId} is not eligible to submit to this task. This is a programmer's error!" }

            /* Execute submission filters. */
            this.filter.acceptOrThrow(submission)

            /* Process Submission. */
            this.submissions.add(submission)
            this.validator.validate(submission)
            AuditLogger.validateSubmission(submission, validator)
        }
    }
}