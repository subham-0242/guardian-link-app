package com.example.util

import com.example.data.model.SosReport

data class HazardCluster(
    val categoryKey: String,
    val categoryLabel: String,
    val floor: Int,
    val topTags: List<String>,
    val reports: List<SosReport>
)

object HazardClusterer {
    private val FIRE_KEYWORDS = setOf("fire", "smoke", "burn", "flame", "heat", "hot")
    private val FLOOD_KEYWORDS = setOf("flood", "water", "leak", "pipe", "burst", "soaking")
    private val STRUCTURAL_KEYWORDS = setOf("collapse", "structural", "crack", "debris", "rubble", "falling")
    private val MEDICAL_KEYWORDS = setOf("medical", "injury", "unconscious", "bleeding", "faint", "hurt", "pain")
    private val CHEMICAL_KEYWORDS = setOf("gas", "chemical", "toxic", "fumes", "smell", "poison")

    fun classifyCategory(text: String): Pair<String, String> {
        val lower = text.lowercase()
        return when {
            FIRE_KEYWORDS.any { lower.contains(it) } -> "fire" to "Fire / Smoke"
            FLOOD_KEYWORDS.any { lower.contains(it) } -> "flood" to "Water / Flood"
            STRUCTURAL_KEYWORDS.any { lower.contains(it) } -> "structural" to "Structural Hazard"
            MEDICAL_KEYWORDS.any { lower.contains(it) } -> "medical" to "Medical Emergency"
            CHEMICAL_KEYWORDS.any { lower.contains(it) } -> "chemical" to "Chemical / Gas Leak"
            else -> "general" to "General Distress"
        }
    }

    fun clusterReports(reports: List<SosReport>): List<HazardCluster> {
        val grouped = reports.groupBy { report ->
            val (catKey, _) = classifyCategory(report.message)
            "$catKey-floor-${report.floor}"
        }

        return grouped.map { (compositeKey, reportGroup) ->
            val sampleMsg = reportGroup.firstOrNull()?.message ?: ""
            val (catKey, catLabel) = classifyCategory(sampleMsg)
            val floor = reportGroup.firstOrNull()?.floor ?: 1

            // Count word frequencies for top 4 representative tags
            val tokenCounts = mutableMapOf<String, Int>()
            reportGroup.forEach { r ->
                val tokens = ReportDeduplicator.tokenize(r.message)
                tokens.forEach { token ->
                    tokenCounts[token] = (tokenCounts[token] ?: 0) + 1
                }
            }

            val top4Tags = tokenCounts.entries
                .sortedByDescending { it.value }
                .take(4)
                .map { it.key.replaceFirstChar { c -> c.uppercase() } }

            HazardCluster(
                categoryKey = catKey,
                categoryLabel = catLabel,
                floor = floor,
                topTags = if (top4Tags.isNotEmpty()) top4Tags else listOf("Emergency", "Floor $floor"),
                reports = reportGroup
            )
        }
    }
}
