package dev.dres.data.model.submissions

interface AnswerType {

    enum class Type {
        ITEM, TEMPORAL, TEXT
    }

    infix fun eq(status: Type): Boolean

}