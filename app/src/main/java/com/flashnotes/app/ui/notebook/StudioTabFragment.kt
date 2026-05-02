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
import com.flashnotes.app.databinding.FragmentStudioTabBinding

class StudioTabFragment : Fragment() {

    private var _binding: FragmentStudioTabBinding? = null
    private val binding get() = _binding!!

    private var notebookId: Int = -1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStudioTabBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        notebookId = arguments?.getInt("notebookId", -1) ?: -1

        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        binding.rvStudioOptions.layoutManager = LinearLayoutManager(requireContext())
        binding.rvStudioOptions.adapter = StudioOptionAdapter(SampleData.studioOptions) { option ->
            // If Flashcards is clicked, navigate to Flashcard player
            if (option.titleRes == R.string.flashcards) {
                // Navigate up to activity level NavController
                requireActivity().supportFragmentManager.findFragmentById(R.id.nav_host_fragment)?.let {
                    val navController = androidx.navigation.fragment.NavHostFragment.findNavController(it)
                    val bundle = Bundle().apply { putInt("notebookId", notebookId) }
                    navController.navigate(R.id.action_notebookDetailFragment_to_flashcardPlayerFragment, bundle)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
