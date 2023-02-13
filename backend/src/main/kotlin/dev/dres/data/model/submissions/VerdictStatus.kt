package dev.dres.data.model.submissions

import dev.dres.api.rest.types.evaluation.ApiVerdictStatus

enum class VerdictStatus {
        CORRECT, WRONG, INDETERMINATE, UNDECIDABLE;

    fun toApi(): ApiVerdictStatus = when(this) {
        CORRECT -> ApiVerdictStatus.CORRECT
        WRONG -> ApiVerdictStatus.WRONG
        INDETERMINATE -> ApiVerdictStatus.INDETERMINATE
        UNDECIDABLE -> ApiVerdictStatus.UNDECIDABLE
    }

    fun toDb(): DbVerdictStatus = when(this) {
        CORRECT -> DbVerdictStatus.CORRECT
        WRONG -> DbVerdictStatus.WRONG
        INDETERMINATE -> DbVerdictStatus.INDETERMINATE
        UNDECIDABLE -> DbVerdictStatus.UNDECIDABLE
    }

    companion object {

        fun fromApi(status: ApiVerdictStatus): VerdictStatus = when(status) {
            ApiVerdictStatus.CORRECT -> CORRECT
            ApiVerdictStatus.WRONG -> WRONG
            ApiVerdictStatus.INDETERMINATE -> INDETERMINATE
            ApiVerdictStatus.UNDECIDABLE -> UNDECIDABLE
        }

        fun fromDb(status: DbVerdictStatus): VerdictStatus = when(status) {
            DbVerdictStatus.CORRECT -> CORRECT
            DbVerdictStatus.WRONG -> WRONG
            DbVerdictStatus.INDETERMINATE -> INDETERMINATE
            DbVerdictStatus.UNDECIDABLE -> UNDECIDABLE
            else -> throw IllegalStateException("Unknown DbVerdictStatus $status")
        }

    }

}