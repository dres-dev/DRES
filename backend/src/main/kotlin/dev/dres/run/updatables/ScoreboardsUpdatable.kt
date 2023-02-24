package dev.dres.run.updatables

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
class ScoreboardsUpdatable(val manager: RunManager, private val updateIntervalMs: Long): StatefulUpdatable {

    companion object {
       private val ELIGIBLE_STATUS = arrayOf(RunManagerStatus.ACTIVE)
    }

    /** The [Phase] this [ScoreboardsUpdatable] belongs to. */
    override val phase: Phase = Phase.MAIN

    @Volatile
    override var dirty: Boolean = true

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
            this.manager.store.transactional(true) {
                this.manager.scoreboards.forEach {
                    it.update()
                    it.scores().map{ score -> this._timeSeries.add(ScoreTimePoint(it.name, score)) }
                }
            }
        }
    }

    override fun shouldBeUpdated(status: RunManagerStatus): Boolean = ELIGIBLE_STATUS.contains(status)
}