package com.flashnotes.app.ui.addsource

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.flashnotes.app.data.model.DocumentMetadata
import com.flashnotes.app.data.model.SourceType
import com.flashnotes.app.data.storage.DocumentStorageManager
import com.flashnotes.app.data.storage.MetadataStore
import com.flashnotes.app.databinding.BottomSheetAddSourceBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Bottom sheet for adding document sources (PDF, Image, Text).
 *
 * When the user selects a file:
 *  1. File is saved to internal storage via [DocumentStorageManager]
 *  2. A [DocumentMetadata] entry is created in [MetadataStore]
 *  3. A new Notebook is auto-created and linked to the document
 *
 * The home screen refreshes on resume and picks up the new notebook.
 */
class AddSourceBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetAddSourceBinding? = null
    private val binding get() = _binding!!

    private lateinit var storageManager: DocumentStorageManager
    private lateinit var metadataStore: MetadataStore

    // File picker launcher
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                handleSelectedFile(uri)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetAddSourceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        storageManager = DocumentStorageManager(requireContext())
        metadataStore = MetadataStore(requireContext())

        binding.btnAddSource.setOnClickListener {
            openFilePicker()
        }
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(
                Intent.EXTRA_MIME_TYPES,
                arrayOf(
                    "application/pdf",
                    "image/jpeg",
                    "image/png",
                    "image/webp",
                    "text/plain"
                )
            )
        }
        filePickerLauncher.launch(intent)
    }

    /**
     * Full pipeline after a file is selected:
     *   1. Resolve file name & MIME type
     *   2. Save file to internal storage
     *   3. Persist DocumentMetadata
     *   4. Auto-create a Notebook for this source
     *   5. Link the document to the notebook
     */
    private fun handleSelectedFile(uri: android.net.Uri) {
        val context = requireContext()

        val fileName = getFileNameFromUri(uri) ?: "Document_${System.currentTimeMillis()}"
        val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
        val sourceType = mimeTypeToSourceType(mimeType)

        // 1. Save file to disk
        val savedPath = storageManager.saveDocument(uri, fileName) ?: run {
            Toast.makeText(context, "❌ Failed to save file", Toast.LENGTH_SHORT).show()
            return
        }

        // 2. Build document metadata
        val documentId = UUID.randomUUID().toString()
        // Check if we are adding to an existing notebook
        val existingNotebookId = arguments?.getInt("notebookId", -1) ?: -1
        val isNewNotebook = existingNotebookId == -1
        val notebookId = if (isNewNotebook) metadataStore.getNextNotebookId() else existingNotebookId

        val metadata = DocumentMetadata(
            id = documentId,
            originalFileName = fileName,
            internalFilePath = savedPath,
            sourceType = sourceType,
            fileSizeBytes = java.io.File(savedPath).length(),
            notebookId = notebookId
        )
        metadataStore.saveDocumentMetadata(metadata)

        if (isNewNotebook) {
            // 3. Auto-create a dedicated Notebook for this source
            val notebookTitle = fileNameToNotebookTitle(fileName)
            val today = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date())
            val iconType = when (sourceType) {
                SourceType.PDF   -> "pdf"
                SourceType.IMAGE -> "image"
                else             -> "text"
            }
            metadataStore.saveNotebook(
                id = notebookId,
                title = notebookTitle,
                sourceCount = 1,
                date = today,
                iconType = iconType
            )
            Toast.makeText(context, "✅ Notebook created: $notebookTitle", Toast.LENGTH_SHORT).show()
        } else {
            // 3. Update existing Notebook source count
            val notebooks = metadataStore.getAllNotebooks()
            val existing = notebooks.find { it["id"] == notebookId }
            if (existing != null) {
                val currentCount = existing["sourceCount"] as? Int ?: 0
                metadataStore.updateNotebookSourceCount(notebookId, currentCount + 1)
            }
            Toast.makeText(context, "✅ Source added to notebook", Toast.LENGTH_SHORT).show()
        }

        dismiss()
    }

    /**
     * Converts a raw file name into a human-readable notebook title.
     * Example: "BCA_CA2-Updated.pdf"  →  "BCA CA2 Updated"
     */
    private fun fileNameToNotebookTitle(fileName: String): String {
        // Strip extension
        val nameWithoutExt = fileName.substringBeforeLast(".")
        // Replace underscores and dashes with spaces, then trim
        return nameWithoutExt.replace(Regex("[_\\-]+"), " ").trim()
    }

    private fun getFileNameFromUri(uri: android.net.Uri): String? {
        var name: String? = null
        requireContext().contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex >= 0) {
                name = cursor.getString(nameIndex)
            }
        }
        return name
    }

    private fun mimeTypeToSourceType(mimeType: String): SourceType {
        return when {
            mimeType.startsWith("application/pdf") -> SourceType.PDF
            mimeType.startsWith("image/")          -> SourceType.IMAGE
            mimeType.startsWith("audio/")          -> SourceType.AUDIO
            mimeType.startsWith("text/")           -> SourceType.TEXT
            else                                   -> SourceType.TEXT
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
