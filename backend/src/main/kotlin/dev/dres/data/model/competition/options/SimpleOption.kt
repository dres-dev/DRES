package dev.dres.data.model.competition.options

/**
 * Simple [Option]s that can be applied to a task.
 *
 * @author Luca Rossetto & Ralph Gasser
 * @version 1.1.0
 */
enum class SimpleOption: Option {
    HIDDEN_RESULTS, /** Do not show submissions while task is running. */
    MAP_TO_SEGMENT, /** Map the time of a submission to a pre-defined segment. */
    PROLONG_ON_SUBMISSION; /** Prolongs a task if a submission arrives within a certain time limit towards the end. */
}