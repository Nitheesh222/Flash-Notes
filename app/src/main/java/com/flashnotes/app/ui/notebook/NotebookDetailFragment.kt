package com.flashnotes.app.ui.notebook

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.flashnotes.app.R
import com.flashnotes.app.databinding.FragmentNotebookDetailBinding

class NotebookDetailFragment : Fragment() {

    private var _binding: FragmentNotebookDetailBinding? = null
    private val binding get() = _binding!!

    private var notebookId: Int = -1
    private var notebookTitle: String = "Untitled Notebook"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotebookDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.let {
            notebookId = it.getInt("notebookId", -1)
            notebookTitle = it.getString("notebookTitle", "Untitled Notebook")
        }

        setupToolbar()
        setupBottomNavigation()

        // Do not implicitly add SourcesTabFragment if a fragment is already loaded.
        if (savedInstanceState == null && childFragmentManager.findFragmentById(R.id.tab_content_container) == null) {
            binding.bottomNavigation.selectedItemId = R.id.tab_sources
        }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)

        // Sync toolbar title with restored bottom navigation selection
        when (binding.bottomNavigation.selectedItemId) {
            R.id.tab_sources -> binding.toolbar.title = notebookTitle
            R.id.tab_chat   -> binding.toolbar.title = "Chat"
            R.id.tab_studio -> binding.toolbar.title = "Studio"
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupBottomNavigation() {
        // Set dynamic tint colors for Bottom Nav states
        val colors = intArrayOf(
            ContextCompat.getColor(requireContext(), R.color.colorBottomNavSelected), 
            ContextCompat.getColor(requireContext(), R.color.colorBottomNavUnselected)
        )
        val states = arrayOf(
            intArrayOf(android.R.attr.state_checked),
            intArrayOf(-android.R.attr.state_checked)
        )
        val colorStateList = ColorStateList(states, colors)

        binding.bottomNavigation.itemIconTintList = colorStateList
        binding.bottomNavigation.itemTextColor = colorStateList

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.tab_sources -> {
                    binding.toolbar.title = notebookTitle
                    loadFragment(SourcesTabFragment().apply {
                        arguments = Bundle().apply { putInt("notebookId", notebookId) }
                    })
                    true
                }
                R.id.tab_chat -> {
                    binding.toolbar.title = "Chat"
                    loadFragment(ChatTabFragment().apply {
                        arguments = Bundle().apply { putInt("notebookId", notebookId) }
                    })
                    true
                }
                R.id.tab_studio -> {
                    binding.toolbar.title = "Studio"
                    loadFragment(StudioTabFragment().apply {
                        arguments = Bundle().apply { putInt("notebookId", notebookId) }
                    })
                    true
                }
                else -> false
            }
        }
    }



    private fun loadFragment(fragment: Fragment) {
        childFragmentManager.beginTransaction()
            .replace(R.id.tab_content_container, fragment)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
