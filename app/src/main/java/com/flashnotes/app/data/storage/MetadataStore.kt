package com.flashnotes.app.data.storage

import android.content.Context
import android.content.SharedPreferences
import com.flashnotes.app.data.model.DocumentMetadata
import com.flashnotes.app.data.model.Flashcard
import com.flashnotes.app.data.model.SourceType

/**
 * Lightweight metadata store backed by SharedPreferences.
 *
 * Stores structured data about:
 *   • Documents (file paths, summaries, notebook links)
 *   • Notebooks (title, source count, date, icon type)
 *
 * This deliberately does NOT store the actual document files —
 * those live on disk via [DocumentStorageManager].
 */
class MetadataStore(context: Context) {

    companion object {
        private const val PREFS_NAME = "flashnotes_metadata"
        private const val KEY_DOCUMENT_IDS = "document_ids"
        private const val KEY_NOTEBOOK_IDS = "notebook_ids"
        private const val KEY_NEXT_NOTEBOOK_ID = "next_notebook_id"
        private const val SEPARATOR = "|||"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ─────────────────────────────────────────────────────────────
    //  Notebook persistence
    // ─────────────────────────────────────────────────────────────

    /**
     * Saves a notebook entry keyed by [id].
     * iconType is one of: "pdf", "image", "text" — used to pick the right drawable.
     */
    fun saveNotebook(id: Int, title: String, sourceCount: Int, date: String, iconType: String) {
        prefs.edit().apply {
            putString("nb_${id}_title", title)
            putInt("nb_${id}_sourceCount", sourceCount)
            putString("nb_${id}_date", date)
            putString("nb_${id}_iconType", iconType)

            val ids = getNotebookIds().toMutableSet()
            ids.add(id.toString())
            putString(KEY_NOTEBOOK_IDS, ids.joinToString(SEPARATOR))
            apply()
        }
    }

    /**
     * Returns all stored notebooks as property maps.
     * Keys: id (Int), title (String), sourceCount (Int), date (String), iconType (String).
     */
    fun getAllNotebooks(): List<Map<String, Any>> {
        return getNotebookIds().mapNotNull { idStr ->
            val id = idStr.toIntOrNull() ?: return@mapNotNull null
            val title = prefs.getString("nb_${id}_title", null) ?: return@mapNotNull null
            val sourceCount = prefs.getInt("nb_${id}_sourceCount", 1)
            val date = prefs.getString("nb_${id}_date", "") ?: ""
            val iconType = prefs.getString("nb_${id}_iconType", "pdf") ?: "pdf"
            mapOf<String, Any>(
                "id" to id,
                "title" to title,
                "sourceCount" to sourceCount,
                "date" to date,
                "iconType" to iconType
            )
        }
    }

    /**
     * Returns the next auto-increment notebook ID and bumps the counter.
     */
    fun getNextNotebookId(): Int {
        val next = prefs.getInt(KEY_NEXT_NOTEBOOK_ID, 1000)
        prefs.edit().putInt(KEY_NEXT_NOTEBOOK_ID, next + 1).apply()
        return next
    }

    /**
     * Updates the source count for an existing notebook.
     */
    fun updateNotebookSourceCount(notebookId: Int, newCount: Int) {
        prefs.edit().putInt("nb_${notebookId}_sourceCount", newCount).apply()
    }

    /**
     * Removes all metadata for a notebook.
     */
    fun deleteNotebook(notebookId: Int) {
        prefs.edit().apply {
            remove("nb_${notebookId}_title")
            remove("nb_${notebookId}_sourceCount")
            remove("nb_${notebookId}_date")
            remove("nb_${notebookId}_iconType")

            val ids = getNotebookIds().toMutableSet()
            ids.remove(notebookId.toString())
            putString(KEY_NOTEBOOK_IDS, ids.joinToString(SEPARATOR))
            apply()
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  Document metadata
    // ─────────────────────────────────────────────────────────────

    /**
     * Stores document metadata. The document ID is used as the key namespace.
     */
    fun saveDocumentMetadata(metadata: DocumentMetadata) {
        prefs.edit().apply {
            putString("${metadata.id}_fileName", metadata.originalFileName)
            putString("${metadata.id}_filePath", metadata.internalFilePath)
            putString("${metadata.id}_sourceType", metadata.sourceType.name)
            putLong("${metadata.id}_fileSize", metadata.fileSizeBytes)
            putLong("${metadata.id}_dateAdded", metadata.dateAdded)
            putString("${metadata.id}_summary", metadata.summary)
            putInt("${metadata.id}_notebookId", metadata.notebookId ?: -1)

            val ids = getDocumentIds().toMutableSet()
            ids.add(metadata.id)
            putString(KEY_DOCUMENT_IDS, ids.joinToString(SEPARATOR))

            apply()
        }
    }

    fun getDocumentMetadata(documentId: String): DocumentMetadata? {
        val fileName = prefs.getString("${documentId}_fileName", null) ?: return null
        val filePath = prefs.getString("${documentId}_filePath", null) ?: return null
        val sourceTypeName = prefs.getString("${documentId}_sourceType", null) ?: return null

        val sourceType = try {
            SourceType.valueOf(sourceTypeName)
        } catch (e: IllegalArgumentException) {
            SourceType.TEXT
        }

        val notebookId = prefs.getInt("${documentId}_notebookId", -1)

        return DocumentMetadata(
            id = documentId,
            originalFileName = fileName,
            internalFilePath = filePath,
            sourceType = sourceType,
            fileSizeBytes = prefs.getLong("${documentId}_fileSize", 0),
            dateAdded = prefs.getLong("${documentId}_dateAdded", 0),
            summary = prefs.getString("${documentId}_summary", null),
            notebookId = if (notebookId == -1) null else notebookId
        )
    }

    fun getAllDocuments(): List<DocumentMetadata> {
        return getDocumentIds().mapNotNull { id -> getDocumentMetadata(id) }
    }

    fun getDocumentsForNotebook(notebookId: Int): List<DocumentMetadata> {
        return getAllDocuments().filter { it.notebookId == notebookId }
    }

    fun saveSummary(documentId: String, summary: String) {
        prefs.edit().putString("${documentId}_summary", summary).apply()
    }

    fun saveFlashcards(documentId: String, flashcards: List<Flashcard>) {
        val jsonArray = org.json.JSONArray()
        for (card in flashcards) {
            val jsonObj = org.json.JSONObject()
            jsonObj.put("id", card.id)
            jsonObj.put("question", card.question)
            jsonObj.put("answer", card.answer)
            jsonArray.put(jsonObj)
        }
        prefs.edit().putString("${documentId}_flashcards", jsonArray.toString()).apply()
    }

    fun getFlashcards(documentId: String): List<Flashcard>? {
        val jsonString = prefs.getString("${documentId}_flashcards", null) ?: return null
        return try {
            val jsonArray = org.json.JSONArray(jsonString)
            val cards = mutableListOf<Flashcard>()
            for (i in 0 until jsonArray.length()) {
                val jsonObj = jsonArray.getJSONObject(i)
                cards.add(
                    Flashcard(
                        id = jsonObj.getInt("id"),
                        question = jsonObj.getString("question"),
                        answer = jsonObj.getString("answer")
                    )
                )
            }
            cards
        } catch (e: Exception) {
            null
        }
    }

    fun linkToNotebook(documentId: String, notebookId: Int) {
        prefs.edit().putInt("${documentId}_notebookId", notebookId).apply()
    }

    fun deleteDocumentMetadata(documentId: String) {
        prefs.edit().apply {
            remove("${documentId}_fileName")
            remove("${documentId}_filePath")
            remove("${documentId}_sourceType")
            remove("${documentId}_fileSize")
            remove("${documentId}_dateAdded")
            remove("${documentId}_summary")
            remove("${documentId}_flashcards")
            remove("${documentId}_notebookId")

            val ids = getDocumentIds().toMutableSet()
            ids.remove(documentId)
            putString(KEY_DOCUMENT_IDS, ids.joinToString(SEPARATOR))

            apply()
        }
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    // ─────────────────────────────────────────────────────────────
    //  Private helpers
    // ─────────────────────────────────────────────────────────────

    private fun getDocumentIds(): Set<String> {
        val raw = prefs.getString(KEY_DOCUMENT_IDS, null) ?: return emptySet()
        return raw.split(SEPARATOR).filter { it.isNotBlank() }.toSet()
    }

    private fun getNotebookIds(): Set<String> {
        val raw = prefs.getString(KEY_NOTEBOOK_IDS, null) ?: return emptySet()
        return raw.split(SEPARATOR).filter { it.isNotBlank() }.toSet()
    }
}
