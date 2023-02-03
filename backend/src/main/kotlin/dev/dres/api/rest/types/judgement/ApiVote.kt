package dev.dres.api.rest.types.judgement

import dev.dres.data.model.submissions.DbVerdictStatus

/**
 *
 * @author Ralph Gasser
 * @version 1.0
 */
data class ApiVote(val verdict: DbVerdictStatus)
