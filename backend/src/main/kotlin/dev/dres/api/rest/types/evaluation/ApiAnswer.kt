package dev.dres.api.rest.types.evaluation

import dev.dres.api.rest.types.collection.ApiMediaItem
import dev.dres.data.model.submissions.Answer
import dev.dres.data.model.submissions.AnswerType

data class ApiAnswer(
    override val type: AnswerType,
    override val item: ApiMediaItem?,
    override val text: String?,
    override val start: Long?,
    override val end: Long?
    ) : Answer
