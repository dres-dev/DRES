package dev.dres.api.rest.types.evaluation

import dev.dres.api.rest.types.collection.ApiMediaItem
import dev.dres.data.model.media.DbMediaItem
import dev.dres.data.model.media.MediaItem
import dev.dres.data.model.submissions.Answer
import dev.dres.data.model.submissions.AnswerType
import dev.dres.data.model.submissions.DbAnswer
import kotlinx.dnq.query.filter
import kotlinx.dnq.query.firstOrNull

data class ApiAnswer(
    val type: ApiAnswerType,
    override val item: ApiMediaItem?,
    override val text: String?,
    override val start: Long?,
    override val end: Long?
    ) : Answer {

    /**
     * Creates a new [DbAnswer] for this [ApiAnswer]. Requires an ongoing transaction.
     *
     * @return [DbAnswer]
     */
    fun toNewDb(): DbAnswer {
        return DbAnswer.new {
            this.type = this@ApiAnswer.type.toDb()
            this.item = DbMediaItem.filter { it.id eq this@ApiAnswer.item?.mediaItemId }.firstOrNull()
            this.text = this@ApiAnswer.text
            this.start = this@ApiAnswer.start
            this.end = this@ApiAnswer.end
        }
    }

    override fun type(): AnswerType = AnswerType.fromApi(this.type)
}
