package com.campussync.app.data

import android.util.Log
import com.campussync.app.BuildConfig
import com.google.gson.Gson
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Advanced Repository for Gemini AI.
 * Prioritizes Gemini 2.5 Flash models with automatic fallback to 2.0.
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
        // 1. Sanitize the API key
        val rawKey = try { BuildConfig.GEMINI_API_KEY } catch (e: Exception) { "" }
        val apiKey = rawKey.replace("\"", "").replace("'", "").trim()

        if (apiKey.isBlank()) {
            return Result.failure(Exception("API Key missing. Add to local.properties and Rebuild."))
        }

        // 2. Prioritized list of current Gemini Flash models
        val models = listOf(
            "gemini-3-flash-preview",        // Latest and most capable Flash model
            "gemini-3.1-flash-lite",         // Stable, cost-effective 3.x model
            "gemini-2.5-flash",              // Stable 2.5 model
            "gemini-2.5-flash-lite"          // Smallest, most cost-effective 2.5 model
        )

        for (model in models) {
            Log.d(TAG, "Attempting AI generation with model: $model")
            val result = makeApiCall(model, apiKey, prompt)

            if (result.isSuccess) {
                Log.i(TAG, "Successfully generated content using: $model")
                return result
            }

            val error = result.exceptionOrNull()?.message ?: ""

            // If the error is 404 (Model Not Found), we proceed to the next model in the list
            if (error.contains("404")) {
                Log.w(TAG, "Model $model returned 404 (Not Found). Trying fallback...")
                continue
            } else {
                // If it's a different error (like 403 Invalid Key or 429 Rate Limit),
                // we stop and report it immediately.
                return result
            }
        }

        return Result.failure(Exception("None of the specified Gemini models are currently available for this API key."))
    }

    private suspend fun makeApiCall(model: String, apiKey: String, prompt: String): Result<String> = suspendCoroutine { continuation ->
        val requestMap = mapOf(
            "contents" to listOf(
                mapOf("parts" to listOf(mapOf("text" to prompt)))
            )
        )
        val body = gson.toJson(requestMap).toRequestBody(mediaType)

        // All current Gemini models use the v1beta endpoint
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent"

        val request = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .addHeader("x-goog-api-key", apiKey) // Use header instead of query param
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                continuation.resume(Result.failure(e))
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val responseBody = response.body?.string() ?: ""
                    if (!response.isSuccessful) {
                        continuation.resume(Result.failure(Exception("HTTP ${response.code}: $responseBody")))
                        return
                    }

                    try {
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