package dev.dres.run.validation

import dev.dres.data.model.submissions.Submission
import dev.dres.data.model.submissions.SubmissionStatus
import dev.dres.data.model.submissions.aspects.TextAspect
import dev.dres.run.validation.interfaces.SubmissionValidator


class TextValidator(targets: List<String>) : SubmissionValidator {

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

    override fun validate(submission: Submission) {

        if (submission !is TextAspect) {
            submission.status = SubmissionStatus.WRONG
            return
        }

        if (regex.any { it matches submission.text })  {
            submission.status = SubmissionStatus.CORRECT
        } else {
            submission.status = SubmissionStatus.WRONG
        }
    }

    override val deferring = false
}
