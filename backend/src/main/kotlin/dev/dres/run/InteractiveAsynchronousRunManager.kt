package dev.dres.run

import dev.dres.api.rest.AccessManager
import dev.dres.api.rest.types.ViewerInfo
import dev.dres.api.rest.types.evaluation.ApiTaskStatus
import dev.dres.api.rest.types.evaluation.submission.ApiClientSubmission
import dev.dres.api.rest.types.evaluation.submission.ApiVerdictStatus
import dev.dres.api.rest.types.template.ApiEvaluationTemplate
import dev.dres.api.rest.types.template.tasks.ApiTaskTemplate
import dev.dres.api.rest.types.template.tasks.options.ApiSubmissionOption
import dev.dres.api.rest.types.users.ApiRole
import dev.dres.data.model.template.DbEvaluationTemplate
import dev.dres.data.model.template.task.DbTaskTemplate
import dev.dres.data.model.run.*
import dev.dres.data.model.run.interfaces.TaskRun
import dev.dres.data.model.submissions.*
import dev.dres.data.model.template.task.TaskTemplateId
import dev.dres.data.model.template.task.options.DbSubmissionOption
import dev.dres.data.model.template.team.DbTeam
import dev.dres.data.model.template.team.TeamId
import dev.dres.run.RunManager.Companion.MAXIMUM_RUN_LOOP_ERROR_COUNT
import dev.dres.run.audit.AuditLogSource
import dev.dres.run.audit.AuditLogger
import dev.dres.run.exceptions.IllegalRunStateException
import dev.dres.run.exceptions.IllegalTeamIdException
import dev.dres.run.score.scoreboard.Scoreboard
import dev.dres.run.updatables.*
import dev.dres.run.validation.interfaces.JudgementValidator
import jetbrains.exodus.database.TransientEntityChange
import jetbrains.exodus.database.TransientEntityStore
import jetbrains.exodus.database.TransientStoreSession
import jetbrains.exodus.database.TransientStoreSessionListener
import jetbrains.exodus.database.exceptions.DataIntegrityViolationException
import kotlinx.dnq.query.*
import kotlinx.dnq.util.findById
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
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
 * @version 2.0.0
 * @author Ralph Gasser
 */
class InteractiveAsynchronousRunManager(
    override val evaluation: InteractiveAsynchronousEvaluation,
    override val store: TransientEntityStore
) : InteractiveRunManager, TransientStoreSessionListener {

    companion object {
        /** The [Logger] instance used by [InteractiveAsynchronousRunManager]. */
        private val LOGGER = LoggerFactory.getLogger(InteractiveAsynchronousRunManager::class.java)
    }

    /** Generates and returns [ApiRunProperties] for this [InteractiveAsynchronousRunManager]. */
    override val runProperties: ApiRunProperties
        get() = ApiRunProperties(
            this.evaluation.participantCanView,
            false,
            this.evaluation.allowRepeatedTasks,
            this.evaluation.limitSubmissionPreviews
        )

    private val teamIds = this.evaluation.template.teams.asSequence().map { it.teamId }.toList()

    /** Tracks the current [DbTaskTemplate] per [TeamId]. */
    private val statusMap: MutableMap<TeamId, RunManagerStatus> = HashMap()

    /** A [Map] of all viewers, i.e., DRES clients currently registered with this [InteractiveAsynchronousRunManager]. */
    private val viewers = ConcurrentHashMap<ViewerInfo, Boolean>()

    /** A lock for state changes to this [InteractiveAsynchronousRunManager]. */
    private val stateLock = ReentrantReadWriteLock()

    /** List of [Updatable] held by this [InteractiveAsynchronousRunManager]. */
    private val updatables = mutableListOf<Updatable>()

    /** Run ID of this [InteractiveAsynchronousEvaluation]. */
    override val id: SubmissionId
        get() = this.evaluation.id

    /** Name of this [InteractiveAsynchronousEvaluation]. */
    override val name: String
        get() = this.evaluation.name

    /** The [ApiEvaluationTemplate] executed by this [InteractiveSynchronousRunManager]. */
    override val template = this.evaluation.template


    /** The global [RunManagerStatus] of this [InteractiveAsynchronousRunManager]. */
    @Volatile
    override var status: RunManagerStatus = if (this.evaluation.hasStarted) {
        RunManagerStatus.ACTIVE
    } else {
        RunManagerStatus.CREATED
    }
        private set

    /** Returns list [JudgementValidator]s associated with this [InteractiveAsynchronousRunManager]. May be empty! */
    override val judgementValidators: List<JudgementValidator>
        get() = this.evaluation.taskRuns.mapNotNull { if (it.hasStarted && it.validator is JudgementValidator) it.validator else null }

    /** The list of [Scoreboard]s maintained by this [InteractiveAsynchronousEvaluation]. */
    override val scoreboards: List<Scoreboard>
        get() = this.evaluation.scoreboards

    init {
        /* Loads optional updatable. */
        this.registerOptionalUpdatables()

        this.teamIds.forEach {
            /* Initialize map and set all tasks pointers to the first task. */
            this.statusMap[it] = if (this.evaluation.hasStarted) {
                RunManagerStatus.ACTIVE
            } else {
                RunManagerStatus.CREATED
            }

            /** End ongoing runs upon initialization (in case server crashed during task execution). */
            if (this.evaluation.tasksForTeam(it).lastOrNull()?.isRunning == true) {
                this.evaluation.tasksForTeam(it).last().end()
            }
        }

        /** Trigger score updates and re-enqueue pending submissions for judgement (if any). */
        this.evaluation.taskRuns.forEach { task ->
            task.getDbSubmissions().forEach { sub ->
                for (answerSet in sub.answerSets.filter { v -> v.status eq DbVerdictStatus.INDETERMINATE }) {
                    task.validator.validate(answerSet)
                }
            }
        }
    }

    /**
     * Starts this [InteractiveAsynchronousEvaluation] moving [RunManager.status] from [RunManagerStatus.CREATED] to
     * [RunManagerStatus.ACTIVE] for the selected team.
     *
     * As all state affecting methods, this method throws an [IllegalStateException] if invocation
     * does not match the current state.
     *
     * @param context The [RunActionContext] for this invocation.
     * @throws IllegalStateException If [RunManager] was not in status [RunManagerStatus.CREATED]
     */
    override fun start(context: RunActionContext) = this.stateLock.write {
        checkGlobalStatus(RunManagerStatus.CREATED)
        if (context.isAdmin) {
            /* Start the run. */
            this.evaluation.start()

            /* Update status. */
            this.statusMap.forEach { (t, _) -> this.statusMap[t] = RunManagerStatus.ACTIVE }
            this.status = RunManagerStatus.ACTIVE

//            /* Enqueue WS message for sending */
//            RunExecutor.broadcastWsMessage(ServerMessage(this.id, ServerMessageType.COMPETITION_START))

            LOGGER.info("Run manager ${this.id} started")
        }
    }

    /**
     * Ends this [InteractiveAsynchronousEvaluation] moving [RunManager.status] from [RunManagerStatus.ACTIVE] to
     * [RunManagerStatus.TERMINATED] for selected team.
     *
     * As all state affecting methods, this method throws an [IllegalStateException] if invocation
     * does not match the current state.
     *
     * @param context The [RunActionContext] for this invocation.
     * @throws IllegalStateException If [RunManager] was not in status [RunManagerStatus.ACTIVE]
     */
    override fun end(context: RunActionContext) {
        checkGlobalStatus(RunManagerStatus.CREATED, RunManagerStatus.ACTIVE)//, RunManagerStatus.TASK_ENDED)
        if (context.isAdmin) {
            /* End the run. */
            this.evaluation.end()

            /* Update status. */
            this.statusMap.forEach { (t, _) -> this.statusMap[t] = RunManagerStatus.TERMINATED }
            this.status = RunManagerStatus.TERMINATED

//            /* Enqueue WS message for sending */
//            RunExecutor.broadcastWsMessage(ServerMessage(this.id, ServerMessageType.COMPETITION_END))

            LOGGER.info("SynchronousRunManager ${this.id} terminated")
        }
    }

    /**
     *
     */
    override fun updateProperties(properties: ApiRunProperties) {
        TODO("Not yet implemented")
    }

    /**
     * Returns the currently active [DbTaskTemplate] for the given team. Requires [RunManager.status] for the
     * requesting team to be [RunManagerStatus.ACTIVE].
     *
     * @param context The [RunActionContext] used for the invocation.
     * @return The [DbTaskTemplate] for the given team.
     */
    override fun currentTaskTemplate(context: RunActionContext): ApiTaskTemplate {
        val teamId = if (context is TeamRunActionContext) {
            context.teamId
        } else {
            context.teamId()
        }
        return this.evaluation.currentTaskTemplate(teamId)
    }

    /**
     * Prepares this [InteractiveAsynchronousEvaluation] for the execution of previous [DbTaskTemplate]
     * as per order defined in [DbEvaluationTemplate.tasks]. Requires [RunManager.status] for the requesting team
     * to be [RunManagerStatus.ACTIVE].
     *
     * As all state affecting methods, this method throws an [IllegalStateException] if invocation
     * does not match the current state.
     *
     * @param context The [RunActionContext] used for the invocation.
     * @return True if [DbTaskTemplate] was moved, false otherwise. Usually happens if last [DbTaskTemplate] has been reached.
     * @throws IllegalStateException If [RunManager] was not in status [RunManagerStatus.ACTIVE]
     */
    override fun previous(context: RunActionContext): Boolean = this.stateLock.write {
        val newIndex = this.template.tasks.indexOf(this.currentTaskTemplate(context)) - 1
        return try {
            this.goTo(context, newIndex)
            true
        } catch (e: IndexOutOfBoundsException) {
            false
        }
    }

    /**
     * Prepares this [InteractiveAsynchronousEvaluation] for the execution of next [DbTaskTemplate]
     * as per order defined in [DbEvaluationTemplate.tasks]. Requires [RunManager.status] for the requesting
     * team to be [RunManagerStatus.ACTIVE].
     *
     * As all state affecting methods, this method throws an [IllegalStateException] if invocation does not match the current state.
     *
     * @param context The [RunActionContext] used for the invocation.
     * @return True if [DbTaskTemplate] was moved, false otherwise. Usually happens if last [DbTaskTemplate] has been reached.
     * @throws IllegalStateException If [RunManager] was not in status [RunManagerStatus.ACTIVE]
     */
    override fun next(context: RunActionContext): Boolean = this.stateLock.write {
        val newIndex = this.template.tasks.indexOf(this.currentTaskTemplate(context)) + 1
        return try {
            this.goTo(context, newIndex)
            true
        } catch (e: IndexOutOfBoundsException) {
            false
        }
    }

    /**
     * Prepares this [InteractiveAsynchronousEvaluation] for the execution of [DbTaskTemplate] with the given [index]
     * as per order defined in [DbEvaluationTemplate.tasks]. Requires [RunManager.status] for the requesting
     * team to be [RunManagerStatus.ACTIVE].
     *
     * As all state affecting methods, this method throws an [IllegalStateException] if invocation does not match the current state.
     *
     * @param context The [RunActionContext] used for the invocation.
     * @return True if [DbTaskTemplate] was moved, false otherwise. Usually happens if last [DbTaskTemplate] has been reached.
     * @throws IllegalStateException If [RunManager] was not in status [RunManagerStatus.ACTIVE]
     */
    override fun goTo(context: RunActionContext, index: Int) = this.stateLock.write {
        val teamId = context.teamId()
        checkTeamStatus(teamId, RunManagerStatus.ACTIVE)//, RunManagerStatus.TASK_ENDED)
        require(!teamHasRunningTask(teamId)) { "Cannot change task while task is active" }

        val idx = (index + this.template.tasks.size) % this.template.tasks.size


        /* Update active task. */
        //this.run.navigationMap[context.teamId] = this.description.tasks[index]
        this.evaluation.goTo(teamId, idx)
        //FIXME since task run and competition run states are separated, this is not actually a state change
        this.statusMap[teamId] = RunManagerStatus.ACTIVE

        LOGGER.info("SynchronousRunManager ${this.id} set to task $idx")
    }

    /**
     * Starts the [currentTask] for the given team and thus moves the [InteractiveAsynchronousRunManager.status] from [RunManagerStatus.ACTIVE] to
     * either [RunManagerStatus.PREPARING_TASK] or [RunManagerStatus.RUNNING_TASK]
     *
     * TODO: Should a team be able to start the same task multiple times? How would this affect scoring?
     *
     * As all state affecting methods, this method throws an [IllegalStateException] if invocation oes not match the current state.
     *
     * @param context The [RunActionContext] used for the invocation.
     * @throws IllegalStateException If [InteractiveRunManager] was not in status [RunManagerStatus.ACTIVE] or [currentTask] is not set.
     */
    override fun startTask(context: RunActionContext) = this.stateLock.write {
        val teamId = context.teamId()
        checkTeamStatus(teamId, RunManagerStatus.ACTIVE)

        /* Create task and update status. */
        val currentTaskTemplate = this.evaluation.currentTaskTemplate(teamId)

        /* Check for duplicate task runs */
        if (!this.evaluation.allowRepeatedTasks && this.evaluation.tasksForTeam(teamId)
                .any { it.template.id == currentTaskTemplate.id }
        ) {
            throw IllegalStateException("Task '${currentTaskTemplate.name}' has already been used")
        }

        val dbTask = this.store.transactional {
            val dbTaskTemplate = DbTaskTemplate.filter { it.id eq currentTaskTemplate.id!! }.first()

            /* create task, needs to be persisted before run can be created */
            val dbTask = DbTask.new {
                status = DbTaskStatus.CREATED
                evaluation = this@InteractiveAsynchronousRunManager.evaluation.dbEvaluation
                template = dbTaskTemplate
                team = DbTeam.filter { it.id eq teamId }.firstOrNull()
            }

            dbTask

        }

        val currentTaskRun = store.transactional {
            val currentTaskRun = this.evaluation.IATaskRun(dbTask)
            currentTaskRun.prepare()
            currentTaskRun
        }

        LOGGER.info("Run manager  ${this.id} started task $currentTaskTemplate.")

        currentTaskRun.taskId
    }

    /**
     * Force-abort the [currentTask] and thus moves the [InteractiveAsynchronousRunManager.status] for the given team from
     * [RunManagerStatus.ACTIVE] or [RunManagerStatus.ACTIVE] to [RunManagerStatus.ACTIVE]
     *
     * TODO: Do we want users to be able to do this? If yes, I'd argue that the ability to repeat a task is a requirement.
     *
     * As all state affecting methods, this method throws an [IllegalStateException] if invocation does not match the current state.
     *
     * @param context The [RunActionContext] used for the invocation.
     * @throws IllegalStateException If [InteractiveRunManager] was not in status [RunManagerStatus.ACTIVE].
     */
    override fun abortTask(context: RunActionContext) = this.stateLock.write {
        val teamId = context.teamId()
        require(teamHasRunningTask(teamId)) { "No running task for Team ${teamId}" }

        /* End TaskRun and update status. */
        val currentTask = this.currentTask(context)
            ?: throw IllegalStateException("Could not find active task for team ${teamId} despite status of the team being ${this.statusMap[teamId]}. This is a programmer's error!")
        currentTask.end()

//        /* Enqueue WS message for sending */
//        RunExecutor.broadcastWsMessage(teamId, ServerMessage(this.id, ServerMessageType.TASK_END, currentTask.taskId))

        LOGGER.info("Run manager ${this.id} aborted task $currentTask.")
    }

    /**
     * Returns the time  in milliseconds that is left until the end of the currently running task for the given team.
     * Only works if the [InteractiveAsynchronousRunManager] is in state [RunManagerStatus.ACTIVE].
     * If no task is running, this method returns -1L.
     * If the task runs perpetually, this method returns NULL.
     *
     * @param context The [RunActionContext] used for the invocation.
     * @return Time remaining until the task will end or -1, if no task is running. NULL if the task runs perpetually.
     */
    override fun timeLeft(context: RunActionContext): Long? = this.stateLock.read {
        val currentTaskRun = this.currentTask(context)
        return if (currentTaskRun?.duration == null) {
            null
        } else {
            if (currentTaskRun.isRunning) {
                max(
                    0L,
                    currentTaskRun.duration!! * 1000L - (System.currentTimeMillis() - currentTaskRun.started!!) + InteractiveRunManager.COUNTDOWN_DURATION
                )
            } else {
                -1L
            }
        }
    }

    /**
     * Returns the time in milliseconds that has elapsed since the start of the current [DbTask].
     * Only works if the [RunManager] is in state [RunManagerStatus.ACTIVE]. If no task is running, this method returns -1L.
     *
     * @return Time remaining until the task will end or -1, if no task is running.
     */
    override fun timeElapsed(context: RunActionContext): Long = this.stateLock.read {
        val currentTaskRun = this.currentTask(context)
        return if (currentTaskRun?.isRunning == true) {
            System.currentTimeMillis() - (currentTaskRun.started!! + InteractiveRunManager.COUNTDOWN_DURATION)
        } else {
            -1L
        }
    }

    /**
     * Returns the number of  [AbstractInteractiveTask]s for this [InteractiveAsynchronousRunManager].
     *
     * Depending on the [RunActionContext], the number may vary.
     *
     * @param context The [RunActionContext] used for the invocation.
     * @return [List] of [AbstractInteractiveTask]s
     */
    override fun taskCount(context: RunActionContext): Int {
        if (context.isAdmin) return this.evaluation.taskRuns.size
        val teamId = context.teamId()
        return this.evaluation.tasksForTeam(teamId).size
    }

    /**
     * Returns a [List] of all [AbstractInteractiveTask]s for this [InteractiveAsynchronousRunManager].
     *
     * Depending on the [RunActionContext], the list may vary.
     *
     * @param context The [RunActionContext] used for the invocation.
     * @return [List] of [AbstractInteractiveTask]s
     */
    override fun tasks(context: RunActionContext): List<AbstractInteractiveTask> {
        if (context.isAdmin) return this.evaluation.taskRuns
        val teamId = context.teamId()
        return this.evaluation.tasksForTeam(teamId)
    }

    /**
     * Returns [AbstractInteractiveTask]s for a specific task [SubmissionId]. May be empty.
     *
     * @param context The [RunActionContext] used for the invocation.
     * @param taskId The [SubmissionId] of the [AbstractInteractiveTask].
     */
    override fun taskForId(context: RunActionContext, taskId: SubmissionId): AbstractInteractiveTask? =
        this.tasks(context).find { it.taskId == taskId }

    /**
     * Returns a reference to the currently active [InteractiveAsynchronousEvaluation.IATaskRun].
     *
     * @param context The [RunActionContext] used for the invocation.
     * @return [InteractiveAsynchronousEvaluation.IATaskRun] that is currently active or null, if no such task is active.
     */
    override fun currentTask(context: RunActionContext): InteractiveAsynchronousEvaluation.IATaskRun? =
        this.stateLock.read {
            val teamId = if (context is TeamRunActionContext) {
                context.teamId
            } else {
                context.teamId()
            }
            return this.evaluation.currentTaskForTeam(teamId)
        }

    /**
     * List of all [DbSubmission]s for this [InteractiveAsynchronousRunManager], irrespective of the [DbTask] it belongs to.
     *
     * @param context The [RunActionContext] used for the invocation.
     * @return List of [DbSubmission]s.
     */
    override fun allSubmissions(context: RunActionContext): List<DbSubmission> = this.stateLock.read {
        this.evaluation.taskRuns.flatMap { it.getDbSubmissions() }
    }

    /**
     * Returns the [DbSubmission]s for all currently active [AbstractInteractiveTask]s or an empty [List], if no such task is active.
     *
     * @param context The [RunActionContext] used for the invocation.
     * @return List of [DbSubmission]s.
     */
    override fun currentSubmissions(context: RunActionContext): List<DbSubmission> = this.stateLock.read {
        this.currentTask(context)?.getDbSubmissions()?.toList() ?: emptyList()
    }

    /**
     * Adjusting task durations is not supported by the [InteractiveAsynchronousRunManager]s.
     *
     */
    override fun adjustDuration(context: RunActionContext, s: Int): Long {
        val teamId = context.teamId()
        require(teamHasRunningTask(teamId)) { "No running task for Team $teamId." }

        val currentTaskRun = this.currentTask(context)
            ?: throw IllegalStateException("No active TaskRun found. This is a serious error!")
        check(currentTaskRun.duration != null){"The task '${currentTaskRun.template.name}' (${currentTaskRun.taskId}) runs perpetually."} // TODO what is the semantic of a perpetual IA task?
        val newDuration = currentTaskRun.duration!! + s
        check((newDuration * 1000L - (System.currentTimeMillis() - currentTaskRun.started!!)) > 0) { "New duration $s can not be applied because too much time has already elapsed." }
        currentTaskRun.duration = newDuration
        return (currentTaskRun.duration!! * 1000L - (System.currentTimeMillis() - currentTaskRun.started!!))

    }

    /**
     * Overriding the ready state is not supported by the [InteractiveAsynchronousRunManager]s.
     *
     * @return false
     */
    override fun overrideReadyState(context: RunActionContext, viewerId: String): Boolean {
        return false
    }

    /**
     * Invoked by an external caller to post a new [DbSubmission] for the [TaskRun] that is currently being
     * executed by this [InteractiveAsynchronousRunManager]. [DbSubmission]s usually cause updates to the
     * internal state and/or the [Scoreboard] of this [InteractiveRunManager].
     *
     * This method will not throw an exception and instead returns false if a [DbSubmission] was
     * ignored for whatever reason (usually a state mismatch). It is up to the caller to re-invoke
     * this method again.
     *
     * @param context The [RunActionContext] used for the invocation
     * @param submission The [DbSubmission] to be posted.
     *
     * @return [DbVerdictStatus] of the [DbSubmission]
     * @throws IllegalStateException If [InteractiveRunManager] was not in status [RunManagerStatus.ACTIVE].
     */
    override fun postSubmission(context: RunActionContext, submission: ApiClientSubmission) = this.stateLock.read {

        /* Phase 1: Basic lookups required for validation (read-only). */
        val (task, transformedSubmission) = this.store.transactional(true) {
            val teamId = context.teamId()

            require(teamHasRunningTask(teamId)) { "No running task for Team ${teamId}" }
            val task = this.currentTask(context)
                ?: throw IllegalStateException("Could not find ongoing task in run manager, despite being in status ${this.statusMap[teamId]}. This is a programmer's error!")

            /* Sanity check. */
            check(task.isRunning) { "Task run '${this.name}.${task.position}' is currently not running. This is a programmer's error!" }

            /* Update submission with contextual information. */
            submission.userId = context.userId
            submission.teamId = teamId
            submission.answerSets.forEach {
                it.taskId = task.taskId /* All answers are explicitly associated with the running task. */
            }

            /* Apply transformer(s) to submission. */
            val transformedSubmission = task.transformer.transform(submission)


            /* Run submission through all the filters. */
            task.filter.acceptOrThrow(transformedSubmission)
            task to transformedSubmission
        }

        /* Phase 2: Create DbSubmission, apply transformers and validate it. */
        return@read this.store.transactional {
            /* Convert submission to database representation. */
            val db = transformedSubmission.toNewDb()

            /* Check if there are answers left after transformation */
            if (db.answerSets.isEmpty) {
                throw IllegalStateException("Submission contains no valid answer sets.")
            }

            /* Apply validators to each answer set. */
            db.answerSets.asSequence().forEach {
                task.validator.validate(it)
            }

            db.toApi()
        }
    }

    override fun reScore(taskId: TaskId) {
        val task = evaluation.taskRuns.find { it.taskId == taskId }
        task?.scorer?.invalidate()
    }

    /**
     * Invoked by an external caller to update an existing [DbSubmission] by its [DbSubmission.submissionId] with a new [DbVerdictStatus].
     * [DbSubmission]s usually cause updates to the internal state and/or the [Scoreboard] of this [InteractiveAsynchronousRunManager].
     *
     * This method will not throw an exception and instead returns false if a [DbSubmission] was
     * ignored for whatever reason (usually a state mismatch). It is up to the caller to re-invoke
     * this method again.
     *
     * @param context The [RunActionContext] used for the invocation
     * @param submissionId The [SubmissionId] of the [DbSubmission] to update.
     * @param submissionStatus The new [DbVerdictStatus]
     *
     * @return Whether the update was successful or not
     */
    override fun updateSubmission(
        context: RunActionContext,
        submissionId: SubmissionId,
        submissionStatus: ApiVerdictStatus
    ): Boolean = this.stateLock.read {
        val answerSet = DbAnswerSet.filter { it.submission.submissionId eq submissionId }.singleOrNull() ?: return false

        /* Actual update - currently, only status update is allowed */
        val dbStatus = submissionStatus.toDb()
        if (answerSet.status != dbStatus) {
            answerSet.status = dbStatus
            return true
        }
        return false
    }

    /**
     * Returns a list of viewer [WebSocketConnection]s for this [InteractiveAsynchronousRunManager] alongside with their respective ready
     * state, which is always true for [InteractiveAsynchronousRunManager]
     *
     * @return List of viewer [WebSocketConnection]s for this [RunManager].
     */
    override fun viewers(): Map<ViewerInfo, Boolean> = Collections.unmodifiableMap(this.viewers)

    override fun viewerPreparing(taskTemplateId: TaskTemplateId, rac: RunActionContext, viewerInfo: ViewerInfo) {
        val currentTaskId = this.currentTask(rac)?.taskId
        if (taskTemplateId == currentTaskId) {
            this.viewers[viewerInfo] = false
        }
    }

    override fun viewerReady(taskTemplateId: TaskTemplateId, rac: RunActionContext, viewerInfo: ViewerInfo) {
        val currentTaskId = this.currentTask(rac)?.taskId
        if (taskTemplateId == currentTaskId) {
            this.viewers[viewerInfo] = true
        }
    }

    /**
     * Method that orchestrates the internal progression of the [InteractiveSynchronousEvaluation].
     *
     * Implements the main run-loop.
     */
    override fun run() {
        /* Preparation / Phase: PREPARE. */
        this.stateLock.read {
            this.store.transactional {
                this.updatables.sortBy { it.phase } /* Sort list of by [Phase] in ascending order. */
                AccessManager.registerRunManager(this) /* Register the run manager with the access manager. */
            }
        }

        /** Add this InteractiveAsynchronousRunManager as a listener for changes to the data store. */
        this.store.addListener(this)

        /* Initialize error counter. */
        var errorCounter = 0

        /* Start [InteractiveSynchronousRunManager]; main run-loop. */
        while (this.status != RunManagerStatus.TERMINATED) {
            try {
                this.stateLock.read {
                    this.store.transactional {
                        /* 1) Invoke all relevant [Updatable]s. */
                        this.invokeUpdatables()

                        /* 2) Process internal state updates (if necessary). */
                        this.internalStateUpdate()
                    }
                }

                /* 3) Yield to other threads. */
                Thread.sleep(500)

                /* 4) Reset error counter and yield to other threads. */
                errorCounter = 0
            } catch (ie: InterruptedException) {
                LOGGER.info("Interrupted SynchronousRunManager, exiting")
                return
            } catch (e: Throwable) {
                LOGGER.error(
                    "Uncaught exception in run loop for competition run ${this.id}. Loop will continue to work but this error should be handled!",
                    e
                )
                LOGGER.error("This is the ${++errorCounter}. in a row, will terminate loop after $MAXIMUM_RUN_LOOP_ERROR_COUNT errors")

                // oh shit, something went horribly, horribly wrong
                if (errorCounter >= MAXIMUM_RUN_LOOP_ERROR_COUNT) {
                    LOGGER.error("Reached maximum consecutive error count, terminating loop")
                    break //terminate loop
                }
            }
        }

        /* Remove this InteractiveAsynchronousRunManager as a listener for changes to the data store. */
        this.store.removeListener(this)

        /* Finalization. */
        this.stateLock.read {
            this.store.transactional {
                this.invokeUpdatables() /* Invoke [Updatable]s one last time. */
                AccessManager.deregisterRunManager(this) /* De-register this run manager with the access manager. */
            }
        }

        LOGGER.info("InteractiveAsynchronousRunManager ${this.id} has reached end of run logic.")
    }

    private data class TeamRunActionContext(val teamId: TeamId) : RunActionContext("", null, setOf(ApiRole.ADMIN))

    /**
     * Invokes all [Updatable]s registered with this [InteractiveSynchronousRunManager].
     */
    private fun invokeUpdatables() = this.stateLock.read {
        for (teamId in this.teamIds) {
            val runStatus = this.statusMap[teamId]
                ?: throw IllegalStateException("Run status for team $teamId is not set. This is a programmers error!")
            val taskStatus = this.evaluation.currentTaskForTeam(teamId)?.status
            for (updatable in this.updatables) {
                if (updatable.shouldBeUpdated(runStatus, taskStatus)) {
                    updatable.update(runStatus, taskStatus, TeamRunActionContext(teamId))
                }
            }
        }
    }

    /**
     * This is an internal method that facilitates internal state updates to this [InteractiveSynchronousRunManager],
     * i.e., status updates that are not triggered by an outside interaction.
     */
    private fun internalStateUpdate() = this.stateLock.read {
        for (teamId in teamIds) {
            if (teamHasRunningTask(teamId)) {
                this.stateLock.write {
                    val task = this.evaluation.currentTaskForTeam(teamId)
                        ?: throw IllegalStateException("Could not find active task for team $teamId despite status of the team being ${this.statusMap[teamId]}. This is a programmer's error!")
                    if(task.duration != null){
                        val timeLeft = max(
                            0L,
                            task.duration!! * 1000L - (System.currentTimeMillis() - task.started!!) + InteractiveRunManager.COUNTDOWN_DURATION
                        )
                        if (timeLeft <= 0) {
                            task.end()
                            AuditLogger.taskEnd(this.id, task.taskId, AuditLogSource.INTERNAL, null)

//                        /* Enqueue WS message for sending */
//                        RunExecutor.broadcastWsMessage(
//                            teamId,
//                            ServerMessage(this.id, ServerMessageType.TASK_END, task.taskId)
//                        )
                        }
                    }

                }
            } else if (teamHasPreparingTask(teamId)) {
                this.stateLock.write {
                    val task = this.evaluation.currentTaskForTeam(teamId)
                        ?: throw IllegalStateException("Could not find active task for team $teamId despite status of the team being ${this.statusMap[teamId]}. This is a programmer's error!")
                    task.start()
                    AuditLogger.taskStart(this.id, task.teamId, task.template, AuditLogSource.REST, null)
//                    RunExecutor.broadcastWsMessage(
//                        teamId,
//                        ServerMessage(this.id, ServerMessageType.TASK_START, task.taskId)
//                    )
                }
            }
        }
    }

    /**
     * Applies the [DbSubmissionOption.LIMIT_CORRECT_PER_TEAM].
     */
    private fun registerOptionalUpdatables() {
        /* Determine if task should be ended once submission threshold per team is reached. */
        val endOnSubmit = this.template.taskTypes.flatMap { it.submissionOptions }
            .contains(ApiSubmissionOption.LIMIT_CORRECT_PER_TEAM) //FIXME should take into account all limits
        if (endOnSubmit) {
            this.updatables.add(EndOnSubmitUpdatable(this))
        }
    }

    /**
     * Checks if the [InteractiveAsynchronousRunManager] is in one of the given [RunManagerStatus] and throws an exception, if not.
     *
     * @param status List of expected [RunManagerStatus].
     */
    private fun checkGlobalStatus(vararg status: RunManagerStatus) {
        if (this.status !in status) throw IllegalRunStateException(this.status)
    }

    /**
     * Checks if the team status for the given [TeamId] and this [InteractiveAsynchronousRunManager]
     * is in one of the given [RunManagerStatus] and throws an exception, if not.
     *
     * @param teamId The [TeamId] to check.
     * @param status List of expected [RunManagerStatus].
     */
    private fun checkTeamStatus(teamId: TeamId, vararg status: RunManagerStatus) {
        val s = this.statusMap[teamId] ?: throw IllegalTeamIdException(teamId)
        if (s !in status) throw IllegalRunStateException(s)
    }

    /**
     * Checks if the team for the given [TeamId] has an active and running task.
     *
     * @param teamId The [TeamId] to check.
     * @return True if task is running for team, false otherwise.
     */
    private fun teamHasRunningTask(teamId: TeamId) = this.evaluation.currentTaskForTeam(teamId)?.isRunning == true

    /**
     *
     */
    private fun teamHasPreparingTask(teamId: TeamId) =
        this.evaluation.currentTaskForTeam(teamId)?.status == ApiTaskStatus.PREPARING

    /**
     * Convenience method: Tries to find a matching [TeamId] in the context of this [InteractiveAsynchronousEvaluation]
     * for the user associated with the current [RunActionContext].
     *
     * @return [TeamId]
     */
    private fun RunActionContext.teamId(): TeamId {

        if (this is TeamRunActionContext) {
            return this.teamId
        }

        val userId = this.userId
        return this@InteractiveAsynchronousRunManager.template.teams.firstOrNull { team -> team.users.any { it.id == userId } }?.teamId
            ?: throw IllegalArgumentException("Could not find matching team for user, which is required for interaction with this run manager.")
    }

    override fun afterConstraintsFail(
        session: TransientStoreSession,
        exceptions: Set<DataIntegrityViolationException>
    ) {
        /* nop */
    }

    override fun beforeFlushBeforeConstraints(
        session: TransientStoreSession,
        changedEntities: Set<TransientEntityChange>
    ) {
        /* nop */
    }

    /**
     * [TransientStoreSessionListener] implementation: Reacts to changes to the [DbAnswerSet] entity and invalidates [Scoreboard]s and [Scorer]s if necessary.
     *
     * @param session The [TransientStoreSession] that triggered the invocation.
     * @param changedEntities The [TransientEntityChange]s for the transaction.
     */
    override fun flushed(session: TransientStoreSession, changedEntities: Set<TransientEntityChange>) {
        if (!session.isReadonly) {
            for (c in changedEntities) {
                if (c.transientEntity.persistentEntity.type == "DbAnswerSet") {
                    val xdId = c.transientEntity.persistentEntity.id.toString()
                    val task = DbAnswerSet.findById(xdId).task
                    if (this.evaluation.id == task.evaluation.id) {
                        val teamId = task.team?.teamId
                        if (teamId != null) {
                            val t = this.evaluation.tasksForTeam(teamId).find { it.taskId == task.id }
                            if (t != null) {
                                t.scorer.invalidate()
                                this.scoreboards.forEach { it.invalidate() }

                            }
                        }

                    }
                }
            }
        }
    }
}
