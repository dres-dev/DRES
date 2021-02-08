package dev.dres.run

import dev.dres.api.rest.types.WebSocketConnection
import dev.dres.api.rest.types.run.websocket.ClientMessage
import dev.dres.data.model.UID
import dev.dres.data.model.competition.CompetitionDescription
import dev.dres.data.model.run.BaseSubmissionBatch
import dev.dres.data.model.run.NonInteractiveCompetitionRun
import dev.dres.data.model.run.Submission
import dev.dres.data.model.run.SubmissionStatus
import dev.dres.run.score.ScoreTimePoint
import dev.dres.run.score.scoreboard.Scoreboard
import dev.dres.run.validation.interfaces.JudgementValidator
import java.util.*
import java.util.concurrent.locks.ReentrantReadWriteLock

class SynchronousNonInteractiveRunManager(val run: NonInteractiveCompetitionRun) : NonInteractiveRunManager {

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


    override val scoreboards: List<Scoreboard>
        get() = TODO("Not yet implemented")

    override val scoreHistory: List<ScoreTimePoint>
        get() = TODO("Not yet implemented")


    override val allSubmissions: List<Submission>
        get() = TODO("Not yet implemented")

    override val status: RunManagerStatus
        get() = TODO("Not yet implemented")

    override val judgementValidators: List<JudgementValidator>
        get() = TODO("Not yet implemented")

    override fun start() {
        TODO("Not yet implemented")
    }

    override fun end() {
        TODO("Not yet implemented")
    }

    override fun tasks(): Int {
        TODO("Not yet implemented")
    }

    override fun viewers(): HashMap<WebSocketConnection, Boolean> {
        TODO("Not yet implemented")
    }

    override fun wsMessageReceived(connection: WebSocketConnection, message: ClientMessage): Boolean {
        TODO("Not yet implemented")
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