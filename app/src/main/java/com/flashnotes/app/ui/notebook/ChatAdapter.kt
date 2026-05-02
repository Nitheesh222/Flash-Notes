package com.flashnotes.app.ui.notebook

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.flashnotes.app.R
import com.flashnotes.app.data.model.ChatMessage

/**
 * Adapter for rendering chat messages in the Chat tab.
 * Uses two different layouts for user vs AI messages.
 */
class ChatAdapter(
    private val messages: MutableList<ChatMessage> = mutableListOf()
) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    companion object {
        private const val VIEW_TYPE_USER = 0
        private const val VIEW_TYPE_AI = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (messages[position].role) {
            ChatMessage.Role.USER -> VIEW_TYPE_USER
            ChatMessage.Role.AI -> VIEW_TYPE_AI
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val layoutId = when (viewType) {
            VIEW_TYPE_USER -> R.layout.item_chat_bubble_user
            else -> R.layout.item_chat_bubble_ai
        }
        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(messages[position])
    }

    override fun getItemCount(): Int = messages.size

    fun addMessage(message: ChatMessage) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }

    fun getMessages(): List<ChatMessage> = messages.toList()

    class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(message: ChatMessage) {
            // Both layouts have a TextView with this ID for the message content
            val tvMessage = itemView.findViewById<TextView>(R.id.tv_message)
            tvMessage?.text = message.content
        }
    }
}
