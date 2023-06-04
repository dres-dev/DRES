package dev.dres.api.rest.types.evaluation

data class ApiClientAnswerSet(
    val taskName: String,
    val answers: List<ApiClientAnswer>
) {

}
