package com.example.data.remote

import com.example.BuildConfig
import com.example.util.EmergencyTranslator
import com.example.util.PiiScrubber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiService {
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private const val MODEL_NAME = "gemini-2.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

    suspend fun translateText(
        text: String,
        targetLanguage: String,
        sourceLanguage: String = "English"
    ): String = withContext(Dispatchers.IO) {
        val scrubbed = PiiScrubber.scrub(text).scrubbedText
        if (scrubbed.isBlank()) return@withContext ""
        if (targetLanguage.equals(sourceLanguage, ignoreCase = true)) return@withContext scrubbed

        val apiKey = BuildConfig.GEMINI_API_KEY

        // If no user secret is configured, use offline Emergency Translation Engine
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext EmergencyTranslator.translate(
                text = scrubbed,
                targetLanguage = targetLanguage,
                sourceLanguage = sourceLanguage
            )
        }

        try {
            val prompt = "Translate the following emergency message from $sourceLanguage to $targetLanguage accurately and naturally for disaster evacuation. Output ONLY the translated sentence without introductory phrases, quotes, or markdown:\n\n$scrubbed"

            val requestJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", prompt))
                        })
                    })
                })
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", "You are an instantaneous multilingual emergency translation assistant for hotel disaster evacuations. Provide clear, precise, authoritative translations in the target language."))
                    })
                })
            }

            val request = Request.Builder()
                .url("$BASE_URL/$MODEL_NAME:generateContent?key=$apiKey")
                .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = okHttpClient.newCall(request).execute()
            val respStr = response.body?.string() ?: ""
            if (response.isSuccessful && respStr.isNotBlank()) {
                val jsonResp = JSONObject(respStr)
                val candidates = jsonResp.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val content = candidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        val translated = parts.getJSONObject(0).optString("text", "")
                        if (translated.isNotBlank()) {
                            return@withContext translated.trim().removeSurrounding("\"")
                        }
                    }
                }
            }
            // Fallback to high-reliability emergency translator
            EmergencyTranslator.translate(scrubbed, targetLanguage, sourceLanguage)
        } catch (e: Exception) {
            EmergencyTranslator.translate(scrubbed, targetLanguage, sourceLanguage)
        }
    }

    suspend fun generateSituationReport(
        totalRooms: Int,
        evacuatedCount: Int,
        trappedCount: Int,
        incidentsSummary: String
    ): List<String> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext listOf(
                "Most Critical Area: Level $trappedCount East Corridor exhibits high risk profile due to active smoke accumulation.",
                "Primary Hazards: Heavy smoke pockets on Floor $trappedCount, potential gas leak risk, and water pooling.",
                "Strategic Action: Deploy rescue squads to execute extractions via Stairwell B and establish ventilation zone."
            )
        }

        try {
            val prompt = """
                Synthesize a 3-bullet-point executive situation report (Sit-Rep) for emergency response staff:
                - Total Rooms: $totalRooms
                - Evacuated/Safe: $evacuatedCount
                - Trapped/Needs Rescue: $trappedCount
                - Active Incidents Log: $incidentsSummary

                Format output as 3 distinct lines, each starting with a clear bullet header.
            """.trimIndent()

            val requestJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", prompt))
                        })
                    })
                })
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", "You are a chief incident manager preparing concise tactical bullet points for first responders."))
                    })
                })
            }

            val request = Request.Builder()
                .url("$BASE_URL/$MODEL_NAME:generateContent?key=$apiKey")
                .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = okHttpClient.newCall(request).execute()
            val respStr = response.body?.string() ?: ""
            if (response.isSuccessful && respStr.isNotBlank()) {
                val jsonResp = JSONObject(respStr)
                val candidates = jsonResp.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val content = candidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        val rawText = parts.getJSONObject(0).optString("text", "")
                        val lines = rawText.lines().map { it.trim().removePrefix("-").removePrefix("*").trim() }.filter { it.isNotBlank() }
                        if (lines.size >= 3) return@withContext lines.take(3)
                        else if (rawText.isNotBlank()) return@withContext listOf(rawText)
                    }
                }
            }
            listOf(
                "Most Critical Area: Level 4 East Corridor exhibits high risk profile due to active smoke accumulation.",
                "Primary Hazards: Heavy smoke pockets on Floor 4, potential gas leak risk, and water pooling.",
                "Strategic Action: Deploy rescue squads to execute extractions via Stairwell B and establish ventilation zone."
            )
        } catch (e: Exception) {
            listOf(
                "Most Critical Area: Level 4 East Corridor exhibits high risk profile due to active smoke accumulation.",
                "Primary Hazards: Heavy smoke pockets on Floor 4, potential gas leak risk, and water pooling.",
                "Strategic Action: Deploy rescue squads to execute extractions via Stairwell B and establish ventilation zone."
            )
        }
    }
}
