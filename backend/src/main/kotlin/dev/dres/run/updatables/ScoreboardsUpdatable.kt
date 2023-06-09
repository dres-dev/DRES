package dev.dres.run.updatables

import dev.dres.api.rest.types.evaluation.ApiEvaluationState
import dev.dres.api.rest.types.evaluation.ApiTaskStatus
import dev.dres.data.model.run.RunActionContext
import dev.dres.run.RunManager
import dev.dres.run.RunManagerStatus
import dev.dres.run.score.ScoreTimePoint
import dev.dres.run.score.scoreboard.Scoreboard
import java.util.*

/**
 * This is a holder for all the [Scoreboard]s maintained by a [RunManager].
 * Implements the [Updatable] interface.
 *
 * @author Ralph Gasser & Luca Rossetto
 * @version 1.1.1
 */
class ScoreboardsUpdatable(val manager: RunManager, private val updateIntervalMs: Long): Updatable {

    /** The [Phase] this [ScoreboardsUpdatable] belongs to. */
    override val phase: Phase = Phase.MAIN

    /** Timestamp of the last update. */
    @Volatile
    private var lastUpdate: Long = System.currentTimeMillis()

    /** List of all [ScoreTimePoint]s tracked by this [ScoreboardsUpdatable]. */
    private val _timeSeries: MutableList<ScoreTimePoint> = LinkedList()

    val timeSeries: List<ScoreTimePoint>
        get() = this._timeSeries

    @Synchronized
    override fun update(runStatus: RunManagerStatus, taskStatus: ApiTaskStatus?, context: RunActionContext) {
        val now = System.currentTimeMillis()
        if ((now - this.lastUpdate) > this.updateIntervalMs) {
            this.lastUpdate = now
            this.manager.store.transactional(true) {
                this.manager.scoreboards.forEach {
                    it.update()
                    it.scores().map{ score -> this._timeSeries.add(ScoreTimePoint(it.name, score)) }
                }
            }
        }
    }

    /**
     * Returns true, if the [ScoreboardsUpdatable] should be updated given the [RunManagerStatus]
     * and the [ApiEvaluationState]. The [ScoreboardsUpdatable] is always triggered if the run is ongoing.
     *
     * @param runStatus The [RunManagerStatus] to check.
     * @param taskStatus The [ApiTaskStatus] to check. Can be null
     * @return True if update is required, which is while a task is running.
     */
    override fun shouldBeUpdated(runStatus: RunManagerStatus, taskStatus: ApiTaskStatus?): Boolean
        = runStatus == RunManagerStatus.ACTIVE
}