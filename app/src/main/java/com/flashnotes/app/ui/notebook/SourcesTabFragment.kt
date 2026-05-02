package com.flashnotes.app.ui.notebook

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.flashnotes.app.R
import com.flashnotes.app.data.SampleData
import com.flashnotes.app.data.model.Source
import com.flashnotes.app.data.storage.MetadataStore
import com.flashnotes.app.databinding.FragmentSourcesTabBinding

/**
 * Sources tab showing all uploaded document sources.
 * Merges sample data with real documents stored in internal storage.
 */
class SourcesTabFragment : Fragment() {

    private var _binding: FragmentSourcesTabBinding? = null
    private val binding get() = _binding!!

    private var notebookId: Int = -1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSourcesTabBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.let {
            notebookId = it.getInt("notebookId", -1)
        }

        refreshSources()

        binding.btnAddSource.setOnClickListener {
            val bundle = Bundle().apply { putInt("notebookId", notebookId) }
            findNavController()
                .navigate(R.id.action_notebookDetailFragment_to_addSourceBottomSheet, bundle)
        }
    }

    override fun onResume() {
        super.onResume()
        refreshSources()
    }

    private fun refreshSources() {
        val allSources = buildSourceList()
        binding.rvSources.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSources.adapter = SourceAdapter(allSources)
    }

    /**
     * Loads documents belonging ONLY to this notebook.
     */
    private fun buildSourceList(): List<Source> {
        val sources = mutableListOf<Source>()

        // Add real documents from internal storage for this specific notebook
        val metadataStore = MetadataStore(requireContext())
        val storedDocs = if (notebookId != -1) {
            metadataStore.getDocumentsForNotebook(notebookId)
        } else {
            emptyList()
        }

        storedDocs.forEachIndexed { index, doc ->
            sources.add(
                Source(
                    id = 1000 + index,
                    name = doc.originalFileName,
                    type = doc.sourceType,
                    isLoading = false,
                    filePath = doc.internalFilePath,
                    documentId = doc.id
                )
            )
        }

        return sources
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
