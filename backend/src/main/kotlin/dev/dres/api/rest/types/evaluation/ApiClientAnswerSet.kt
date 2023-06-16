package dev.dres.api.rest.types.evaluation

data class ApiClientAnswerSet(
    val taskName: String? = null,
    val answers: List<ApiClientAnswer>
) {

}
