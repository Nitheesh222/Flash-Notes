package com.flashnotes.app.ui.flashcard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.flashnotes.app.data.llm.LlmManager
import com.flashnotes.app.data.model.Flashcard
import com.flashnotes.app.data.model.SummaryResult
import com.flashnotes.app.data.storage.MetadataStore
import com.flashnotes.app.databinding.FragmentFlashcardPlayerBinding
import kotlinx.coroutines.launch
import java.io.File

class FlashcardPlayerFragment : Fragment() {

    private var _binding: FragmentFlashcardPlayerBinding? = null
    private val binding get() = _binding!!

    private var flashcards: List<Flashcard> = emptyList()
    private var currentIndex = 0
    private var isFrontShowing = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFlashcardPlayerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup camera distance for flip animation
        val scale = requireContext().resources.displayMetrics.density
        binding.cardFront.cameraDistance = 8000 * scale
        binding.cardBack.cameraDistance = 8000 * scale

        setupToolbar()
        setupListeners()
        loadFlashcards()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    /**
     * Loads flashcards from Gemini using the most recently uploaded document.
     * Falls back to a friendly error state if no document is available.
     */
    private fun loadFlashcards() {
        showLoading(true)

        viewLifecycleOwner.lifecycleScope.launch {
            val metadataStore = MetadataStore(requireContext())
            val notebookId = arguments?.getInt("notebookId", -1) ?: -1
            
            val docs = if (notebookId != -1) {
                metadataStore.getDocumentsForNotebook(notebookId)
            } else {
                metadataStore.getAllDocuments()
            }

            if (docs.isEmpty()) {
                showLoading(false)
                showError("No documents uploaded yet. Add a source first!")
                return@launch
            }

            // Use the most recently uploaded document
            val latestDoc = docs.last()

            // Check if flashcards are already saved
            val savedFlashcards = metadataStore.getFlashcards(latestDoc.id)
            if (!savedFlashcards.isNullOrEmpty()) {
                flashcards = savedFlashcards
                currentIndex = 0
                showLoading(false)
                updateCard()
                return@launch
            }

            val file = File(latestDoc.internalFilePath)

            if (!file.exists()) {
                showLoading(false)
                showError("Source file not found. Please re-upload the document.")
                return@launch
            }

            // Determine MIME type from file extension
            val mimeType = when {
                file.name.endsWith(".pdf", ignoreCase = true) -> "application/pdf"
                file.name.endsWith(".jpg", ignoreCase = true) ||
                file.name.endsWith(".jpeg", ignoreCase = true) -> "image/jpeg"
                file.name.endsWith(".png", ignoreCase = true) -> "image/png"
                file.name.endsWith(".webp", ignoreCase = true) -> "image/webp"
                else -> "text/plain"
            }

            val llm = LlmManager.getProvider()
            val result = llm.generateFlashcards(file = file, mimeType = mimeType, count = 5)

            showLoading(false)

            when (result) {
                is SummaryResult.Success -> {
                    flashcards = parseFlashcards(result.text)
                    if (flashcards.isEmpty()) {
                        showError("Could not parse flashcards. Please try again.")
                    } else {
                        metadataStore.saveFlashcards(latestDoc.id, flashcards)
                        currentIndex = 0
                        updateCard()
                    }
                }
                is SummaryResult.Error -> {
                    showError("AI Error: ${result.message}")
                }
                is SummaryResult.Loading -> { /* handled by showLoading */ }
            }
        }
    }

    /**
     * Parses the structured Q/A text returned by Gemini into [Flashcard] objects.
     *
     * Expected format:
     *   Q1: Question text
     *   A1: Answer text
     *   (blank line)
     *   Q2: ...
     */
    private fun parseFlashcards(text: String): List<Flashcard> {
        val cards = mutableListOf<Flashcard>()
        val lines = text.lines()

        var question: String? = null
        var id = 1

        for (line in lines) {
            val trimmed = line.trim()
            when {
                trimmed.matches(Regex("^Q\\d+:.*")) -> {
                    question = trimmed.substringAfter(":").trim()
                }
                trimmed.matches(Regex("^A\\d+:.*")) && question != null -> {
                    val answer = trimmed.substringAfter(":").trim()
                    if (question!!.isNotBlank() && answer.isNotBlank()) {
                        cards.add(Flashcard(id = id++, question = question!!, answer = answer))
                    }
                    question = null
                }
            }
        }
        return cards
    }

    private fun setupListeners() {
        // Card flip
        binding.cardFront.setOnClickListener { flipCard() }
        binding.cardBack.setOnClickListener { flipCard() }

        // Next
        binding.btnNext.setOnClickListener {
            if (flashcards.isNotEmpty() && currentIndex < flashcards.size - 1) {
                currentIndex++
                resetFlip()
                updateCard()
            }
        }

        // Previous
        binding.btnPrev.setOnClickListener {
            if (currentIndex > 0) {
                currentIndex--
                resetFlip()
                updateCard()
            }
        }

    }

    private fun updateCard() {
        if (flashcards.isEmpty()) return
        val total = flashcards.size
        val currentCard = flashcards[currentIndex]
        val counterText = "${currentIndex + 1} / $total"

        binding.tvCounterFront.text = counterText
        binding.tvCounterBack.text = counterText

        binding.tvQuestion.text = currentCard.question
        binding.tvAnswer.text = currentCard.answer
    }

    /** Resets the card to front-facing (question side) when navigating. */
    private fun resetFlip() {
        if (!isFrontShowing) {
            binding.cardFront.alpha = 1f
            binding.cardBack.alpha = 0f
            binding.cardFront.rotationY = 0f
            binding.cardBack.rotationY = -180f
            binding.cardBack.visibility = View.INVISIBLE
            isFrontShowing = true
        }
    }

    private fun flipCard() {
        if (isFrontShowing) {
            binding.cardBack.visibility = View.VISIBLE
            binding.cardFront.animate().rotationY(180f).alpha(0f).setDuration(300).start()
            binding.cardBack.animate().rotationY(0f).alpha(1f).setDuration(300).start()
        } else {
            binding.cardFront.animate().rotationY(0f).alpha(1f).setDuration(300).start()
            binding.cardBack.animate().rotationY(-180f).alpha(0f).setDuration(300).withEndAction {
                binding.cardBack.visibility = View.INVISIBLE
            }.start()
        }
        isFrontShowing = !isFrontShowing
    }

    private fun showLoading(show: Boolean) {
        if (_binding == null) return
        binding.loadingContainer.visibility = if (show) View.VISIBLE else View.GONE
        binding.cardFront.visibility = if (show) View.INVISIBLE else View.VISIBLE
        binding.btnNext.isEnabled = !show
        binding.btnPrev.isEnabled = !show
    }

    private fun showError(message: String) {
        if (_binding == null) return
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        binding.tvQuestion.text = message
        binding.tvAnswer.text = ""
        binding.tvCounterFront.text = "0 / 0"
        binding.tvCounterBack.text = "0 / 0"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
