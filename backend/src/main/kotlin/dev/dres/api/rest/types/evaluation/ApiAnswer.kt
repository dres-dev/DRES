package dev.dres.api.rest.types.evaluation

import dev.dres.api.rest.types.collection.ApiMediaItem
import dev.dres.data.model.submissions.Answer
import dev.dres.data.model.submissions.AnswerType
import dev.dres.data.model.submissions.DbAnswer

data class ApiAnswer(
    val type: ApiAnswerType,
    override val item: ApiMediaItem?,
    override val text: String?,
    override val start: Long?,
    override val end: Long?
    ) : Answer {
    override fun toDb(): DbAnswer {
        return DbAnswer.new {
            this.type = this@ApiAnswer.type.toDb()

        }
    }

    override fun type(): AnswerType = AnswerType.fromApi(this.type)
}
