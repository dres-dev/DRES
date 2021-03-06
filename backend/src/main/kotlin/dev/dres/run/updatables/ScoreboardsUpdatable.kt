package dev.dres.run.updatables

import dev.dres.data.model.run.AbstractInteractiveTask
import dev.dres.data.model.run.interfaces.Competition
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
 * @version 1.1.0
 */
class ScoreboardsUpdatable(val scoreboards: List<Scoreboard>, private val updateIntervalMs: Long, private val competition: Competition): StatefulUpdatable {

    companion object {
       val ELIGIBLE_STATUS = arrayOf(RunManagerStatus.ACTIVE, RunManagerStatus.RUNNING_TASK, RunManagerStatus.TASK_ENDED, RunManagerStatus.PREPARING_TASK)
    }

    /** The [Phase] this [ScoreboardsUpdatable] belongs to. */
    override val phase: Phase = Phase.MAIN

    @Volatile
    override var dirty: Boolean = false

    /** Timestamp of the last update. */
    private var lastUpdate: Long = System.currentTimeMillis()

    /** List of all [ScoreTimePoint]s tracked by this [ScoreboardsUpdatable]. */
    private val _timeSeries: MutableList<ScoreTimePoint> = LinkedList()
    val timeSeries: List<ScoreTimePoint>
        get() = this._timeSeries

    override fun update(status: RunManagerStatus) {
        val now = System.currentTimeMillis()
        if (this.dirty && (now - lastUpdate) > this.updateIntervalMs) {
            this.dirty = false
            this.lastUpdate = now
            this.scoreboards.forEach {
                it.update(this.competition.tasks.filterIsInstance<AbstractInteractiveTask>())
                it.scores().map{ score -> this._timeSeries.add(ScoreTimePoint(it.name, score)) }
            }
        }
    }

    override fun shouldBeUpdated(status: RunManagerStatus): Boolean = ELIGIBLE_STATUS.contains(status)
}