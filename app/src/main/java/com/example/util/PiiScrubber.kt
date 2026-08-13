package com.example.util

import java.util.regex.Pattern

data class PiiScrubResult(
    val scrubbedText: String,
    val redactedCount: Int
)

object PiiScrubber {
    private val emailPattern = Pattern.compile("(?i)\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}\\b")
    private val phonePattern = Pattern.compile("\\b(?:\\+?\\d{1,3}[\\s-]?)?(?:\\(?\\d{2,4}\\)?[\\s-]?)?\\d{3}[\\s-]?\\d{3,4}\\b")
    private val cardPattern = Pattern.compile("\\b(?:\\d[ -]*?){13,16}\\b")
    private val idPattern = Pattern.compile("\\b[A-Z]{1,2}\\d{6,9}\\b")
    private val ssnPattern = Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b")

    fun scrub(input: String): PiiScrubResult {
        if (input.isBlank()) return PiiScrubResult("", 0)

        var count = 0
        var current = input

        fun replaceAll(pattern: Pattern, replacement: String) {
            val matcher = pattern.matcher(current)
            while (matcher.find()) {
                count++
            }
            current = matcher.replaceAll(replacement)
        }

        replaceAll(emailPattern, "[REDACTED_EMAIL]")
        replaceAll(phonePattern, "[REDACTED_PHONE]")
        replaceAll(cardPattern, "[REDACTED_CARD]")
        replaceAll(ssnPattern, "[REDACTED_SSN]")
        replaceAll(idPattern, "[REDACTED_ID]")

        return PiiScrubResult(current, count)
    }
}
