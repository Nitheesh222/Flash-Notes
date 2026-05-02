package com.flashnotes.app.data.llm

import com.flashnotes.app.data.model.SummaryResult
import java.io.File

/**
 * Contract that any LLM provider must implement.
 *
 * This interface defines the capabilities your app needs from an LLM:
 *   1. Summarize a document (PDF, image, text)
 *   2. Generate flashcards from content
 *   3. Answer questions about uploaded sources
 *
 * To integrate a real LLM (e.g. Gemini Flash API, OpenAI API, On-Device Gemma),
 * create a new class implementing this interface and swap it via [LlmManager].
 *
 * Example future implementations:
 *   - GeminiLlmProvider      → Cloud API (free tier)
 *   - OpenAiLlmProvider      → Cloud API (pay-per-use)
 *   - OnDeviceLlmProvider    → MediaPipe / Gemma 2B (offline)
 */
interface LlmProvider {

    /** Human-readable name of this provider (for UI display). */
    val providerName: String

    /** Whether this provider requires internet connectivity. */
    val requiresInternet: Boolean

    /**
     * Summarizes the content of a document file.
     *
     * @param file      The document file (PDF, image, or text).
     * @param mimeType  The MIME type (e.g. "application/pdf", "image/jpeg").
     * @return          A [SummaryResult] containing the summarized text.
     */
    suspend fun summarizeDocument(file: File, mimeType: String): SummaryResult

    /**
     * Summarizes raw text content directly.
     *
     * @param text  The source text to summarize.
     * @return      A [SummaryResult] containing the summarized text.
     */
    suspend fun summarizeText(text: String): SummaryResult

    /**
     * Generates flashcard Q&A pairs from document content.
     *
     * @param file      The document file.
     * @param mimeType  The MIME type.
     * @param count     How many flashcards to generate.
     * @return          A [SummaryResult] where `text` contains formatted Q&A pairs.
     */
    suspend fun generateFlashcards(file: File, mimeType: String, count: Int = 5): SummaryResult

    /**
     * Answers a user question based on the provided context/source text.
     *
     * @param question     The user's question.
     * @param contextText  The source material to reference when answering.
     * @return             A [SummaryResult] containing the answer.
     */
    suspend fun answerQuestion(question: String, contextText: String): SummaryResult

    /**
     * Checks if the provider is ready to accept requests.
     * (e.g. API key configured, model loaded, device supported)
     */
    fun isAvailable(): Boolean
}
