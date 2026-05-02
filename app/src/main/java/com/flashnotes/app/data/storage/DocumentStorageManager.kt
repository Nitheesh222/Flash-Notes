package com.flashnotes.app.data.storage

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Manages document file storage using the device's Internal Storage.
 *
 * Storage structure under `getFilesDir()`:
 *   documents/          — Permanent storage for uploaded PDFs, images, etc.
 *   cache/processing/   — Temporary files used during LLM processing.
 *
 * Files saved here are private to the app and automatically deleted on uninstall.
 */
class DocumentStorageManager(private val context: Context) {

    companion object {
        private const val DOCUMENTS_DIR = "documents"
        private const val PROCESSING_CACHE_DIR = "processing"
    }

    // ─────────────────────────────────────────────────────────────
    //  Directory helpers
    // ─────────────────────────────────────────────────────────────

    /** Returns (and creates if needed) the root documents directory. */
    private fun getDocumentsDir(): File {
        val dir = File(context.filesDir, DOCUMENTS_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /** Returns (and creates if needed) the temporary processing cache directory. */
    private fun getProcessingCacheDir(): File {
        val dir = File(context.cacheDir, PROCESSING_CACHE_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    // ─────────────────────────────────────────────────────────────
    //  Save
    // ─────────────────────────────────────────────────────────────

    /**
     * Saves a document from a content Uri (from file picker) to internal storage.
     *
     * @param uri         The content:// URI returned by the file picker.
     * @param fileName    The desired file name (e.g. "lecture_notes.pdf").
     * @return            The absolute path of the saved file, or null on failure.
     */
    fun saveDocument(uri: Uri, fileName: String): String? {
        return try {
            val targetFile = File(getDocumentsDir(), sanitizeFileName(fileName))
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(targetFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            targetFile.absolutePath
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Saves raw bytes directly (e.g. from a camera capture or in-memory data).
     *
     * @param data        The raw byte array to save.
     * @param fileName    The desired file name.
     * @return            The absolute path of the saved file, or null on failure.
     */
    fun saveDocument(data: ByteArray, fileName: String): String? {
        return try {
            val targetFile = File(getDocumentsDir(), sanitizeFileName(fileName))
            FileOutputStream(targetFile).use { outputStream ->
                outputStream.write(data)
            }
            targetFile.absolutePath
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  Read
    // ─────────────────────────────────────────────────────────────

    /**
     * Reads the full content of a stored text-based document as a String.
     * Should only be used for .txt or small text files — NOT raw PDFs.
     */
    fun readTextDocument(filePath: String): String? {
        return try {
            File(filePath).readText()
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Returns the raw bytes of any stored document file.
     */
    fun readDocumentBytes(filePath: String): ByteArray? {
        return try {
            File(filePath).readBytes()
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Returns a File handle for a stored document (for passing to LLM, viewers, etc.).
     */
    fun getDocumentFile(filePath: String): File? {
        val file = File(filePath)
        return if (file.exists()) file else null
    }

    // ─────────────────────────────────────────────────────────────
    //  Delete
    // ─────────────────────────────────────────────────────────────

    /**
     * Deletes a document from internal storage.
     *
     * @return true if the file was successfully deleted.
     */
    fun deleteDocument(filePath: String): Boolean {
        return try {
            File(filePath).delete()
        } catch (e: SecurityException) {
            e.printStackTrace()
            false
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  Query
    // ─────────────────────────────────────────────────────────────

    /** Lists all document files currently stored. */
    fun listAllDocuments(): List<File> {
        return getDocumentsDir().listFiles()?.toList() ?: emptyList()
    }

    /** Returns the total size consumed by stored documents in bytes. */
    fun getTotalStorageUsed(): Long {
        return listAllDocuments().sumOf { it.length() }
    }

    /** Checks if a file exists at the given path. */
    fun documentExists(filePath: String): Boolean {
        return File(filePath).exists()
    }

    // ─────────────────────────────────────────────────────────────
    //  Cache helpers (for temporary processing files)
    // ─────────────────────────────────────────────────────────────

    /**
     * Saves a temporary copy to the processing cache (auto-cleaned by OS).
     * Use this for intermediate files needed only while the LLM is processing.
     */
    fun saveToCacheForProcessing(data: ByteArray, fileName: String): String? {
        return try {
            val cacheFile = File(getProcessingCacheDir(), sanitizeFileName(fileName))
            FileOutputStream(cacheFile).use { it.write(data) }
            cacheFile.absolutePath
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    /** Clears the entire processing cache. */
    fun clearProcessingCache() {
        getProcessingCacheDir().listFiles()?.forEach { it.delete() }
    }

    // ─────────────────────────────────────────────────────────────
    //  Utilities
    // ─────────────────────────────────────────────────────────────

    /**
     * Sanitizes a file name by replacing characters that are unsafe for the
     * file system (spaces → underscores, removes special chars).
     */
    private fun sanitizeFileName(name: String): String {
        return name
            .replace("[^a-zA-Z0-9._-]".toRegex(), "_")
            .replace("_{2,}".toRegex(), "_")
            .trimStart('_')
            .trimEnd('_')
            .ifEmpty { "untitled_document" }
    }
}
