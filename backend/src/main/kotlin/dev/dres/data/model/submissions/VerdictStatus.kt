package dev.dres.data.model.submissions

interface VerdictStatus {

    enum class Status {
        CORRECT, WRONG, INDETERMINATE, UNDECIDABLE
    }

    infix fun eq(status: Status): Boolean

}