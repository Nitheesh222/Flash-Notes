package com.flashnotes.app.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.flashnotes.app.R
import com.flashnotes.app.data.model.Notebook
import com.flashnotes.app.databinding.ItemNotebookBinding

class NotebookAdapter(
    private var notebooks: List<Notebook>,
    private val onClick: (Notebook) -> Unit,
    private val onLongClick: (Notebook) -> Unit = {}
) : RecyclerView.Adapter<NotebookAdapter.NotebookViewHolder>() {

    fun updateData(newNotebooks: List<Notebook>) {
        notebooks = newNotebooks
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotebookViewHolder {
        val binding = ItemNotebookBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NotebookViewHolder(binding, onClick, onLongClick)
    }

    override fun onBindViewHolder(holder: NotebookViewHolder, position: Int) {
        holder.bind(notebooks[position])
    }

    override fun getItemCount() = notebooks.size

    class NotebookViewHolder(
        private val binding: ItemNotebookBinding,
        private val onClick: (Notebook) -> Unit,
        private val onLongClick: (Notebook) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(notebook: Notebook) {
            val context = binding.root.context
            
            // Set data
            binding.tvTitle.text = notebook.title
            
            val sourceText = if (notebook.sourceCount == 1) {
                context.getString(R.string.sources_format, notebook.sourceCount)
            } else {
                context.getString(R.string.sources_format_plural, notebook.sourceCount)
            }
            binding.tvSubtitle.text = "$sourceText • ${notebook.date}"
            binding.tvSourcesBadge.text = sourceText.uppercase()
            
            binding.ivIcon.setImageResource(notebook.iconRes)

            // Click listener
            binding.root.setOnClickListener { onClick(notebook) }
            binding.root.setOnLongClickListener { 
                onLongClick(notebook)
                true 
            }
        }
    }
}
