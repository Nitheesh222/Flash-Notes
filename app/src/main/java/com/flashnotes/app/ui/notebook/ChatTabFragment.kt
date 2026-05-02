package com.flashnotes.app.ui.notebook

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.flashnotes.app.data.llm.LlmManager
import com.flashnotes.app.data.model.ChatMessage
import com.flashnotes.app.data.model.SummaryResult
import com.flashnotes.app.databinding.FragmentChatTabBinding
import kotlinx.coroutines.launch

/**
 * Chat tab that lets the user ask questions about their uploaded sources.
 * Uses [LlmManager] to route queries to the active LLM provider (currently placeholder).
 */
class ChatTabFragment : Fragment() {

    private var _binding: FragmentChatTabBinding? = null
    private val binding get() = _binding!!

    private lateinit var chatAdapter: ChatAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatTabBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupChatRecyclerView()
        setupInputListeners()
        showWelcomeMessage()
    }

    private fun setupChatRecyclerView() {
        chatAdapter = ChatAdapter()
        binding.rvChat.apply {
            layoutManager = LinearLayoutManager(requireContext()).apply {
                stackFromEnd = true
            }
            adapter = chatAdapter
        }
    }

    private fun setupInputListeners() {
        // Send button click
        binding.btnSend.setOnClickListener {
            sendMessage()
        }

        // Enter key on soft keyboard
        binding.etMessage.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage()
                true
            } else {
                false
            }
        }
    }

    private fun showWelcomeMessage() {
        val providerName = LlmManager.getProviderName()
        val welcome = ChatMessage(
            content = "👋 Hello! I'm your Notebook Guide.\n\n" +
                    "Ask me anything about your uploaded sources — I can summarize, " +
                    "explain concepts, or generate flashcards.\n\n" +
                    "LLM Provider: $providerName\n" +
                    "Status: ${if (LlmManager.isReady()) "✅ Ready" else "⚠️ Not configured"}",
            role = ChatMessage.Role.AI
        )
        chatAdapter.addMessage(welcome)
    }

    private fun sendMessage() {
        val text = binding.etMessage.text?.toString()?.trim() ?: return
        if (text.isEmpty()) return

        // Clear input
        binding.etMessage.text?.clear()

        // Add user message to chat
        val userMessage = ChatMessage(content = text, role = ChatMessage.Role.USER)
        chatAdapter.addMessage(userMessage)
        scrollToBottom()

        // Send to LLM and show response
        viewLifecycleOwner.lifecycleScope.launch {
            // Show typing indicator
            val typingMessage = ChatMessage(content = "Thinking...", role = ChatMessage.Role.AI)
            chatAdapter.addMessage(typingMessage)
            scrollToBottom()

            // Get LLM response
            val llm = LlmManager.getProvider()
            val result = llm.answerQuestion(
                question = text,
                contextText = "Sources from the current notebook (placeholder context)"
            )

            // Remove typing indicator (replace last message)
            val responseText = when (result) {
                is SummaryResult.Success -> result.text
                is SummaryResult.Error -> "❌ Error: ${result.message}"
                is SummaryResult.Loading -> "Processing..."
            }

            // Update the typing message with the actual response
            val responseMessage = ChatMessage(content = responseText, role = ChatMessage.Role.AI)
            chatAdapter.addMessage(responseMessage)
            scrollToBottom()
        }
    }

    private fun scrollToBottom() {
        binding.rvChat.post {
            val itemCount = chatAdapter.itemCount
            if (itemCount > 0) {
                binding.rvChat.smoothScrollToPosition(itemCount - 1)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
