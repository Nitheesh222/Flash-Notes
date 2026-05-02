package com.flashnotes.app.ui.notebook

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.flashnotes.app.data.model.StudioOption
import com.flashnotes.app.databinding.ItemStudioOptionBinding

class StudioOptionAdapter(
    private val options: List<StudioOption>,
    private val onClick: (StudioOption) -> Unit
) : RecyclerView.Adapter<StudioOptionAdapter.StudioOptionViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudioOptionViewHolder {
        val binding = ItemStudioOptionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return StudioOptionViewHolder(binding, onClick)
    }

    override fun onBindViewHolder(holder: StudioOptionViewHolder, position: Int) {
        holder.bind(options[position])
    }

    override fun getItemCount() = options.size

    class StudioOptionViewHolder(
        private val binding: ItemStudioOptionBinding,
        private val onClick: (StudioOption) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(option: StudioOption) {
            val context = binding.root.context
            
            binding.tvTitle.setText(option.titleRes)
            binding.ivIcon.setImageResource(option.iconRes)
            
            // Set dynamic background color based on the design
            val bgColor = ContextCompat.getColor(context, option.bgColorRes)
            binding.rootLayout.backgroundTintList = ColorStateList.valueOf(bgColor)

            binding.root.setOnClickListener { onClick(option) }
        }
    }
}
