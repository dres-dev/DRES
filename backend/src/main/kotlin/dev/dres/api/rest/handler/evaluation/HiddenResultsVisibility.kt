package dev.dres.api.rest.handler.evaluation

import dev.dres.api.rest.types.evaluation.submission.ApiSubmission

/** Applies HIDDEN_RESULTS answer redaction while preserving verdict metadata. */
internal object HiddenResultsVisibility {
    fun shouldBlind(
        hidden: Boolean,
        participant: Boolean,
        submissionTeamId: String,
        ownTeamIds: Set<String>
    ): Boolean = hidden && (!participant || submissionTeamId !in ownTeamIds)

    fun redactIfRequired(
        hidden: Boolean,
        participant: Boolean,
        submission: ApiSubmission,
        ownTeamIds: Set<String>
    ): ApiSubmission = if (shouldBlind(hidden, participant, submission.teamId, ownTeamIds)) blind(submission) else submission

    fun blind(submission: ApiSubmission): ApiSubmission = submission.copy(
        answers = submission.answers.map { answerSet ->
            answerSet.copy(answers = answerSet.answers.map { answer ->
                answer.copy(item = null, start = null, end = null, text = null, hidden = true)
            })
        }
    )
}
