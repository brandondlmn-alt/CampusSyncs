package com.campussync.app.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.campussync.app.databinding.ItemMarkBinding
import com.campussync.app.databinding.ItemMarkHeaderBinding
import com.campussync.app.models.MarkListItem
import java.util.Locale

/**
 * Adapter for the Marks RecyclerView that supports Group Headers and Mark Items.
 */
class MarkAdapter(
    private val onEditClick: (com.campussync.app.models.MarkEntry) -> Unit,
    private val onDeleteClick: (com.campussync.app.models.MarkEntry) -> Unit
) : ListAdapter<MarkListItem, RecyclerView.ViewHolder>(MarkDiffCallback()) {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is MarkListItem.Header -> TYPE_HEADER
            is MarkListItem.Item -> TYPE_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            val binding = ItemMarkHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            HeaderViewHolder(binding)
        } else {
            val binding = ItemMarkBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            ItemViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        if (holder is HeaderViewHolder && item is MarkListItem.Header) {
            holder.bind(item)
        } else if (holder is ItemViewHolder && item is MarkListItem.Item) {
            holder.bind(item.markEntry)
        }
    }

    inner class HeaderViewHolder(private val binding: ItemMarkHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(header: MarkListItem.Header) {
            binding.tvModuleCode.text = header.moduleCode
            binding.tvModuleName.text = header.moduleName
            binding.tvModuleAverage.text = String.format(Locale.getDefault(), "%.1f%%", header.average)
        }
    }

    inner class ItemViewHolder(private val binding: ItemMarkBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(mark: com.campussync.app.models.MarkEntry) {
            binding.tvModuleCode.visibility = android.view.View.GONE // Hide code in sub-items as it's in the header
            binding.tvAssessmentType.text = mark.assessmentType
            binding.tvWeight.text = "Weight: ${mark.weight}%"
            binding.tvMarkValue.text = "${mark.mark}%"

            binding.root.setOnClickListener { onEditClick(mark) }
            binding.root.setOnLongClickListener {
                onDeleteClick(mark)
                true
            }
        }
    }

    class MarkDiffCallback : DiffUtil.ItemCallback<MarkListItem>() {
        override fun areItemsTheSame(oldItem: MarkListItem, newItem: MarkListItem): Boolean {
            return if (oldItem is MarkListItem.Header && newItem is MarkListItem.Header) {
                oldItem.moduleCode == newItem.moduleCode
            } else if (oldItem is MarkListItem.Item && newItem is MarkListItem.Item) {
                oldItem.markEntry.id == newItem.markEntry.id
            } else false
        }

        override fun areContentsTheSame(oldItem: MarkListItem, newItem: MarkListItem): Boolean {
            return oldItem == newItem
        }
    }
}
