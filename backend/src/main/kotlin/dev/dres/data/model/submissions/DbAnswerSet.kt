package dev.dres.data.model.submissions

import dev.dres.api.rest.types.evaluation.ApiAnswerSet
import dev.dres.data.model.PersistentEntity
import dev.dres.data.model.run.DbTask
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*
import kotlinx.dnq.query.asSequence

/**
 * A [DbVerdictStatus] as submitted by a competition participant. Makes a statement about a [DbTask].
 *
 * @author Ralph Gasser
 * @author Luca Rossetto
 * @version 2.0.0
 */
class DbAnswerSet(entity: Entity) : PersistentEntity(entity) {
    companion object : XdNaturalEntityType<DbAnswerSet>()

    /** The [DbVerdictStatus] of this [DbAnswerSet]. */
    var status by xdLink1(DbVerdictStatus)

    /** The [DbSubmission] this [DbAnswerSet] belongs to. */
    var submission: DbSubmission by xdParent<DbAnswerSet,DbSubmission>(DbSubmission::answerSets)

    /** The [DbTask] this [DbAnswerSet] belongs to. */
    var task: DbTask by xdParent<DbAnswerSet, DbTask>(DbTask::submissions)

    val answers by xdChildren1_N<DbAnswerSet, DbAnswer>(DbAnswer::answerSet)

    /**
     * Converts this [DbVerdictStatus] to a RESTful API representation [ApiAnswerSet].
     *
     * This is a convenience method and requires an active transaction context.
     *
     * @return [ApiAnswerSet]
     */
    fun toApi(): ApiAnswerSet = ApiAnswerSet(
        status = this.status.toApi(),
        answers = this.answers.asSequence().map { it.toApi() }.toList()
    )
}
