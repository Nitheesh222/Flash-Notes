package com.flashnotes.app.data.model

/**
 * Result returned by an LLM provider after processing a document or query.
 */
sealed class SummaryResult {

    /** Successful LLM response with the generated text. */
    data class Success(
        val text: String,
        val tokensUsed: Int = 0
    ) : SummaryResult()

    /** The request is currently being processed. */
    data object Loading : SummaryResult()

    /** The request failed with an error. */
    data class Error(
        val message: String,
        val exception: Throwable? = null
    ) : SummaryResult()
}
