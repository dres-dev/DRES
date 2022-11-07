package dev.dres.api.rest.types.evaluation

import dev.dres.data.model.submissions.VerdictStatus
import dev.dres.data.model.submissions.VerdictType

/**
 * The RESTful API equivalent for the type of a [VerdictType]
 *
 * @see ApiVerdict
 * @author Ralph Gasser
 * @version 1.0.0
 */
enum class ApiVerdictType(val type: VerdictType) {
    ITEM(VerdictType.ITEM),
    TEMPORAL(VerdictType.TEMPORAL),
    TEXT(VerdictType.TEXT)
}