package com.flashnotes.app.data.llm

import com.flashnotes.app.data.model.SummaryResult
import kotlinx.coroutines.delay
import java.io.File

/**
 * Placeholder LLM provider that returns mock/simulated responses.
 *
 * Use this for development and UI testing. When you're ready to integrate
 * a real LLM, create a new class (e.g. GeminiLlmProvider) implementing
 * [LlmProvider] and swap it via [LlmManager.setProvider].
 *
 * ┌─────────────────────────────────────────────────────────────────┐
 * │  TO INTEGRATE A REAL LLM, REPLACE THIS CLASS:                  │
 * │                                                                │
 * │  Option A — Cloud API (Gemini Flash / OpenAI):                 │
 * │    1. Add the SDK dependency to build.gradle.kts               │
 * │    2. Create GeminiLlmProvider implementing LlmProvider        │
 * │    3. Call API endpoints in each method                        │
 * │    4. Swap: LlmManager.setProvider(GeminiLlmProvider(apiKey))  │
 * │                                                                │
 * │  Option B — On-Device (Gemma / MediaPipe):                     │
 * │    1. Add MediaPipe LLM Inference dependency                   │
 * │    2. Create OnDeviceLlmProvider implementing LlmProvider      │
 * │    3. Load model and run inference locally                     │
 * │    4. Swap: LlmManager.setProvider(OnDeviceLlmProvider(ctx))   │
 * └─────────────────────────────────────────────────────────────────┘
 */
class PlaceholderLlmProvider : LlmProvider {

    override val providerName: String = "Placeholder (Mock)"

    override val requiresInternet: Boolean = false

    override suspend fun summarizeDocument(file: File, mimeType: String): SummaryResult {
        // Simulate processing delay
        delay(1500)

        val fileName = file.name
        val fileSizeKb = file.length() / 1024

        return SummaryResult.Success(
            text = buildString {
                appendLine("📄 Summary of \"$fileName\"")
                appendLine("─".repeat(40))
                appendLine()
                appendLine("This is a placeholder summary generated for testing purposes.")
                appendLine()
                appendLine("In a production app, this is where the real LLM would:")
                appendLine("  • Extract text from the $mimeType file")
                appendLine("  • Identify key topics and themes")
                appendLine("  • Generate a concise, structured summary")
                appendLine("  • Highlight important concepts for flashcard generation")
                appendLine()
                appendLine("File size: ${fileSizeKb}KB")
                appendLine("Processing time: ~1.5s (simulated)")
            },
            tokensUsed = 0
        )
    }

    override suspend fun summarizeText(text: String): SummaryResult {
        delay(1000)

        val wordCount = text.split("\\s+".toRegex()).size
        val preview = text.take(100).replace("\n", " ")

        return SummaryResult.Success(
            text = buildString {
                appendLine("📝 Text Summary")
                appendLine("─".repeat(40))
                appendLine()
                appendLine("Source text: $wordCount words")
                appendLine("Preview: \"$preview...\"")
                appendLine()
                appendLine("This is a placeholder summary. Connect a real LLM provider")
                appendLine("(Gemini Flash API or on-device Gemma) to generate actual summaries.")
                appendLine()
                appendLine("Key placeholder topics:")
                appendLine("  1. Topic identification would appear here")
                appendLine("  2. Main concepts would be extracted")
                appendLine("  3. Actionable insights would be highlighted")
            },
            tokensUsed = 0
        )
    }

    override suspend fun generateFlashcards(file: File, mimeType: String, count: Int): SummaryResult {
        delay(2000)

        return SummaryResult.Success(
            text = buildString {
                appendLine("🃏 Generated Flashcards (Placeholder)")
                appendLine("─".repeat(40))
                appendLine()
                for (i in 1..count) {
                    appendLine("Q$i: [Placeholder question $i about ${file.name}]")
                    appendLine("A$i: [Placeholder answer $i — real LLM would extract this from the document]")
                    appendLine()
                }
                appendLine("─".repeat(40))
                appendLine("To generate real flashcards, integrate a Cloud or On-Device LLM.")
            },
            tokensUsed = 0
        )
    }

    override suspend fun answerQuestion(question: String, contextText: String): SummaryResult {
        delay(1200)

        return SummaryResult.Success(
            text = buildString {
                appendLine("🤖 AI Response (Placeholder)")
                appendLine()
                appendLine("You asked: \"$question\"")
                appendLine()
                appendLine("This is a placeholder response. In a production app, the LLM would:")
                appendLine("  • Analyze your uploaded sources (${contextText.take(50)}...)")
                appendLine("  • Find relevant passages matching your question")
                appendLine("  • Synthesize an accurate, cited answer")
                appendLine()
                appendLine("To enable real Q&A, configure an LLM provider in LlmManager:")
                appendLine("  → Gemini Flash API (free tier, great for long documents)")
                appendLine("  → OpenAI GPT-4o mini (low cost, excellent vision)")
                appendLine("  → On-device Gemma 2B (fully offline, zero cost)")
            },
            tokensUsed = 0
        )
    }

    override fun isAvailable(): Boolean = true
}
