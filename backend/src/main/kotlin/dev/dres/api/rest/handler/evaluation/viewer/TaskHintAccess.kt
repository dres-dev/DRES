package dev.dres.api.rest.handler.evaluation.viewer

/** Participants and judges may only receive hints after a matching task run exists. */
fun isTaskHintAccessAllowed(isParticipant: Boolean, isJudge: Boolean, taskRunExists: Boolean): Boolean =
    !(isParticipant || isJudge) || taskRunExists
