package dev.dres.run

import dev.dres.api.rest.types.WebSocketConnection
import dev.dres.api.rest.types.run.websocket.ClientMessage
import dev.dres.data.model.UID
import dev.dres.data.model.competition.CompetitionDescription
import dev.dres.data.model.competition.TaskDescription
import dev.dres.data.model.run.*
import dev.dres.run.score.ScoreTimePoint
import dev.dres.run.score.scoreboard.Scoreboard
import dev.dres.run.validation.interfaces.JudgementValidator

class InteractiveAsynchronousRunManager(private val run: InteractiveAsynchronousCompetitionRun) : InteractiveRunManager {



    override fun currentTask(context: RunActionContext): TaskDescription? {
        TODO("Not yet implemented")
    }

    override val submissions: List<Submission>
        get() = TODO("Not yet implemented")

    override val scoreHistory: List<ScoreTimePoint>
        get() = TODO("Not yet implemented")

    override val allSubmissions: List<Submission>
        get() = TODO("Not yet implemented")

    override val currentTaskRun: InteractiveSynchronousCompetitionRun.TaskRun?
        get() = TODO("Not yet implemented")

    override fun tasks(context: RunActionContext): List<InteractiveTask> {
        TODO("Not yet implemented")
    }

    override fun previousTask(context: RunActionContext): Boolean {
        TODO("Not yet implemented")
    }

    override fun nextTask(context: RunActionContext): Boolean {
        TODO("Not yet implemented")
    }

    override fun goToTask(context: RunActionContext, index: Int) {
        TODO("Not yet implemented")
    }

    override fun startTask(context: RunActionContext) {
        TODO("Not yet implemented")
    }

    override fun abortTask(context: RunActionContext) {
        TODO("Not yet implemented")
    }

    override fun adjustDuration(context: RunActionContext, s: Int): Long {
        TODO("Not yet implemented")
    }

    override fun timeLeft(context: RunActionContext): Long {
        TODO("Not yet implemented")
    }

    override fun taskRunForId(
        context: RunActionContext,
        taskRunId: UID
    ): InteractiveSynchronousCompetitionRun.TaskRun? {
        TODO("Not yet implemented")
    }

    override fun overrideReadyState(context: RunActionContext, viewerId: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun postSubmission(sub: Submission): SubmissionStatus {
        TODO("Not yet implemented")
    }

    override fun updateSubmission(suid: UID, newStatus: SubmissionStatus): Boolean {
        TODO("Not yet implemented")
    }

    override val id: UID
        get() = TODO("Not yet implemented")

    override val name: String
        get() = TODO("Not yet implemented")

    override val competitionDescription: CompetitionDescription
        get() = this.run.competitionDescription

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