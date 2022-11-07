package dev.dres.run.validation

import dev.dres.data.model.submissions.Submission
import dev.dres.data.model.submissions.VerdictStatus
import dev.dres.data.model.submissions.VerdictType
import dev.dres.run.validation.interfaces.SubmissionValidator
import kotlinx.dnq.query.asSequence

/**
 * A [SubmissionValidator] class that valiadates textual submissions based on [Regex].
 *
 * @author Luca Rossetto
 * @author Ralph Gasser
 * @version 1.1.0
 */
class TextValidator(targets: List<String>) : SubmissionValidator {

    override val deferring = false

    /**
     * Transforms the targets to [Regex]s.
     * There is the convention introduced, that targets padded in backslashes (single) (\)
     * are interpreted as regular expressions and the enclosing backslashes are removed.
     * Ending a target string with '\i' will cause capitalization to be ignored.
     * Regular Java pattern compilation rules apply.
     * If the enclosing backslashes are missing, then the target is treated as a literal string.
     *
     * [RegexOption.CANON_EQ] is activated for both, regex and literals.
     */
    private val regex = targets.map {
        when {
            it.startsWith("\\") && it.endsWith("\\") -> {
                Regex(it.substring(1, it.length - 1), RegexOption.CANON_EQ)
            }
            it.startsWith("\\") && it.endsWith("\\i") -> {
                Regex(it.substring(1, it.length - 2), setOf(RegexOption.CANON_EQ, RegexOption.IGNORE_CASE))
            }
            else -> {
                Regex(it, setOf(RegexOption.CANON_EQ, RegexOption.LITERAL))
            }
        }
    }

    /**
     * Validates a textual [Submission] based on the provided [Regex].
     *
     * @param submission The [Submission] to validate.
     */
    override fun validate(submission: Submission) {
        submission.verdicts.asSequence().forEach { verdict ->
            /* Perform sanity checks. */
            if (verdict.type != VerdictType.TEXT) {
                verdict.status = VerdictStatus.WRONG
                return@forEach
            }

            /* Perform text validation. */
            val text = verdict.text
            if (text == null) {
                verdict.status = VerdictStatus.WRONG
                return@forEach
            }

            if (regex.any { it matches text })  {
                verdict.status = VerdictStatus.CORRECT
            } else {
                verdict.status = VerdictStatus.WRONG
            }
        }
    }
}
