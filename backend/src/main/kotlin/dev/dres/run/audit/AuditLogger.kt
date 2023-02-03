package dev.dres.run.audit

import dev.dres.api.rest.handler.users.SessionToken
import dev.dres.data.model.admin.UserId
import dev.dres.data.model.audit.DbAuditLogEntry
import dev.dres.data.model.audit.DbAuditLogSource
import dev.dres.data.model.audit.DbAuditLogType
import dev.dres.data.model.run.EvaluationId
import dev.dres.data.model.template.DbEvaluationTemplate
import dev.dres.data.model.template.task.DbTaskTemplate
import dev.dres.data.model.submissions.DbSubmission
import dev.dres.data.model.submissions.DbAnswerSet
import dev.dres.data.model.submissions.DbVerdictStatus
import dev.dres.run.eventstream.*
import dev.dres.run.validation.interfaces.JudgementValidator
import dev.dres.run.validation.interfaces.SubmissionValidator
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.first
import org.joda.time.DateTime

/**
 * Audit logging instance of DRES. Requires one-time initialisation
 *
 * @author Luca Rossetto
 * @version 2.0.0
 */
object AuditLogger {
    /** The [TransientEntityStore] instance used by this [AuditLogger]. */
    private lateinit var store: TransientEntityStore

    /** Initializes this [AuditLogger]. */
    fun init(store: TransientEntityStore) {
        this.store = store
    }

    /**
     * Logs the start of a DRES competition.
     *
     * @param description The [DbEvaluationTemplate].
     * @param api The [DbAuditLogSource]
     * @param session The identifier of the user session.
     */
    fun competitionStart(evaluationId: EvaluationId, description: DbEvaluationTemplate, api: DbAuditLogSource, session: SessionToken?) = this.store.transactional {
        DbAuditLogEntry.new {
            this.type = DbAuditLogType.COMPETITION_START
            this.source = api
            this.timestamp = DateTime.now()
            this.evaluationId = evaluationId
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
    fun competitionEnd(evaluationId: EvaluationId, api: DbAuditLogSource, session: SessionToken?) = this.store.transactional {
        DbAuditLogEntry.new {
            this.type = DbAuditLogType.COMPETITION_END
            this.source = api
            this.timestamp = DateTime.now()
            this.evaluationId = evaluationId
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
    fun taskStart(evaluationId: EvaluationId, taskId: EvaluationId, description: DbTaskTemplate, api: DbAuditLogSource, session: SessionToken?) = this.store.transactional {
        DbAuditLogEntry.new {
            this.type = DbAuditLogType.TASK_START
            this.source = api
            this.timestamp = DateTime.now()
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
    fun taskModified(evaluationId: EvaluationId, taskId: EvaluationId, modification: String, api: DbAuditLogSource, session: String?)  = this.store.transactional {
        DbAuditLogEntry.new {
            this.type = DbAuditLogType.TASK_MODIFIED
            this.source = api
            this.timestamp = DateTime.now()
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
    fun taskEnd(evaluationId: EvaluationId, taskId: EvaluationId, api: DbAuditLogSource, session: SessionToken?) = this.store.transactional {
        DbAuditLogEntry.new {
            this.type = DbAuditLogType.TASK_END
            this.source = api
            this.timestamp = DateTime.now()
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
    fun submission(submission: DbSubmission, api: DbAuditLogSource, sessionToken: SessionToken?, address: String) = this.store.transactional {
        DbAuditLogEntry.new {
            this.type = DbAuditLogType.SUBMISSION
            this.source = api
            this.timestamp = DateTime.now()
            this.submissionId = submission.id
            this.evaluationId = submission.verdicts.first().task.evaluation.evaluationId
            this.taskId = submission.verdicts.first().task.id /* TODO: Multiple verdicts. */
            this.session = sessionToken
            this.address = address
        }
        EventStreamProcessor.event(SubmissionEvent(sessionToken ?: "na", submission.verdicts.first().task.evaluation.evaluationId, submission.verdicts.first().task.id, submission))
    }

    /**
     * Logs the validation of a [DbSubmission] to DRES.
     *
     * @param submission The [DbSubmission] the submission that was validated
     * @param validator The [SubmissionValidator] instance.
     */
    fun validateSubmission(submission: DbSubmission, validator: SubmissionValidator) = this.store.transactional {
        this.store.transactional {
            DbAuditLogEntry.new {
                this.type = DbAuditLogType.SUBMISSION_VALIDATION
                this.source = DbAuditLogSource.INTERNAL
                this.timestamp = DateTime.now()
                this.submissionId = submission.id
                this.evaluationId = submission.verdicts.first().task.evaluation.evaluationId
                this.taskId = submission.verdicts.first().task.id /* TODO: Multiple verdicts. */
                this.description = "Validator: ${validator::class.simpleName}, Verdict: ${submission.verdicts.first().status.description}" /* TODO: Here name, there ID. Why? */
            }
        }
    }

    /**
     * Logs a submission override to DRES.
     *
     * @param submission The [DbSubmission] that was overriden (new snapshot).
     * @param api The [DbAuditLogSource]
     * @param sessionToken The identifier of the user session.
     */
    fun overrideSubmission(submission: DbSubmission, api: DbAuditLogSource, sessionToken: SessionToken?) = this.store.transactional {
        this.store.transactional {
            DbAuditLogEntry.new {
                this.type = DbAuditLogType.SUBMISSION_STATUS_OVERWRITE
                this.source = api
                this.timestamp = DateTime.now()
                this.submissionId = submission.id
                this.evaluationId = submission.verdicts.first().task.evaluation.evaluationId
                this.taskId = submission.verdicts.first().task.id /* TODO: Multiple verdicts. */
                this.description = "Verdict: ${submission.verdicts.first().status.description}"
                this.session = sessionToken
            }
        }
    }

    /**
     * Logs a submission override to DRES.
     *
     * @param answerSet The [DbSubmission] that was overriden (new snapshot).
     * @param validator The [JudgementValidator] instance.
     * @param token The token generated by the judgement sub-system
     */
    fun prepareJudgement(answerSet: DbAnswerSet, validator: JudgementValidator, token: String) = this.store.transactional {
        DbAuditLogEntry.new {
            this.type = DbAuditLogType.PREPARE_JUDGEMENT
            this.source = DbAuditLogSource.INTERNAL
            this.timestamp = DateTime.now()
            this.submissionId = answerSet.submission.id
            this.evaluationId = answerSet.task.evaluation.evaluationId
            this.taskId = answerSet.task.taskId
            this.description = "Token: $token, Validator: ${validator.id}, Verdict: ${answerSet.status.description}"
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
    fun judgement(evaluationId: EvaluationId, validator: JudgementValidator, token: String, verdict: DbVerdictStatus, api: DbAuditLogSource, sessionToken: SessionToken?) = this.store.transactional {
        DbAuditLogEntry.new {
            this.type = DbAuditLogType.JUDGEMENT
            this.source = api
            this.timestamp = DateTime.now()
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
    fun login(userId: UserId, api: DbAuditLogSource, sessionToken: SessionToken) = this.store.transactional {
        DbAuditLogEntry.new {
            this.type = DbAuditLogType.LOGIN
            this.source = api
            this.timestamp = DateTime.now()
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
    fun logout(userId: UserId, api: DbAuditLogSource, sessionToken: SessionToken) = this.store.transactional {
        DbAuditLogEntry.new {
            this.type = DbAuditLogType.LOGOUT
            this.source = api
            this.timestamp = DateTime.now()
            this.userId = userId
            this.session = sessionToken
        }
    }
}