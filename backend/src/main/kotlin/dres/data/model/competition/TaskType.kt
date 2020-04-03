package dres.data.model.competition

enum class TaskType(val defaultDuration: Long) {
    KIS_VISUAL(300),
    KIS_TEXTUAL(420),
    AVS(300)
}