package com.campussync.app.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.RecyclerView
import com.campussync.app.databinding.ItemScannedEntryBinding
import com.campussync.app.models.ScannedTimetableEntry

/**
 * Adapter for editing scanned timetable entries.
 * Updates the underlying data model in real-time as the user types.
 */
class ScannedEntryAdapter(
    private val entries: MutableList<ScannedTimetableEntry>
) : RecyclerView.Adapter<ScannedEntryAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemScannedEntryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemScannedEntryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = entries[position]
        val b = holder.binding

        // Set values without triggering listeners
        b.etModuleCode.setText(entry.moduleCode)
        b.etModuleName.setText(entry.moduleName)
        b.etDay.setText(entry.dayOfWeek)
        b.etStartTime.setText(entry.startTime)
        b.etEndTime.setText(entry.endTime)
        b.etVenue.setText(entry.venue)

        // Update model on text change
        b.etModuleCode.doOnTextChanged { text, _, _, _ -> entry.moduleCode = text.toString() }
        b.etModuleName.doOnTextChanged { text, _, _, _ -> entry.moduleName = text.toString() }
        b.etDay.doOnTextChanged { text, _, _, _ -> entry.dayOfWeek = text.toString() }
        b.etStartTime.doOnTextChanged { text, _, _, _ -> entry.startTime = text.toString() }
        b.etEndTime.doOnTextChanged { text, _, _, _ -> entry.endTime = text.toString() }
        b.etVenue.doOnTextChanged { text, _, _, _ -> entry.venue = text.toString() }

        b.btnDelete.setOnClickListener {
            removeEntry(holder.adapterPosition)
        }
    }

    override fun getItemCount() = entries.size

    fun getEntries(): List<ScannedTimetableEntry> = entries

    private fun removeEntry(position: Int) {
        if (position != RecyclerView.NO_POSITION) {
            entries.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, entries.size)
        }
    }
}
