package com.campussync.app.adapters

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.campussync.app.databinding.ItemAssessmentBinding
import com.campussync.app.models.Assessment
import com.campussync.app.utils.DateUtils
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter for the main Assessments list.
 * Handles completion toggles and notification scheduling.
 */
class AssessmentAdapter(
    private val onCompleteToggle: (Assessment, Boolean) -> Unit,
    private val onNotifyClick: (Assessment) -> Unit,
    private val onClick: (Assessment) -> Unit
) : ListAdapter<Assessment, AssessmentAdapter.ViewHolder>(DiffCallback()) {

    class ViewHolder(val binding: ItemAssessmentBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAssessmentBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        val b = holder.binding

        b.tvModuleCode.text = item.moduleCode
        b.tvType.text = item.assessmentType
        b.tvDisplayDate.text = DateUtils.formatForDisplay(item.dueDate, item.dueTime)
        b.tvCountdown.text = DateUtils.getCountdown(item.dueDate)

        // Set completion status without triggering listener
        b.cbComplete.setOnCheckedChangeListener(null)
        b.cbComplete.isChecked = item.completed
        
        // Strike through text and dim card if completed
        if (item.completed) {
            b.tvModuleCode.paintFlags = b.tvModuleCode.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            b.tvType.paintFlags = b.tvType.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            b.viewUrgency.setBackgroundColor(ContextCompat.getColor(b.root.context, android.R.color.darker_gray))
            b.cardAssessment.alpha = 0.6f
        } else {
            b.tvModuleCode.paintFlags = b.tvModuleCode.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            b.tvType.paintFlags = b.tvType.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            val colorRes = getUrgencyColor(item.dueDate)
            b.viewUrgency.setBackgroundColor(ContextCompat.getColor(b.root.context, colorRes))
            b.cardAssessment.alpha = 1.0f
        }

        b.cbComplete.setOnCheckedChangeListener { _, isChecked ->
            onCompleteToggle(item, isChecked)
        }

        b.btnNotification.setOnClickListener { onNotifyClick(item) }
        b.root.setOnClickListener { onClick(item) }
    }

    private fun getUrgencyColor(isoDate: String): Int {
        if (isoDate == "N/A") return android.R.color.darker_gray
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val targetDate = sdf.parse(isoDate) ?: return android.R.color.darker_gray
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.time

            val diffDays = (targetDate.time - today.time) / (1000 * 60 * 60 * 24)

            when {
                diffDays < 0 -> android.R.color.holo_red_dark
                diffDays < 3 -> android.R.color.holo_red_dark
                diffDays < 7 -> android.R.color.holo_orange_dark
                else -> android.R.color.holo_green_dark
            }
        } catch (e: Exception) {
            android.R.color.darker_gray
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Assessment>() {
        override fun areItemsTheSame(oldItem: Assessment, newItem: Assessment) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Assessment, newItem: Assessment) = oldItem == newItem
    }
}
