package dev.dres.api.rest.types.judgement

import dev.dres.api.rest.types.collection.ApiMediaType

/**
 *
 * @author Ralph Gasser
 * @version 1.0
 */
class ApiJudgementRequest(val token: String?, val mediaType: ApiMediaType, val validator: String, val collection: String, val item: String, val taskDescription: String, val startTime: Long?, val endTime: Long?)