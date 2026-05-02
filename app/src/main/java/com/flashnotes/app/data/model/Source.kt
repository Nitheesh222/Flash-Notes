package com.flashnotes.app.data.model

enum class SourceType {
    PDF, AUDIO, IMAGE, WEBSITE, YOUTUBE, TEXT
}

data class Source(
    val id: Int,
    val name: String,
    val type: SourceType,
    val isLoading: Boolean = false,
    /** Absolute path to the file in internal storage (null if not yet saved). */
    val filePath: String? = null,
    /** Unique document ID for linking to MetadataStore entries. */
    val documentId: String? = null
)
