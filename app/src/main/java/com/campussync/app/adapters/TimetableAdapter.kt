package com.campussync.app.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.campussync.app.databinding.ItemTimetableEntryBinding
import com.campussync.app.models.TimetableEntry

/**
 * Adapter for the Timetable RecyclerView.
 * Uses ListAdapter for efficient list updates and DiffUtil for performance.
 */
class TimetableAdapter(
    private val onEditClick: (TimetableEntry) -> Unit,
    private val onDeleteClick: (TimetableEntry) -> Unit
) : ListAdapter<TimetableEntry, TimetableAdapter.TimetableViewHolder>(TimetableDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimetableViewHolder {
        val binding = ItemTimetableEntryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TimetableViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TimetableViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TimetableViewHolder(private val binding: ItemTimetableEntryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(entry: TimetableEntry) {
            binding.tvModuleCode.text = entry.moduleCode
            binding.tvModuleName.text = entry.moduleName
            binding.tvDayAndTime.text = "${entry.dayOfWeek}, ${entry.startTime} - ${entry.endTime}"
            binding.tvVenue.text = entry.venue

            binding.root.setOnClickListener { onEditClick(entry) }
            binding.root.setOnLongClickListener {
                onDeleteClick(entry)
                true
            }
        }
    }

    class TimetableDiffCallback : DiffUtil.ItemCallback<TimetableEntry>() {
        override fun areItemsTheSame(oldItem: TimetableEntry, newItem: TimetableEntry): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: TimetableEntry, newItem: TimetableEntry): Boolean {
            return oldItem == newItem
        }
    }
}
