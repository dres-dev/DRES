package dev.dres.run

import dev.dres.api.rest.types.WebSocketConnection
import dev.dres.api.rest.types.run.websocket.ClientMessage
import dev.dres.api.rest.types.run.websocket.ServerMessage
import dev.dres.api.rest.types.run.websocket.ServerMessageType
import dev.dres.data.model.UID
import dev.dres.data.model.competition.CompetitionDescription
import dev.dres.data.model.competition.TaskDescription
import dev.dres.data.model.competition.TeamId
import dev.dres.data.model.run.*
import dev.dres.data.model.submissions.Submission
import dev.dres.data.model.submissions.SubmissionStatus
import dev.dres.run.score.ScoreTimePoint
import dev.dres.run.score.scoreboard.Scoreboard
import dev.dres.run.updatables.DAOUpdatable
import dev.dres.run.updatables.MessageQueueUpdatable
import dev.dres.run.updatables.ScoreboardsUpdatable
import dev.dres.run.updatables.ScoresUpdatable
import dev.dres.run.validation.interfaces.JudgementValidator
import org.slf4j.LoggerFactory
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.collections.HashMap
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.math.max

/**
 * An implementation of a [RunManager] aimed at distributed execution having a single DRES Server instance and
 * multiple clients connected via WebSocket.
 *
 * As opposed to the [InteractiveSynchronousRunManager], competitions in the [InteractiveAsynchronousRunManager]
 * can take place at different points in time for different teams and tasks, i.e., the competitions are executed
 * asynchronously.
 *
 * @version 1.0.0
 * @author Ralph Gasser
 */
class InteractiveAsynchronousRunManager(private val run: InteractiveAsynchronousCompetition) : InteractiveRunManager {

    companion object {
        private val LOGGER = LoggerFactory.getLogger(InteractiveAsynchronousRunManager::class.java)
        private val SCOREBOARD_UPDATE_INTERVAL_MS = 1000L // TODO make configurable
    }

    /** Tracks the current [TaskDescription] per [TeamId]. */
    private val navigationMap: MutableMap<TeamId, TaskDescription> = HashMap()

    /** Tracks the current [TaskDescription] per [TeamId]. */
    private val statusMap: MutableMap<TeamId, RunManagerStatus> = HashMap()

    /** A lock for state changes to this [InteractiveAsynchronousRunManager]. */
    private val stateLock = ReentrantReadWriteLock()

    /** The internal [ScoreboardsUpdatable] instance for this [InteractiveSynchronousRunManager]. */
    private val scoreboardsUpdatable = ScoreboardsUpdatable(this.competitionDescription.generateDefaultScoreboards(), SCOREBOARD_UPDATE_INTERVAL_MS, this.run)

    /** The internal [MessageQueueUpdatable] instance used by this [InteractiveSynchronousRunManager]. */
    private val messageQueueUpdatable = MessageQueueUpdatable(RunExecutor)

    /** The internal [ScoresUpdatable] instance for this [InteractiveSynchronousRunManager]. */
    private val scoresUpdatable = ScoresUpdatable(this.id, this.scoreboardsUpdatable, this.messageQueueUpdatable)

    /** The internal [DAOUpdatable] instance used by this [InteractiveSynchronousRunManager]. */
    private val daoUpdatable = DAOUpdatable(RunExecutor.runs, this.run)

    /** Run ID of this [InteractiveAsynchronousCompetition]. */
    override val id: UID
        get() = this.run.id

    /** Name of this [InteractiveAsynchronousCompetition]. */
    override val name: String
        get() = this.run.name

    /** The [CompetitionDescription] executed by this [InteractiveSynchronousRunManager]. */
    override val competitionDescription: CompetitionDescription
        get() = this.run.description

    override fun currentTaskDescription(context: RunActionContext): TaskDescription {
        require(context.teamId != null) { "TeamId missing from RunActionContext, which is required for interaction with InteractiveAsynchronousRunManager."}
        return this.navigationMap[context.teamId] ?: throw IllegalStateException("Could not find active task description for team ${context.teamId}.")
    }

    override val scoreHistory: List<ScoreTimePoint>
        get() = TODO("Not yet implemented")

    override val allSubmissions: List<Submission>
        get() = TODO("Not yet implemented")

    init {
        /* Initialize map and set all tasks pointers to the first task. */
        this.competitionDescription.teams.forEach {
            this.navigationMap[it.uid] = this.competitionDescription.tasks[0]
            this.statusMap[it.uid] = if (this.run.hasStarted) { RunManagerStatus.ACTIVE } else { RunManagerStatus.CREATED }
        }
    }


    override fun tasks(context: RunActionContext): List<AbstractInteractiveTask> {
        TODO("Not yet implemented")
    }

    /**
     * Prepares this [InteractiveAsynchronousCompetition] for the execution of previous [TaskDescription]
     * as per order defined in [CompetitionDescription.tasks]. Requires [RunManager.status] for the requesting team
     * to be [RunManagerStatus.ACTIVE].
     *
     * As all state affecting methods, this method throws an [IllegalStateException] if invocation
     * does not match the current state.
     *
     * @param context The [RunActionContext] used for the invocation.
     * @return True if [TaskDescription] was moved, false otherwise. Usually happens if last [TaskDescription] has been reached.
     * @throws IllegalStateException If [RunManager] was not in status [RunManagerStatus.ACTIVE]
     */
    override fun previous(context: RunActionContext): Boolean = this.stateLock.write {
        val newIndex = this.competitionDescription.tasks.indexOf(this.currentTaskDescription(context)) + 1
        return try {
            this.goTo(context, newIndex)
            true
        } catch (e: IndexOutOfBoundsException) {
            false
        }
    }

    /**
     * Prepares this [InteractiveAsynchronousCompetition] for the execution of next [TaskDescription]
     * as per order defined in [CompetitionDescription.tasks]. Requires [RunManager.status] for the requesting
     * team to be [RunManagerStatus.ACTIVE].
     *
     * As all state affecting methods, this method throws an [IllegalStateException] if invocation does not match the current state.
     *
     * @param context The [RunActionContext] used for the invocation.
     * @return True if [TaskDescription] was moved, false otherwise. Usually happens if last [TaskDescription] has been reached.
     * @throws IllegalStateException If [RunManager] was not in status [RunManagerStatus.ACTIVE]
     */
    override fun next(context: RunActionContext): Boolean = this.stateLock.write  {
        val newIndex = this.competitionDescription.tasks.indexOf(this.currentTaskDescription(context)) + 1
        return try {
            this.goTo(context, newIndex)
            true
        } catch (e: IndexOutOfBoundsException) {
            false
        }
    }

    /**
     * Prepares this [InteractiveAsynchronousCompetition] for the execution of [TaskDescription] with the given [index]
     * as per order defined in [CompetitionDescription.tasks]. Requires [RunManager.status] for the requesting
     * team to be [RunManagerStatus.ACTIVE].
     *
     * As all state affecting methods, this method throws an [IllegalStateException] if invocation does not match the current state.
     *
     * @param context The [RunActionContext] used for the invocation.
     * @return True if [TaskDescription] was moved, false otherwise. Usually happens if last [TaskDescription] has been reached.
     * @throws IllegalStateException If [RunManager] was not in status [RunManagerStatus.ACTIVE]
     */
    override fun goTo(context: RunActionContext, index: Int) = this.stateLock.write {
        require(context.teamId != null) { "TeamId is missing from action context, which is required for interaction with run manager."}
        check(this.statusMap[context.teamId] == RunManagerStatus.ACTIVE || this.status == RunManagerStatus.TASK_ENDED) { "Run manager for team ${context.teamId} is in status ${this.status}. Tasks can therefore not be changed." }
        if (index >= 0 && index < this.competitionDescription.tasks.size) {

            /* Update active task. */
            this.navigationMap[context.teamId] = this.competitionDescription.tasks[index]
            this.statusMap[context.teamId] = RunManagerStatus.ACTIVE

            /* Mark scoreboards for update. */
            this.scoreboardsUpdatable.dirty = true

            /* Enqueue WS message for sending */
            this.messageQueueUpdatable.enqueue(ServerMessage(this.id.string, ServerMessageType.COMPETITION_UPDATE))

            LOGGER.info("SynchronousRunManager ${this.id} set to task $index")
        } else {
            throw IndexOutOfBoundsException("Index $index is out of bounds for the number of available tasks.")
        }

    }

    override fun startTask(context: RunActionContext) = this.stateLock.write {
        require(context.teamId != null) { "TeamId is missing from action context, which is required for interaction with run manager."}
        check(this.statusMap[context.teamId] == RunManagerStatus.ACTIVE) { "Run manager for team ${context.teamId} is in status ${this.status}. New task cannot be started." }
        val currentTask = this.navigationMap[context.teamId]
        check(currentTask != null) { }

        /* Create and prepare pipeline for submission. */
        this.run.Task(teamId = context.teamId, descriptionId = this.navigationMap[context.teamId]!!.id)

        /* Update status. */
        this.statusMap[context.teamId] = RunManagerStatus.PREPARING_TASK

        /* Mark scoreboards and DAO for update. */
        this.scoreboardsUpdatable.dirty = true
        this.daoUpdatable.dirty = true

        LOGGER.info("SynchronousRunManager ${this.id} started task task $currentTask.")
    }

    override fun abortTask(context: RunActionContext) = this.stateLock.write {
        TODO("Not yet implemented")
    }

    override fun timeLeft(context: RunActionContext): Long = this.stateLock.read {
        require(context.teamId != null) { "TeamId is missing from action context, which is required for interaction with run manager."}
        if (this.statusMap[context.teamId] == RunManagerStatus.RUNNING_TASK) {
            val currentTaskRun = this.currentTask(context) ?: throw IllegalStateException("Run manager is in status ${this.status} but has no active task. This is a programmer's error!")
            return max(0L, currentTaskRun.duration * 1000L - (System.currentTimeMillis() - currentTaskRun.started!!))
        } else {
            -1L
        }
    }

    override fun currentTask(context: RunActionContext): AbstractInteractiveTask? {
        TODO("Not yet implemented")
    }

    override fun taskForId(
        context: RunActionContext,
        taskId: UID
    ): InteractiveSynchronousCompetition.Task? {
        TODO("Not yet implemented")
    }

    override fun submissions(context: RunActionContext): List<Submission> {
        TODO("Not yet implemented")
    }

    /**
     * Overriding the ready state is not supported by the [InteractiveAsynchronousRunManager]s.
     *
     * @return false
     */
    override fun overrideReadyState(context: RunActionContext, viewerId: String): Boolean {
        return false
    }

    override fun postSubmission(context: RunActionContext, sub: Submission): SubmissionStatus {
        TODO("Not yet implemented")
    }

    override fun updateSubmission(context: RunActionContext, submissionId: UID, submissionStatus: SubmissionStatus): Boolean {
        TODO("Not yet implemented")
    }

    /**
     * Adjusting task durations is not supported by the [InteractiveAsynchronousRunManager]s.
     *
     * @return Time left (see [timeLeft]).
     */
    override fun adjustDuration(context: RunActionContext, s: Int): Long = this.timeLeft(context)


    override val scoreboards: List<Scoreboard>
        get() = TODO("Not yet implemented")

    override val status: RunManagerStatus
        get() = TODO("Not yet implemented")

    override val judgementValidators: List<JudgementValidator>
        get() = TODO("Not yet implemented")

    override fun start(context: RunActionContext) {
        TODO("Not yet implemented")
    }

    override fun end(context: RunActionContext) {
        TODO("Not yet implemented")
    }

    override fun taskCount(context: RunActionContext): Int {
        TODO("Not yet implemented")
    }

    override fun viewers(): Map<WebSocketConnection, Boolean> {
        TODO("Not yet implemented")
    }

    override fun wsMessageReceived(connection: WebSocketConnection, message: ClientMessage): Boolean {
        TODO("Not yet implemented")
    }

    override fun run() {
        TODO("Not yet implemented")
    }
}