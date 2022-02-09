package dev.dres.run.validation

import dev.dres.data.model.submissions.Submission
import dev.dres.data.model.submissions.SubmissionStatus
import dev.dres.data.model.submissions.aspects.TextAspect
import dev.dres.run.validation.interfaces.SubmissionValidator
import java.util.regex.Pattern

class TextValidator(targets: List<String>) : SubmissionValidator {

    /**
     * Transforms the targets to [Pattern]s.
     * There is the convention introduced, that targets padded in backslashes (single) (\)
     * are interpreted as regular expressions and the enclosing backslashes are removed.
     * Regular Java pattern compilation rules apply.
     * If the enclosing backslashes are missing, then the target is treated as a literal string.
     *
     * [Pattern.CANON_EQ] is activated for both, regex and literals.
     */
    private val regexes = targets.map {
        if(it.startsWith("\\") && it.endsWith("\\")) {
            it.substring(1, it.length - 1).toRegex(RegexOption.CANON_EQ)
        } else {
            it.toRegex(setOf(RegexOption.CANON_EQ, RegexOption.LITERAL))
        }
    }

    override fun validate(submission: Submission) {

        if (submission !is TextAspect) {
            submission.status = SubmissionStatus.WRONG
            return
        }

        if (regexes.any { submission.text matches it })  {
            submission.status = SubmissionStatus.CORRECT
        } else {
            submission.status = SubmissionStatus.WRONG
        }
    }

    override val deferring = false
}
