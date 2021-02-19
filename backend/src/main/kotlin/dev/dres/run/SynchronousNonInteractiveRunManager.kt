package dev.dres.run

import dev.dres.api.rest.types.WebSocketConnection
import dev.dres.api.rest.types.run.websocket.ClientMessage
import dev.dres.api.rest.types.run.websocket.ClientMessageType
import dev.dres.data.model.UID
import dev.dres.data.model.competition.CompetitionDescription
import dev.dres.data.model.run.BaseSubmissionBatch
import dev.dres.data.model.run.NonInteractiveCompetitionRun
import dev.dres.data.model.run.SubmissionStatus
import dev.dres.run.score.scoreboard.Scoreboard
import dev.dres.run.updatables.ScoreboardsUpdatable
import dev.dres.run.validation.interfaces.JudgementValidator
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read

class SynchronousNonInteractiveRunManager(val run: NonInteractiveCompetitionRun) : NonInteractiveRunManager {

    private val SCOREBOARD_UPDATE_INTERVAL_MS = 10_000L // TODO make configurable

    /** A lock for state changes to this [SynchronousInteractiveRunManager]. */
    private val stateLock = ReentrantReadWriteLock()

    /** Run ID of this [SynchronousInteractiveRunManager]. */
    override val id: UID
        get() = this.run.id

    /** Name of this [SynchronousInteractiveRunManager]. */
    override val name: String
        get() = this.run.name

    /** The [CompetitionDescription] executed by this [SynchronousInteractiveRunManager]. */
    override val competitionDescription: CompetitionDescription
        get() = this.run.competitionDescription

    /** The internal [ScoreboardsUpdatable] instance for this [SynchronousInteractiveRunManager]. */
    private val scoreboardsUpdatable = ScoreboardsUpdatable(this.competitionDescription.generateDefaultScoreboards(), SCOREBOARD_UPDATE_INTERVAL_MS, this.run)

    override val scoreboards: List<Scoreboard>
        get() = this.scoreboardsUpdatable.scoreboards

    @Volatile
    override var status: RunManagerStatus = if (this.run.hasStarted) {
        RunManagerStatus.ACTIVE
    } else {
        RunManagerStatus.CREATED
    }
        get() = this.stateLock.read {
            return field
        }
        private set

    override val judgementValidators: List<JudgementValidator>
        get() = this.run.tasks.map{ it.validator }.filterIsInstance(JudgementValidator::class.java)

    override fun start() {
        TODO("Not yet implemented")
    }

    override fun end() {
        TODO("Not yet implemented")
    }

    override fun tasks(): Int = this.run.tasks.size

    private val viewerMap: MutableMap<WebSocketConnection, Boolean> = mutableMapOf()

    override fun viewers(): Map<WebSocketConnection, Boolean> = viewerMap

    override fun wsMessageReceived(connection: WebSocketConnection, message: ClientMessage): Boolean {
        when (message.type) {
            ClientMessageType.REGISTER -> this.viewerMap[connection] = true
            ClientMessageType.UNREGISTER -> this.viewerMap.remove(connection)
            ClientMessageType.ACK, ClientMessageType.PING -> {} //nop
        }
        return true
    }

    override fun updateSubmission(suid: UID, newStatus: SubmissionStatus): Boolean {
        TODO("Not yet implemented")
    }

    override fun run() {
        TODO("Not yet implemented")
    }

    override fun addSubmissionBatch(batch: BaseSubmissionBatch<*>) {
        TODO("Not yet implemented")
    }
}