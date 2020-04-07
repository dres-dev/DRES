package dres.data.model.competition

enum class TaskType(val defaultDuration: Long) {
    KIS_VISUAL(300_000),
    KIS_TEXTUAL(420_000),
    AVS(300_000)
}