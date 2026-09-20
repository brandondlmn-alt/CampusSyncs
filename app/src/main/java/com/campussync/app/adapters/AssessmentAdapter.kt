package com.campussync.app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.campussync.app.R
import com.campussync.app.databinding.ItemAssessmentBinding
import com.campussync.app.models.Assessment
import com.campussync.app.utils.DateUtils
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter for the main Assessments list.
 */
class AssessmentAdapter(
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
        b.tvMethod.text = item.submissionMethod
        b.badgeTurnitin.visibility = if (item.requiresTurnitin) View.VISIBLE else View.GONE

        // Dynamic urgency color
        val colorRes = getUrgencyColor(item.dueDate)
        b.viewUrgency.setBackgroundColor(ContextCompat.getColor(b.root.context, colorRes))

        b.root.setOnClickListener { onClick(item) }
    }

    private fun getUrgencyColor(isoDate: String): Int {
        if (isoDate == "N/A") return android.R.color.darker_gray
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val targetDate = sdf.parse(isoDate) ?: return android.R.color.darker_gray
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time

            val diffDays = (targetDate.time - today.time) / (1000 * 60 * 60 * 24)

            when {
                diffDays < 0 -> android.R.color.holo_red_dark // Overdue
                diffDays < 3 -> android.R.color.holo_red_dark // Red: < 3 days
                diffDays < 7 -> android.R.color.holo_orange_dark // Orange: 3-7 days
                else -> android.R.color.holo_green_dark // Green: > 7 days
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
