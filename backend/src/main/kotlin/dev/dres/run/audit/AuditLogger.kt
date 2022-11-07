package dev.dres.run.audit

import dev.dres.api.rest.handler.users.SessionId
import dev.dres.data.model.admin.UserId
import dev.dres.data.model.audit.AuditLogEntry
import dev.dres.data.model.audit.AuditLogSource
import dev.dres.data.model.audit.AuditLogType
import dev.dres.data.model.template.EvaluationTemplate
import dev.dres.data.model.template.task.TaskTemplate
import dev.dres.data.model.template.task.TaskDescriptionId
import dev.dres.data.model.run.interfaces.CompetitionId
import dev.dres.data.model.submissions.Submission
import dev.dres.data.model.submissions.Verdict
import dev.dres.data.model.submissions.VerdictStatus
import dev.dres.run.eventstream.*
import dev.dres.run.validation.interfaces.JudgementValidator
import dev.dres.run.validation.interfaces.SubmissionValidator
import jetbrains.exodus.database.TransientEntityStore
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
     * @param description The [EvaluationTemplate].
     * @param api The [AuditLogSource]
     * @param session The identifier of the user session.
     */
    fun competitionStart(competitionId: CompetitionId, description: EvaluationTemplate, api: AuditLogSource, session: SessionId?) {
        this.store.transactional {
            AuditLogEntry.new {
                this.type = AuditLogType.COMPETITION_START
                this.source = api
                this.timestamp = DateTime.now()
                this.competitionId = competitionId.string
                this.session = session
            }
        }
        EventStreamProcessor.event(RunStartEvent(competitionId, description))
    }

    /**
     * Logs the end of a DRES competition.
     *
     * @param competitionId [EvaluationId] that identifies the competition
     * @param api The [AuditLogSource]
     * @param session The identifier of the user session.
     */
    fun competitionEnd(competitionId: CompetitionId, api: AuditLogSource, session: SessionId?) {
        this.store.transactional {
            AuditLogEntry.new {
                this.type = AuditLogType.COMPETITION_END
                this.source = api
                this.timestamp = DateTime.now()
                this.competitionId = competitionId.string
                this.session = session
            }
        }
        EventStreamProcessor.event(RunEndEvent(competitionId))
    }

    /**
     * Logs the start of a DRES task.
     *
     * @param competitionId [EvaluationId] that identifies the competition
     * @param taskId [EvaluationId] that identifies the task
     * @param description The [TaskTemplate].
     * @param api The [AuditLogSource]
     * @param session The identifier of the user session.
     */
    fun taskStart(competitionId: CompetitionId, taskId: TaskId, description: TaskTemplate, api: AuditLogSource, session: SessionId?) {
        this.store.transactional {
            AuditLogEntry.new {
                this.type = AuditLogType.TASK_START
                this.source = api
                this.timestamp = DateTime.now()
                this.competitionId = competitionId.string
                this.taskId = taskId.string
                this.session = session
            }
        }
        EventStreamProcessor.event(TaskStartEvent(competitionId, taskId, description))
    }

    /**
     * Logs the start of a DRES task.
     *
     * @param competitionId [EvaluationId] that identifies the competition
     * @param taskId [EvaluationId] that identifies the task
     * @param modification Description of the modification.
     * @param api The [AuditLogSource]
     * @param session The identifier of the user session.
     */
    fun taskModified(competitionId: CompetitionId, taskId: TaskDescriptionId, modification: String, api: AuditLogSource, session: String?)  {
        this.store.transactional {
            AuditLogEntry.new {
                this.type = AuditLogType.TASK_MODIFIED
                this.source = api
                this.timestamp = DateTime.now()
                this.competitionId = competitionId.string
                this.taskId = taskId.string
                this.description = modification
                this.session = session
            }
        }
    }

    /**
     * Logs the end of a DRES task.
     *
     * @param competitionId [EvaluationId] that identifies the competition
     * @param taskId [EvaluationId] that identifies the task
     * @param api The [AuditLogSource]
     * @param session The identifier of the user session.
     */
    fun taskEnd(competitionId: CompetitionId, taskId: TaskId, api: AuditLogSource, session: SessionId?) {
        this.store.transactional {
            AuditLogEntry.new {
                this.type = AuditLogType.TASK_END
                this.source = api
                this.timestamp = DateTime.now()
                this.competitionId = competitionId.string
                this.taskId = taskId.string
                this.session = session
            }
        }
        EventStreamProcessor.event(TaskEndEvent(competitionId, taskId))
    }

    /**
     * Logs an incoming submission to DRES.
     *
     * @param submission The [Submission] that was registered.
     * @param api The [AuditLogSource]
     * @param sessionId The identifier of the user session.
     * @param address The IP address of the submitter.
     */
    fun submission(submission: Submission, api: AuditLogSource, sessionId: SessionId?, address: String) {
        this.store.transactional {
            AuditLogEntry.new {
                this.type = AuditLogType.SUBMISSION
                this.source = api
                this.timestamp = DateTime.now()
                this.submissionId = submission.uid.string
                this.competitionId = submission.task?.competition?.id?.string
                this.taskId = submission.task?.uid?.string
                this.submissionId = submission.uid.string
                this.session = sessionId
                this.address = address
            }
        }
        EventStreamProcessor.event(SubmissionEvent(sessionId ?: "na", submission.task?.competition?.id!!, submission.task?.uid, submission))
    }

    /**
     * Logs the validation of a [Submission] to DRES.
     *
     * @param submission The [Submission] the submission that was validated
     * @param validator The [SubmissionValidator] instance.
     */
    fun validateSubmission(submission: Submission, validator: SubmissionValidator) {
        this.store.transactional {
            AuditLogEntry.new {
                this.type = AuditLogType.SUBMISSION_VALIDATION
                this.source = AuditLogSource.INTERNAL
                this.timestamp = DateTime.now()
                this.submissionId = submission.uid.string
                this.competitionId = submission.task?.competition?.id?.string
                this.taskId = submission.task?.uid?.string
                this.verdict = submission.status.toString()
                this.validatorName = validator::class.simpleName
            }
        }
    }

    /**
     * Logs a submission override to DRES.
     *
     * @param submission The [Submission] that was overriden (new snapshot).
     * @param api The [AuditLogSource]
     * @param sessionId The identifier of the user session.
     */
    fun overrideSubmission(submission: Submission, api: AuditLogSource, sessionId: SessionId?) {
        this.store.transactional {
            AuditLogEntry.new {
                this.type = AuditLogType.SUBMISSION_STATUS_OVERWRITE
                this.source = api
                this.timestamp = DateTime.now()
                this.submissionId = submission.uid.string
                this.competitionId = submission.task?.competition?.id?.string
                this.taskId = submission.task?.uid?.string
                this.verdict = submission.status.toString()
                this.session = sessionId
            }
        }
    }

    /**
     * Logs a submission override to DRES.
     *
     * @param verdict The [Submission] that was overriden (new snapshot).
     * @param validator The [JudgementValidator] instance.
     * @param token The token generated by the judgement sub-system
     */
    fun prepareJudgement(verdict: Verdict, validator: JudgementValidator, token: String) {
        AuditLogEntry.new {
            this.type = AuditLogType.PREPARE_JUDGEMENT
            this.source = AuditLogSource.INTERNAL
            this.timestamp = DateTime.now()
            this.submissionId = verdict.submission.id
            this.competitionId = verdict.task.evaluation.evaluationId
            this.taskId = verdict.task.taskId
            this.description = "Token: $token, Validator: ${validator.id}, Verdict: ${verdict.status.description}"
        }
    }

    /**
     * Logs a submission override to DRES.
     *
     * @param competitionId [EvaluationId] that identifies the competition
     * @param validator The [JudgementValidator] instance.
     * @param token The token generated by the judgement sub-system
     * @param verdict The [VerdictStatus] submitted by the judge.
     * @param api The [AuditLogSource]
     * @param sessionId The identifier of the user session.
     */
    fun judgement(competitionId: EvaluationId, validator: JudgementValidator, token: String, verdict: VerdictStatus, api: AuditLogSource, sessionId: SessionId?) {
        AuditLogEntry.new {
            this.type = AuditLogType.JUDGEMENT
            this.source = api
            this.timestamp = DateTime.now()
            this.competitionId = competitionId.string
            this.verdict = verdict.toString()
            this.validatorName = validator.id
            this.description = "Token: $token"
            this.session = sessionId
        }
    }

    /**
     * Logs a user user login event.
     *
     * @param userId [EvaluationId] of the user who logged out.
     * @param api The [AuditLogSource]
     * @param sessionId The [SessionId]
     */
    fun login(userId: UserId, api: AuditLogSource, sessionId: SessionId) {
        this.store.transactional {
            AuditLogEntry.new {
                this.type = AuditLogType.LOGIN
                this.source = api
                this.timestamp = DateTime.now()
                this.userId = userId
                this.session = sessionId
            }
        }
    }

    /**
     * Logs a user logout event.
     *
     * @param userId [EvaluationId] of the user who logged out.
     * @param api The [AuditLogSource]
     * @param sessionId The [SessionId]
     */
    fun logout(userId: UserId, api: AuditLogSource, sessionId: SessionId) {
        this.store.transactional {
            AuditLogEntry.new {
                this.type = AuditLogType.LOGOUT
                this.source = api
                this.timestamp = DateTime.now()
                this.userId = userId
                this.session = sessionId
            }
        }
    }
}