package dev.dres.api.rest.types.evaluation

import kotlinx.serialization.Serializable

/** Counts of submitted answer sets grouped by their current verdict. */
@Serializable
data class ApiVerdictCounts(
    val correct: Int,
    val wrong: Int,
    val indeterminate: Int,
    val undecidable: Int
) {
    val total: Int get() = correct + wrong + indeterminate + undecidable
}
