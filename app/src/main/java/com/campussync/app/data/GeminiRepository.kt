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
 * Advanced Repository for Gemini AI.
 * Handles content generation, timetable extraction, and assessment extraction.
 */
class GeminiRepository {

    private val gson = Gson()
    private val TAG = "GeminiRepository"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val mediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun generateContent(prompt: String): Result<String> {
        val apiKey = getApiKey()
        if (apiKey.isBlank()) return Result.failure(Exception("API Key missing"))

        val models = getModelList()
        for (model in models) {
            val result = makeApiCall(model, apiKey, prompt, null, null)
            if (result.isSuccess) return result
            if (result.exceptionOrNull()?.message?.contains("404") != true) return result
        }
        return Result.failure(Exception("All models failed"))
    }

    /**
     * Extracts structured timetable data from a base64 encoded image.
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

            Return ONLY a valid JSON array with no additional text, markdown, or code 
            fences. The response must be parseable by Gson.

            Each entry must have exactly these fields:
            {
              "moduleCode": "string - e.g., OPSC6312",
              "moduleName": "string - e.g., Open Source Coding",
              "dayOfWeek": "string - Monday, Tuesday, Wednesday, Thursday, Friday, Saturday, or Sunday",
              "startTime": "string in 24-hour format - e.g., 09:00",
              "endTime": "string in 24-hour format - e.g., 10:30",
              "venue": "string - room or building name"
            }

            Rules:
            - If a field is not visible, use "N/A"
            - Do NOT include duplicate entries
            - Ensure times are in 24-hour HH:MM format
            - If no timetable is visible, return an empty array []

            Image:
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
            if (result.exceptionOrNull()?.message?.contains("404") != true) return Result.failure(result.exceptionOrNull()!!)
        }
        return Result.failure(Exception("Extraction failed"))
    }

    /**
     * Extracts assessment deadlines from a Programme Assessment Schedule (PAS) image.
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

            Do NOT include rows from other programme codes. For example, if the filter 
            is "DIS3", only extract rows with Programme Code "DIS3" and skip all rows 
            with "DIS1" or "DIS2".
            """.trimIndent()
        } else {
            ""
        }

        val prompt = """
            You are an OCR and data extraction assistant for a university assessment 
            schedule (called a PAS - Programme Assessment Schedule).

            Analyze the uploaded image and extract ALL assessment entries (assignments, 
            tests, exams, portfolios of evidence, practical assessments).

            $filterInstruction

            Return ONLY a valid JSON array with no additional text, markdown, or code 
            fences. The response must be parseable by Gson.

            Each entry must have exactly these fields:
            {
              "programmeCode": "string - the exact Programme Code from the row (e.g., 'DIS3')",
              "moduleCode": "string - e.g., OPSC6312",
              "moduleName": "string - e.g., Open Source Coding",
              "assessmentType": "string - e.g., Test 1 Sitting 1, Assignment 2, Portfolio of Evidence (POE), Practical Assessment 1",
              "submissionMethod": "string - either 'Online Submission' or 'Campus Sitting'",
              "requiresTurnitin": true or false,
              "dueDate": "string in ISO format YYYY-MM-DD - convert dates like '15-Sep-26' to '2026-09-15'",
              "dueTime": "string in 24-hour HH:MM format - e.g., '23:50'"
            }

            Rules:
            - Extract EVERY row from the table, even if the date column seems short
            - Convert dates: '15-Sep-26' -> '2026-09-15'; '30-Nov-26' -> '2026-11-30'
            - Preserve assessment types exactly as shown (e.g., 'Test 1 Sitting 1')
            - If a field is missing, use "N/A"
            - If the image is not a PAS/assessment schedule, return an empty array []
            - Do NOT skip matching rows

            Image:
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
            if (result.exceptionOrNull()?.message?.contains("404") != true) return Result.failure(result.exceptionOrNull()!!)
        }
        return Result.failure(Exception("Assessment extraction failed"))
    }

    private fun getApiKey() = BuildConfig.GEMINI_API_KEY.replace("\"", "").replace("'", "").trim()

    private fun getModelList() = listOf(
        "gemini-3-flash-preview",
        "gemini-3.1-flash-lite",
        "gemini-2.5-flash",
        "gemini-2.5-flash-lite"
    )

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
                        val responseBody = response.body?.string() ?: ""
                        val geminiResponse = gson.fromJson(responseBody, GeminiResponse::class.java)
                        val text = geminiResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                        if (text != null) {
                            continuation.resume(Result.success(text))
                        } else {
                            continuation.resume(Result.failure(Exception("Empty AI Response")))
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
