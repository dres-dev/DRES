package dev.dres.run.updatables

import dev.dres.data.model.run.CompetitionRun
import dev.dres.run.RunManager
import dev.dres.run.RunManagerStatus
import dev.dres.run.score.ScoreTimePoint
import dev.dres.run.score.scoreboard.Scoreboard

/**
 * This is a holder for all the [Scoreboard]s maintained by a [RunManager].
 * Implements the [Updatable] interface.
 *
 * @author Ralph Gasser
 * @version 1.0
 */
class ScoreboardsUpdatable(val scoreboards: List<Scoreboard>, private val run: CompetitionRun, private val timeSeries: MutableList<ScoreTimePoint>): StatefulUpdatable {

    companion object {
       val ELIGIBLE_STATUS = arrayOf(RunManagerStatus.ACTIVE, RunManagerStatus.RUNNING_TASK, RunManagerStatus.TASK_ENDED, RunManagerStatus.PREPARING_TASK)
    }

    /** The [Phase] this [ScoreboardsUpdatable] belongs to. */
    override val phase: Phase = Phase.MAIN

    @Volatile
    override var dirty: Boolean = false

    override fun update(status: RunManagerStatus) {
        if (this.dirty) {
            this.dirty = false
            this.scoreboards.forEach {
                it.update(this.run.runs)
                it.scores().map{ score -> this.timeSeries.add(ScoreTimePoint(it.name, score)) }
            }
        }
    }

    override fun shouldBeUpdated(status: RunManagerStatus): Boolean = ELIGIBLE_STATUS.contains(status)
}