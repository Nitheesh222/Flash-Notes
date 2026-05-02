package com.flashnotes.app.ui.notebook

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.flashnotes.app.data.model.Source
import com.flashnotes.app.databinding.ItemSourceBinding

class SourceAdapter(
    private val sources: List<Source>
) : RecyclerView.Adapter<SourceAdapter.SourceViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SourceViewHolder {
        val binding = ItemSourceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SourceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SourceViewHolder, position: Int) {
        holder.bind(sources[position])
    }

    override fun getItemCount() = sources.size

    class SourceViewHolder(
        private val binding: ItemSourceBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(source: Source) {
            binding.tvSourceName.text = source.name
        }
    }
}
