package dev.dres.api.rest.types.submission

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class ResultElement(
    val item: String? = null,           /** Name of the item */
    val text: String? = null,           /** Text submission */
    val startTimeCode: String? = null,  /** Starting time of a segment */
    val endTimeCode: String? = null,    /** End time of a segment */
    val index: Int? = null,             /** Index of a segment in case of predefined segmentation */
    val rank: Int? = null,              /** Rank of a result within a result list */
    val weight: Float? = null           /** Weight in case of weighted results */
) {
    init {
        if (item.isNullOrBlank() && text.isNullOrBlank()) {
            throw IllegalArgumentException("ResultElement needs at least an item or text but neither was provided")
        }
    }
}
