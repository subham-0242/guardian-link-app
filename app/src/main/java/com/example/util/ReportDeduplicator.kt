package com.example.util

object ReportDeduplicator {
    private val STOP_WORDS = setOf(
        "the", "a", "is", "are", "to", "in", "on", "near", "with", "and", "or",
        "from", "room", "floor", "this", "that", "there", "have", "been", "was"
    )

    fun tokenize(text: String): Set<String> {
        return text.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .split("\\s+".toRegex())
            .filter { it.length >= 3 && !STOP_WORDS.contains(it) }
            .toSet()
    }

    fun jaccardSimilarity(tokensA: Set<String>, tokensB: Set<String>): Double {
        if (tokensA.isEmpty() && tokensB.isEmpty()) return 1.0
        if (tokensA.isEmpty() || tokensB.isEmpty()) return 0.0

        val intersection = tokensA.intersect(tokensB).size.toDouble()
        val union = tokensA.union(tokensB).size.toDouble()

        return if (union == 0.0) 0.0 else intersection / union
    }

    fun isDuplicate(
        reportAText: String,
        reportBText: String,
        roomA: String,
        roomB: String,
        floorA: Int,
        floorB: Int,
        threshold: Double = 0.82
    ): Boolean {
        if (roomA != roomB || floorA != floorB) return false
        val tokensA = tokenize(reportAText)
        val tokensB = tokenize(reportBText)
        return jaccardSimilarity(tokensA, tokensB) >= threshold
    }
}
