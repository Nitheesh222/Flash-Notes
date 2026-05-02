package com.flashnotes.app.data.llm

/**
 * Singleton manager that provides access to the current LLM provider.
 *
 * Usage:
 *   // Get the current provider
 *   val llm = LlmManager.getProvider()
 *   val result = llm.summarizeText("Some notes...")
 *
 *   // Swap to a different provider:
 *   LlmManager.setProvider(PlaceholderLlmProvider())  // offline / testing
 *   LlmManager.setProvider(GeminiLlmProvider(apiKey = "YOUR_KEY")) // production
 */
object LlmManager {

    /** Google Gemini 2.0 Flash API key. */
    private const val GEMINI_API_KEY = "YOUR_API_KEY_HERE"

    @Volatile
    private var currentProvider: LlmProvider = GeminiLlmProvider(apiKey = GEMINI_API_KEY)

    /**
     * Returns the currently configured LLM provider.
     */
    fun getProvider(): LlmProvider = currentProvider

    /**
     * Replaces the active LLM provider.
     *
     * Call this at app startup (or from settings) to switch between:
     *   - PlaceholderLlmProvider   (testing / offline fallback)
     *   - GeminiLlmProvider        (Google Gemini Flash API)
     *   - OpenAiLlmProvider        (OpenAI GPT-4o mini)
     *   - OnDeviceLlmProvider      (Gemma 2B via MediaPipe)
     */
    fun setProvider(provider: LlmProvider) {
        currentProvider = provider
    }

    /**
     * Checks if the current provider is ready to process requests.
     */
    fun isReady(): Boolean = currentProvider.isAvailable()

    /**
     * Returns the name of the active provider (for display in UI).
     */
    fun getProviderName(): String = currentProvider.providerName
}
