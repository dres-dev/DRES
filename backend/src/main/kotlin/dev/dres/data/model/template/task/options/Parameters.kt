package dev.dres.data.model.template.task.options

object Parameters {
    /**
     * Key for [DbTaskOption.PROLONG_ON_SUBMISSION]; [Int] value expected.
     *
     * The parameter determines how many milliseconds before the end of a task the option should be applied.
     * If a task has a total duration of 500s, then a value of 5s means that submissions arriving between 495s and 500s
     * may trigger prolongation of the task.
     */
    const val PROLONG_ON_SUBMISSION_LIMIT_PARAM = "PROLONG_ON_SUBMISSION.limit" /* Configuration keys must have the form DOMAIN.KEY (enforced by the TemplateManager). */

    /** Key for [DbTaskOption.PROLONG_ON_SUBMISSION]; [Int] value expected. Determines by how many seconds a task should be prolonged. */
    const val PROLONG_ON_SUBMISSION_BY_PARAM = "PROLONG_ON_SUBMISSION.by"

    /** Key for [DbTaskOption.PROLONG_ON_SUBMISSION]; [Boolean] value expected. If true, task will only be prolonged for correct submissions. */
    const val PROLONG_ON_SUBMISSION_CORRECT_PARAM = "PROLONG_ON_SUBMISSION.correct"

    /** Legacy keys retained so existing task templates continue to honor their stored prolongation configuration. */
    const val LEGACY_PROLONG_ON_SUBMISSION_LIMIT_PARAM = "PROLONG_ON_SUBMISSION_LIMIT"
    const val LEGACY_PROLONG_ON_SUBMISSION_BY_PARAM = "PROLONG_ON_SUBMISSION_BY"
    const val LEGACY_PROLONG_ON_SUBMISSION_CORRECT_PARAM = "PROLONG_ON_SUBMISSION_CORRECT"

    /** Legacy key for the per-team correct-submission limit. */
    const val LEGACY_LIMIT_CORRECT_PER_TEAM_PARAM = "LIMIT_CORRECT_PER_TEAM"
    const val LIMIT_CORRECT_PER_TEAM_PARAM = "LIMIT_CORRECT_PER_TEAM.limit"

    private val legacyParameterNames = mapOf(
        LEGACY_PROLONG_ON_SUBMISSION_LIMIT_PARAM to PROLONG_ON_SUBMISSION_LIMIT_PARAM,
        LEGACY_PROLONG_ON_SUBMISSION_BY_PARAM to PROLONG_ON_SUBMISSION_BY_PARAM,
        LEGACY_PROLONG_ON_SUBMISSION_CORRECT_PARAM to PROLONG_ON_SUBMISSION_CORRECT_PARAM,
        LEGACY_LIMIT_CORRECT_PER_TEAM_PARAM to LIMIT_CORRECT_PER_TEAM_PARAM
    )

    /** Reads the renamed parameter while continuing to honor values persisted under its legacy key. */
    fun value(configuration: Map<String, String>, key: String): String? =
        configuration[key] ?: legacyParameterNames.entries.firstOrNull { it.value == key }?.let { configuration[it.key] }

    /** Rewrites legacy keys when a saved template is imported or edited. Current-format values take precedence. */
    fun normalizeLegacyKeys(configuration: Map<String, String>): Map<String, String> {
        val normalized = configuration.filterKeys { it !in legacyParameterNames }.toMutableMap()
        legacyParameterNames.forEach { (legacyKey, currentKey) ->
            if (currentKey !in normalized) {
                configuration[legacyKey]?.let { normalized[currentKey] = it }
            }
        }
        return normalized
    }
}
