package com.campussync.app.data

import android.util.Log
import com.campussync.app.BuildConfig
import com.campussync.app.models.ScannedAssessment
import com.campussync.app.models.ScannedTimetableEntry
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Repository for interacting with the Google Gemini AI API.
 * Supports text generation and vision-based data extraction for timetables and assessments.
 */
class GeminiRepository {

    private val gson = Gson()
    private val TAG = "GeminiRepository"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val mediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * Generates a text response from the AI based on a given prompt.
     */
    suspend fun generateContent(prompt: String): Result<String> {
        val apiKey = getApiKey()
        if (apiKey.isBlank()) return Result.failure(Exception("API Key missing"))

        val models = getModelList()

        for (model in models) {
            val result = makeApiCall(model, apiKey, prompt, null, null)

            if (result.isSuccess) return result
            
            val error = result.exceptionOrNull()?.message ?: ""
            if (!error.contains("404")) return result
        }

        return Result.failure(Exception("AI services are currently unavailable."))
    }

    /**
     * Extracts structured timetable information from an image.
     */
    suspend fun extractTimetableFromImage(
        imageBase64: String,
        mimeType: String
    ): Result<List<ScannedTimetableEntry>> {
        val apiKey = getApiKey()
        if (apiKey.isBlank()) return Result.failure(Exception("API Key missing"))

        val prompt = """
            You are an OCR and data extraction assistant. Analyze the uploaded timetable 
            image and extract all class entries.

            Return ONLY a valid JSON array. Each entry must have exactly these fields:
            {
              "moduleCode": "string",
              "moduleName": "string",
              "dayOfWeek": "string",
              "startTime": "HH:MM",
              "endTime": "HH:MM",
              "venue": "string"
            }

            Rules:
            - Use "N/A" for missing fields.
            - Ensure 24-hour format.
            - If no timetable is found, return an empty array [].
        """.trimIndent()

        val models = getModelList()
        for (model in models) {
            val result = makeApiCall(model, apiKey, prompt, imageBase64, mimeType)
            if (result.isSuccess) {
                return try {
                    val json = cleanJsonResponse(result.getOrThrow())
                    val type = object : TypeToken<List<ScannedTimetableEntry>>() {}.type
                    Result.success(gson.fromJson(json, type))
                } catch (e: Exception) { Result.failure(e) }
            }
            if (result.exceptionOrNull()?.message?.contains("404") != true) {
                return Result.failure(result.exceptionOrNull()!!)
            }
        }
        return Result.failure(Exception("Timetable extraction failed."))
    }

    /**
     * Extracts assessment deadlines from a PAS image, applying a year-based filter.
     */
    suspend fun extractAssessmentsFromImage(
        imageBase64: String,
        mimeType: String,
        programmeCodeFilter: String
    ): Result<List<ScannedAssessment>> {
        val apiKey = getApiKey()
        if (apiKey.isBlank()) return Result.failure(Exception("API Key missing"))

        val filterInstruction = if (programmeCodeFilter.isNotBlank()) {
            """
            IMPORTANT FILTER: Only extract rows where the "Programme Code" column 
            matches EXACTLY: "$programmeCodeFilter"
            """.trimIndent()
        } else {
            "Extract all assessment entries from the schedule."
        }

        val prompt = """
            You are an OCR and data extraction assistant for a university assessment schedule (PAS).
            Extract ALL assessment entries. Return ONLY a valid JSON array.

            Fields: {
              "programmeCode": "string",
              "moduleCode": "string",
              "moduleName": "string",
              "assessmentType": "string",
              "submissionMethod": "string",
              "requiresTurnitin": true or false,
              "dueDate": "ISO format YYYY-MM-DD",
              "dueTime": "24-hour HH:MM format"
            }

            $filterInstruction
        """.trimIndent()

        val models = getModelList()
        for (model in models) {
            val result = makeApiCall(model, apiKey, prompt, imageBase64, mimeType)
            if (result.isSuccess) {
                return try {
                    val json = cleanJsonResponse(result.getOrThrow())
                    val type = object : TypeToken<List<ScannedAssessment>>() {}.type
                    Result.success(gson.fromJson(json, type))
                } catch (e: Exception) { Result.failure(e) }
            }
            if (result.exceptionOrNull()?.message?.contains("404") != true) {
                return Result.failure(result.exceptionOrNull()!!)
            }
        }
        return Result.failure(Exception("Assessment extraction failed."))
    }

    private fun getApiKey(): String {
        return BuildConfig.GEMINI_API_KEY.replace("\"", "").replace("'", "").trim()
    }

    private fun getModelList(): List<String> {
        return listOf(
            "gemini-3.5-flash",
            "gemini-3.5-flash-lite",
            "gemini-2.5-flash",
            "gemini-2.5-flash-lite",
            "gemini-2.0-flash",
            "gemini-1.5-flash"
        )
    }

    private fun cleanJsonResponse(raw: String): String {
        return raw.trim().removeSurrounding("```json", "```").trim()
    }

    private suspend fun makeApiCall(
        model: String,
        apiKey: String,
        prompt: String,
        imageBase64: String?,
        mimeType: String?
    ): Result<String> = suspendCoroutine { continuation ->
        val parts = mutableListOf<Map<String, Any>>(
            mapOf("text" to prompt)
        )
        
        if (imageBase64 != null && mimeType != null) {
            parts.add(mapOf(
                "inlineData" to mapOf(
                    "mimeType" to mimeType,
                    "data" to imageBase64
                )
            ))
        }

        val requestMap = mutableMapOf<String, Any>(
            "contents" to listOf(mapOf("parts" to parts))
        )
        
        if (imageBase64 != null) {
            requestMap["generationConfig"] = mapOf("responseMimeType" to "application/json")
        }

        val body = gson.toJson(requestMap).toRequestBody(mediaType)
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent"

        val request = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .addHeader("x-goog-api-key", apiKey)
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                continuation.resume(Result.failure(e))
            }
            
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        continuation.resume(Result.failure(Exception("Error ${response.code}")))
                    } else try {
                        val bodyString = response.body?.string() ?: ""
                        val geminiResponse = gson.fromJson(bodyString, GeminiResponse::class.java)
                        val text = geminiResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text

                        if (text != null) {
                            continuation.resume(Result.success(text))
                        } else {
                            continuation.resume(Result.failure(Exception("Empty response")))
                        }
                    } catch (e: Exception) {
                        continuation.resume(Result.failure(e))
                    }
                }
            }
        })
    }

    private data class GeminiResponse(val candidates: List<Candidate>?)
    private data class Candidate(val content: Content?)
    private data class Content(val parts: List<Part>?)
    private data class Part(val text: String?)
}
