package dev.dres.run.audit

import com.fasterxml.jackson.annotation.JsonTypeInfo
import dev.dres.api.rest.handler.users.SessionToken
import dev.dres.api.rest.types.evaluation.submission.ApiAnswerSet
import dev.dres.api.rest.types.evaluation.submission.ApiClientSubmission
import dev.dres.api.rest.types.evaluation.submission.ApiVerdictStatus
import dev.dres.api.rest.types.template.ApiEvaluationTemplate
import dev.dres.api.rest.types.template.tasks.ApiTaskTemplate
import dev.dres.data.model.admin.UserId
import dev.dres.data.model.run.interfaces.EvaluationId
import dev.dres.run.validation.interfaces.JudgementValidator

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "class")
sealed class AuditLogEntry{
    val timestamp: Long = System.currentTimeMillis()
}

data class EvaluationStartAuditLogEntry(
    val evaluationId: EvaluationId,
    val description: ApiEvaluationTemplate,
    val api: AuditLogSource,
    val userId: UserId?,
    val session: SessionToken?
): AuditLogEntry()

data class EvaluationEndAuditLogEntry(
    val evaluationId: EvaluationId,
    val api: AuditLogSource,
    val userId: UserId?,
    val session: SessionToken?
) : AuditLogEntry()

data class TaskStartAuditLogEntry(
    val evaluationId: EvaluationId,
    val taskId: EvaluationId,
    val description: ApiTaskTemplate,
    val api: AuditLogSource,
    val session: SessionToken?
) : AuditLogEntry()

data class TaskModifiedAuditLogEntry(
    val evaluationId: EvaluationId,
    val taskId: EvaluationId,
    val modification: String,
    val api: AuditLogSource,
    val session: String?
) : AuditLogEntry()

data class TaskEndAuditLogEntry(
    val evaluationId: EvaluationId,
    val taskId: EvaluationId,
    val api: AuditLogSource,
    val session: SessionToken?
) : AuditLogEntry()

data class SubmissionAuditLogEntry(
    val submission: ApiClientSubmission,
    val evaluationId: EvaluationId,
    val api: AuditLogSource,
    val sessionToken: SessionToken?,
    val address: String
) : AuditLogEntry()

data class OverrideVerdictAuditLogEntry(
    val answerSet: ApiAnswerSet,
    val verdict: ApiVerdictStatus,
    val api: AuditLogSource,
    val sessionToken: SessionToken?
) : AuditLogEntry()

data class PrepareJudgementAuditLogEntry(
    val answerSet: ApiAnswerSet,
    val validator: JudgementValidator,
    val token: String
) : AuditLogEntry()

data class JudgementAuditLogEntry(
    val evaluationId: EvaluationId,
    val validator: JudgementValidator,
    val token: String,
    val verdict: ApiVerdictStatus,
    val api: AuditLogSource,
    val sessionToken: SessionToken?
) : AuditLogEntry()

data class LoginAuditLogEntry(
    val userId: UserId,
    val api: AuditLogSource,
    val sessionToken: SessionToken
) : AuditLogEntry()

data class LogoutAuditLogEntry(
    val userId: UserId,
    val api: AuditLogSource,
    val sessionToken: SessionToken
) : AuditLogEntry()