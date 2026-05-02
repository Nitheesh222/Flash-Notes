package com.flashnotes.app.data.llm

import android.util.Base64
import com.flashnotes.app.data.model.SummaryResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Real LLM provider that calls the Google Gemini 1.5 Flash REST API.
 *
 * Supports:
 *  - Text summarization
 *  - Document summarization (PDF / image sent as base64 inline_data)
 *  - Flashcard generation (structured Q&A output)
 *  - Contextual question answering
 *
 * Swap into LlmManager at app startup:
 *   LlmManager.setProvider(GeminiLlmProvider(apiKey = "YOUR_KEY"))
 */
class GeminiLlmProvider(private val apiKey: String) : LlmProvider {

    override val providerName: String = "Google Gemini 2.0 Flash"
    override val requiresInternet: Boolean = true

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    // The base URL is now constructed dynamically in callGemini

    // ─────────────────────────────────────────────────────────────────────────
    //  Public API
    // ─────────────────────────────────────────────────────────────────────────

    override suspend fun summarizeDocument(file: File, mimeType: String): SummaryResult {
        val prompt = """
            You are an expert study assistant. Carefully analyze the document provided and produce 
            a clear, well-structured summary. The summary should:
            - Start with a brief overview (2-3 sentences)
            - List the key concepts, themes, and definitions
            - Highlight any important facts, formulas, or takeaways
            - Be concise yet comprehensive enough to understand the document without reading it
            
            Format the output with clear headings and bullet points.
        """.trimIndent()

        return callGemini(prompt, file, mimeType)
    }

    override suspend fun summarizeText(text: String): SummaryResult {
        val prompt = """
            You are an expert study assistant. Summarize the following text clearly and concisely.
            Include:
            - A 2-3 sentence overview
            - Key concepts and definitions as bullet points
            - Important takeaways

            Text to summarize:
            ---
            $text
            ---
        """.trimIndent()

        return callGemini(prompt)
    }

    override suspend fun generateFlashcards(file: File, mimeType: String, count: Int): SummaryResult {
        val prompt = """
            You are a flashcard generation expert. Analyze the document and create exactly $count 
            high-quality flashcards that cover the most important concepts.

            Return ONLY the flashcards in this exact format (no extra text before or after):

            Q1: [Question]
            A1: [Answer]

            Q2: [Question]
            A2: [Answer]

            Q3: [Question]
            A3: [Answer]

            Q4: [Question]
            A4: [Answer]

            Q5: [Question]
            A5: [Answer]

            Rules:
            - Questions should test understanding, not just recall
            - Answers should be concise (1-3 sentences max)
            - Cover different topics from the document
            - Use simple, clear language
        """.trimIndent()

        return callGemini(prompt, file, mimeType)
    }

    override suspend fun answerQuestion(question: String, contextText: String): SummaryResult {
        val prompt = """
            You are an AI assistant inside a flashnotes app.

            Your job is to answer user questions strictly based on the provided source content.

            RULES:
            1. Only use the information from the given source file or extracted notes.
            2. Do NOT use outside knowledge.
            3. If the answer is not present in the source, respond with:
               "This information is not available in the provided notes."
            4. Keep answers clear, concise, and easy to understand.
            5. When possible, quote or reference the exact part of the notes.
            6. Do NOT make assumptions or guesses.
            7. Stay focused on the topic of the uploaded content.

            CONTEXT:
            $contextText

            USER QUESTION:
            $question

            ANSWER:
        """.trimIndent()

        return callGemini(prompt)
    }

    override fun isAvailable(): Boolean = apiKey.isNotBlank()

    // ─────────────────────────────────────────────────────────────────────────
    //  Internal helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * List of models to try in order of priority. 
     * This helps bypass strict per-model quota limits (e.g., 20 RPD on 2.5 Flash).
     */
    private val modelPriorityList = listOf(
        "gemini-2.0-flash-lite",
        "gemini-2.0-flash",
        "gemini-flash-latest",
        "gemini-2.5-flash-lite",
        "gemini-1.5-flash-8b-latest"
    )

    /**
     * Calls the Gemini REST API with an optional file attachment.
     * Files are sent as base64-encoded inline_data parts.
     */
    private suspend fun callGemini(
        prompt: String,
        file: File? = null,
        mimeType: String? = null,
        maxRetries: Int = 2
    ): SummaryResult {
        var attempt = 0
        var lastErrorMsg = ""
        var lastErrorCode = 0

        while (attempt <= maxRetries) {
            try {
                val requestBody = buildRequestBody(prompt, file, mimeType)
                
                // Pick a model from the priority list based on the current attempt
                val modelIndex = attempt % modelPriorityList.size
                val model = modelPriorityList[modelIndex]
                
                // Using v1beta as it supports a wider range of models for the fallback chain
                val requestUrl = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
                
                val request = Request.Builder()
                    .url(requestUrl)
                    .post(requestBody.toRequestBody("application/json".toMediaType()))
                    .addHeader("Content-Type", "application/json")
                    .build()

                // Switch to IO thread — OkHttp execute() is a blocking call
                val (response, responseBody) = withContext(Dispatchers.IO) {
                    val resp = client.newCall(request).execute()
                    Pair(resp, resp.body?.string() ?: "")
                }

                if (!response.isSuccessful) {
                    lastErrorCode = response.code
                    lastErrorMsg = extractErrorMessage(responseBody, response.code)
                    
                    // If capacity is exhausted (503), rate limited (429), or model not found/retired (404)
                    // we increment 'attempt' which will cause the next model in the list to be used.
                    if (response.code == 503 || response.code == 429 || response.code == 404) {
                        attempt++
                        if (attempt < modelPriorityList.size) {
                            // Exponential backoff: shorter for 404/503, longer for 429
                            val delayMs = if (response.code == 429) 4000L else 1000L
                            kotlinx.coroutines.delay(delayMs)
                            continue
                        }
                    }
                    return SummaryResult.Error("Gemini API error (${response.code}): $lastErrorMsg")
                }

                val text = extractGeneratedText(responseBody)
                if (text.isBlank()) {
                    return SummaryResult.Error("Gemini returned an empty response. Please try again.")
                } else {
                    return SummaryResult.Success(text = text, tokensUsed = extractTokenCount(responseBody))
                }
            } catch (e: Exception) {
                if (attempt < maxRetries) {
                    attempt++
                    kotlinx.coroutines.delay(1000L * attempt)
                    continue
                }
                return SummaryResult.Error(
                    message = "Network error: ${e.message ?: "Unknown error"}",
                    exception = e
                )
            }
        }
        return SummaryResult.Error("Gemini API error ($lastErrorCode): $lastErrorMsg")
    }

    /**
     * Builds the Gemini API JSON request body.
     * Supports optional inline file data (base64) for PDFs and images.
     */
    private fun buildRequestBody(
        prompt: String,
        file: File?,
        mimeType: String?
    ): String {
        val parts = JSONArray()

        // Text prompt part
        parts.put(
            JSONObject().apply {
                put("text", prompt)
            }
        )

        // Inline file part (base64) — Gemini supports up to ~20 MB inline
        if (file != null && mimeType != null && file.exists()) {
            try {
                val fileBytes = file.readBytes()
                val base64Data = Base64.encodeToString(fileBytes, Base64.NO_WRAP)
                parts.put(
                    JSONObject().apply {
                        put("inline_data", JSONObject().apply {
                            put("mime_type", mimeType)
                            put("data", base64Data)
                        })
                    }
                )
            } catch (e: Exception) {
                // File read failed — proceed with text-only prompt
            }
        }

        val content = JSONObject().apply {
            put("role", "user")
            put("parts", parts)
        }

        val generationConfig = JSONObject().apply {
            put("temperature", 0.4)
            put("maxOutputTokens", 2048)
        }

        return JSONObject().apply {
            put("contents", JSONArray().put(content))
            put("generationConfig", generationConfig)
        }.toString()
    }

    /** Parses the generated text from the Gemini response JSON. */
    private fun extractGeneratedText(responseJson: String): String {
        return try {
            val root = JSONObject(responseJson)
            val candidates = root.getJSONArray("candidates")
            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.getJSONObject("content")
            val parts = content.getJSONArray("parts")
            val text = StringBuilder()
            for (i in 0 until parts.length()) {
                val part = parts.getJSONObject(i)
                if (part.has("text")) {
                    text.append(part.getString("text"))
                }
            }
            text.toString().trim()
        } catch (e: Exception) {
            ""
        }
    }

    /** Extracts total token count from the Gemini response (for info purposes). */
    private fun extractTokenCount(responseJson: String): Int {
        return try {
            val root = JSONObject(responseJson)
            val usage = root.getJSONObject("usageMetadata")
            usage.optInt("totalTokenCount", 0)
        } catch (e: Exception) {
            0
        }
    }

    /** Extracts a human-readable error message from an API error response. */
    private fun extractErrorMessage(responseJson: String, code: Int): String {
        return try {
            val root = JSONObject(responseJson)
            val error = root.getJSONObject("error")
            error.optString("message", "HTTP $code")
        } catch (e: Exception) {
            "HTTP $code"
        }
    }
}
