package dev.dres.run.audit

import dev.dres.api.rest.handler.users.SessionToken
import dev.dres.data.model.admin.UserId
import dev.dres.data.model.audit.DbAuditLogEntry
import dev.dres.data.model.audit.DbAuditLogSource
import dev.dres.data.model.audit.DbAuditLogType
import dev.dres.data.model.run.interfaces.EvaluationId
import dev.dres.data.model.submissions.*
import dev.dres.data.model.template.DbEvaluationTemplate
import dev.dres.data.model.template.task.DbTaskTemplate
import dev.dres.run.eventstream.*
import dev.dres.run.validation.interfaces.JudgementValidator
import kotlinx.dnq.query.first

/**
 * Audit logging instance of DRES. Requires one-time initialisation
 *
 * @author Luca Rossetto
 * @version 2.0.0
 */
object DbAuditLogger {

    /**
     * Logs the start of a DRES competition.
     *
     * @param description The [DbEvaluationTemplate].
     * @param api The [DbAuditLogSource]
     * @param session The identifier of the user session.
     */
    fun evaluationStart(evaluationId: EvaluationId, description: DbEvaluationTemplate, api: DbAuditLogSource, userId: UserId?, session: SessionToken?) {
        DbAuditLogEntry.new {
            this.type = DbAuditLogType.COMPETITION_START
            this.source = api
            this.evaluationId = evaluationId
            this.userId = userId
            this.session = session
        }
        EventStreamProcessor.event(RunStartEvent(evaluationId, description))
    }

    /**
     * Logs the end of a DRES competition.
     *
     * @param evaluationId [EvaluationId] that identifies the competition
     * @param api The [DbAuditLogSource]
     * @param session The identifier of the user session.
     */
    fun evaluationEnd(evaluationId: EvaluationId, api: DbAuditLogSource, userId: UserId?, session: SessionToken?) {
        DbAuditLogEntry.new {
            this.type = DbAuditLogType.COMPETITION_END
            this.source = api
            this.evaluationId = evaluationId
            this.userId = userId
            this.session = session
        }
        EventStreamProcessor.event(RunEndEvent(evaluationId))
    }

    /**
     * Logs the start of a DRES task.
     *
     * @param evaluationId [EvaluationId] that identifies the competition
     * @param taskId [EvaluationId] that identifies the task
     * @param description The [DbTaskTemplate].
     * @param api The [DbAuditLogSource]
     * @param session The identifier of the user session.
     */
    fun taskStart(evaluationId: EvaluationId, taskId: EvaluationId, description: DbTaskTemplate, api: DbAuditLogSource, session: SessionToken?) {
        DbAuditLogEntry.new {
            this.type = DbAuditLogType.TASK_START
            this.source = api
            this.evaluationId = evaluationId
            this.taskId = taskId
            this.session = session
        }
        EventStreamProcessor.event(TaskStartEvent(evaluationId, taskId, description))
    }

    /**
     * Logs the start of a DRES task.
     *
     * @param evaluationId [EvaluationId] that identifies the competition
     * @param taskId [EvaluationId] that identifies the task
     * @param modification Description of the modification.
     * @param api The [DbAuditLogSource]
     * @param session The identifier of the user session.
     */
    fun taskModified(evaluationId: EvaluationId, taskId: EvaluationId, modification: String, api: DbAuditLogSource, session: String?) {
        DbAuditLogEntry.new {
            this.type = DbAuditLogType.TASK_MODIFIED
            this.source = api
            this.evaluationId = evaluationId
            this.taskId = taskId
            this.description = modification
            this.session = session
        }
    }

    /**
     * Logs the end of a DRES task.
     *
     * @param evaluationId [EvaluationId] that identifies the competition
     * @param taskId [EvaluationId] that identifies the task
     * @param api The [DbAuditLogSource]
     * @param session The identifier of the user session.
     */
    fun taskEnd(evaluationId: EvaluationId, taskId: EvaluationId, api: DbAuditLogSource, session: SessionToken?) {
        DbAuditLogEntry.new {
            this.type = DbAuditLogType.TASK_END
            this.source = api
            this.evaluationId = evaluationId
            this.taskId = taskId
            this.session = session
        }
        EventStreamProcessor.event(TaskEndEvent(evaluationId, taskId))
    }

    /**
     * Logs an incoming submission to DRES.
     *
     * @param submission The [DbSubmission] that was registered.
     * @param api The [DbAuditLogSource]
     * @param sessionToken The identifier of the user session.
     * @param address The IP address of the submitter.
     */
    fun submission(submission: DbSubmission, api: DbAuditLogSource, sessionToken: SessionToken?, address: String) {
        val entry = DbAuditLogEntry.new {
            this.type = DbAuditLogType.SUBMISSION
            this.source = api
            this.submissionId = submission.submissionId
            this.evaluationId = submission.answerSets.first().task.evaluation.evaluationId
            this.session = sessionToken
            this.address = address
        }
        EventStreamProcessor.event(SubmissionEvent(sessionToken ?: "na", entry.evaluationId!!, null, submission))
    }

    /**
     * Logs a AnswerSet VerdictStatus override to DRES.
     */
    fun overrideVerdict(answerSet: DbAnswerSet, verdict: DbVerdictStatus, api: DbAuditLogSource, sessionToken: SessionToken?) {
        DbAuditLogEntry.new {
            this.type = DbAuditLogType.SUBMISSION_STATUS_OVERWRITE
            this.source = api
            this.submissionId = answerSet.submission.submissionId
            this.evaluationId = answerSet.task.evaluation.evaluationId
            this.taskId =answerSet.task.id
            this.description = "Set verdict of '${answerSet.id}' to ${verdict.description}"
            this.session = sessionToken
        }
    }

    /**
     * Logs a submission override to DRES.
     *
     * @param answerSet The [DbSubmission] that was overwritten (new snapshot).
     * @param validator The [JudgementValidator] instance.
     * @param token The token generated by the judgement sub-system
     */
    fun prepareJudgement(answerSet: DbAnswerSet, validator: JudgementValidator, token: String) {
        DbAuditLogEntry.new {
            this.type = DbAuditLogType.PREPARE_JUDGEMENT
            this.source = DbAuditLogSource.INTERNAL
            this.submissionId = answerSet.submission.submissionId
            this.evaluationId = answerSet.task.evaluation.evaluationId
            this.taskId = answerSet.task.taskId
            this.description = "Token: $token, Validator: ${validator.id}, AnswerSet: ${answerSet.id}, Verdict: ${answerSet.status.description}"
        }
    }

    /**
     * Logs a submission override to DRES.
     *
     * @param evaluationId [EvaluationId] that identifies the competition
     * @param validator The [JudgementValidator] instance.
     * @param token The token generated by the judgement sub-system
     * @param verdict The [DbVerdictStatus] submitted by the judge.
     * @param api The [DbAuditLogSource]
     * @param sessionToken The identifier of the user session.
     */
    fun judgement(evaluationId: EvaluationId, validator: JudgementValidator, token: String, verdict: DbVerdictStatus, api: DbAuditLogSource, sessionToken: SessionToken?) {
        DbAuditLogEntry.new {
            this.type = DbAuditLogType.JUDGEMENT
            this.source = api
            this.evaluationId = evaluationId
            this.description = "Token: $token, Validator: ${validator.id}, Verdict: ${verdict.description}"
            this.session = sessionToken
        }
    }

    /**
     * Logs a user user login event.
     *
     * @param userId [EvaluationId] of the user who logged out.
     * @param api The [DbAuditLogSource]
     * @param sessionToken The [SessionToken]
     */
    fun login(userId: UserId, api: DbAuditLogSource, sessionToken: SessionToken) {
        DbAuditLogEntry.new {
            this.type = DbAuditLogType.LOGIN
            this.source = api
            this.userId = userId
            this.session = sessionToken
        }
    }

    /**
     * Logs a user logout event.
     *
     * @param userId [EvaluationId] of the user who logged out.
     * @param api The [DbAuditLogSource]
     * @param sessionToken The [SessionToken]
     */
    fun logout(userId: UserId, api: DbAuditLogSource, sessionToken: SessionToken) {
        DbAuditLogEntry.new {
            this.type = DbAuditLogType.LOGOUT
            this.source = api
            this.userId = userId
            this.session = sessionToken
        }
    }
}