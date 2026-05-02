package com.flashnotes.app.data.model

/**
 * Represents the metadata of a document stored in internal storage.
 * The actual file lives on disk; this model stores the reference + extracted info.
 */
data class DocumentMetadata(
    val id: String,
    val originalFileName: String,
    val internalFilePath: String,
    val sourceType: SourceType,
    val fileSizeBytes: Long,
    val dateAdded: Long = System.currentTimeMillis(),
    val summary: String? = null,
    val notebookId: Int? = null
)
