package dev.dres.run.filter

import dev.dres.data.model.submissions.Submission
import dev.dres.data.model.submissions.SubmissionStatus
import dev.dres.data.model.submissions.aspects.ItemAspect

class CorrectPerTeamItemFilter(private val limit: Int = 1) : SubmissionFilter {

    constructor(parameters: Map<String, String>) : this(parameters.getOrDefault("limit", "1").toIntOrNull() ?: 1)

    override val reason: String = "Maximum number of correct submissions ($limit) exceeded for this item."

    override fun test(t: Submission): Boolean =
        t is ItemAspect && t.task!!.submissions.count { it is ItemAspect && it.item.id == t.item.id && it.status == SubmissionStatus.CORRECT && it.teamId == t.teamId } < limit
}