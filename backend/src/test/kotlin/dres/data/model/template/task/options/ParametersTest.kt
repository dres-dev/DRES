package dev.dres.data.model.template.task.options

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ParametersTest {
    @Test
    fun `legacy prolongation and limit keys normalize to current dotted names`() {
        val legacy = mapOf(
            Parameters.LEGACY_PROLONG_ON_SUBMISSION_LIMIT_PARAM to "8",
            Parameters.LEGACY_PROLONG_ON_SUBMISSION_BY_PARAM to "12",
            Parameters.LEGACY_PROLONG_ON_SUBMISSION_CORRECT_PARAM to "true",
            Parameters.LEGACY_LIMIT_CORRECT_PER_TEAM_PARAM to "3"
        )

        val normalized = Parameters.normalizeLegacyKeys(legacy)

        assertEquals(
            mapOf(
                Parameters.PROLONG_ON_SUBMISSION_LIMIT_PARAM to "8",
                Parameters.PROLONG_ON_SUBMISSION_BY_PARAM to "12",
                Parameters.PROLONG_ON_SUBMISSION_CORRECT_PARAM to "true",
                Parameters.LIMIT_CORRECT_PER_TEAM_PARAM to "3"
            ),
            normalized
        )
        assertEquals("8", Parameters.value(legacy, Parameters.PROLONG_ON_SUBMISSION_LIMIT_PARAM))
    }

    @Test
    fun `current values take precedence over legacy values`() {
        val configuration = mapOf(
            Parameters.PROLONG_ON_SUBMISSION_LIMIT_PARAM to "5",
            Parameters.LEGACY_PROLONG_ON_SUBMISSION_LIMIT_PARAM to "9"
        )

        assertEquals("5", Parameters.value(configuration, Parameters.PROLONG_ON_SUBMISSION_LIMIT_PARAM))
        assertEquals(
            "5",
            Parameters.normalizeLegacyKeys(configuration)[Parameters.PROLONG_ON_SUBMISSION_LIMIT_PARAM]
        )
    }
}
