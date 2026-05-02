package com.flashnotes.app.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.flashnotes.app.R
import com.flashnotes.app.data.model.Notebook
import com.flashnotes.app.data.storage.MetadataStore
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AlertDialog
import com.flashnotes.app.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private var allNotebooksList = mutableListOf<Notebook>()
    private lateinit var adapter: NotebookAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        adapter = NotebookAdapter(
            notebooks = allNotebooksList,
            onClick = { notebook ->
                val bundle = Bundle().apply {
                    putInt("notebookId", notebook.id)
                    putString("notebookTitle", notebook.title)
                }
                findNavController().navigate(R.id.action_homeFragment_to_notebookDetailFragment, bundle)
            },
            onLongClick = { notebook ->
                showDeleteConfirmation(notebook)
            }
        )
        binding.rvNotebooks.layoutManager = LinearLayoutManager(requireContext())
        binding.rvNotebooks.adapter = adapter

        setupClickListeners()
        setupSearch()
    }

    /**
     * Refresh the notebook list every time the fragment becomes visible.
     * This ensures newly added notebooks (from AddSourceBottomSheet) appear
     * immediately when the user returns to the home screen.
     */
    override fun onResume() {
        super.onResume()
        refreshNotebookList()
    }

    /**
     * Builds the notebook list by merging real persisted notebooks from
     * [MetadataStore] with the static sample data — sample entries come last
     * so real notebooks appear at the top.
     */
    private fun refreshNotebookList() {
        val metadataStore = MetadataStore(requireContext())
        val allNotebooks = mutableListOf<Notebook>()

        // Load real notebooks from persistent storage
        val stored = metadataStore.getAllNotebooks()
        stored.forEach { map ->
            val id         = map["id"] as Int
            val title      = map["title"] as String
            val srcCount   = map["sourceCount"] as Int
            val date       = map["date"] as String
            val iconType   = map["iconType"] as String

            val iconRes = when (iconType) {
                "image" -> R.drawable.ic_image
                "text"  -> R.drawable.ic_quiz
                else    -> R.drawable.ic_infographic   // pdf / default
            }

            allNotebooks.add(Notebook(id, title, srcCount, date, iconRes))
        }

        // Show empty-state hint if no notebooks yet
        if (allNotebooks.isEmpty()) {
            binding.tvEmptyState.visibility = View.VISIBLE
        } else {
            binding.tvEmptyState.visibility = View.GONE
        }

        allNotebooksList.clear()
        allNotebooksList.addAll(allNotebooks)

        // If search is currently active, apply the filter
        val query = binding.etSearch?.text?.toString() ?: ""
        if (query.isNotEmpty() && binding.etSearch?.visibility == View.VISIBLE) {
            filterNotebooks(query)
        } else {
            adapter.updateData(allNotebooksList)
        }
    }

    private fun setupClickListeners() {
        binding.btnCreateNew.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_addSourceBottomSheet)
        }

        binding.btnCamera.setOnClickListener {
            // Camera action placeholder
        }
    }

    private fun setupSearch() {
        binding.btnSearch.setOnClickListener {
            // Toggle search bar
            if (binding.etSearch.visibility == View.VISIBLE) {
                binding.etSearch.visibility = View.GONE
                binding.tvTitle.visibility = View.VISIBLE
                binding.etSearch.setText("")
                adapter.updateData(allNotebooksList)
            } else {
                binding.etSearch.visibility = View.VISIBLE
                binding.tvTitle.visibility = View.GONE
                binding.etSearch.requestFocus()
            }
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterNotebooks(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun filterNotebooks(query: String) {
        if (query.isEmpty()) {
            adapter.updateData(allNotebooksList)
            return
        }
        val lowerQuery = query.lowercase()
        val filtered = allNotebooksList.filter { 
            it.title.lowercase().contains(lowerQuery)
        }
        adapter.updateData(filtered)
    }

    private fun showDeleteConfirmation(notebook: Notebook) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Notebook")
            .setMessage("Are you sure you want to delete '${notebook.title}'? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                val metadataStore = MetadataStore(requireContext())
                metadataStore.deleteNotebook(notebook.id)
                refreshNotebookList()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
