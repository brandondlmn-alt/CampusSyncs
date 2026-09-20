package com.campussync.app.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.RecyclerView
import com.campussync.app.R
import com.campussync.app.databinding.ItemScannedAssessmentBinding
import com.campussync.app.models.ScannedAssessment

/**
 * Adapter for reviewing and editing assessments scanned via AI.
 * Implements two-way binding to the ScannedAssessment model.
 */
class ScannedAssessmentAdapter(
    private val entries: MutableList<ScannedAssessment>
) : RecyclerView.Adapter<ScannedAssessmentAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemScannedAssessmentBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemScannedAssessmentBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = entries[position]
        val b = holder.binding

        // Initial values
        b.etModuleCode.setText(item.moduleCode)
        b.etModuleName.setText(item.moduleName)
        b.etType.setText(item.assessmentType)
        b.actvSubmission.setText(item.submissionMethod, false)
        b.switchTurnitin.isChecked = item.requiresTurnitin
        b.etDate.setText(item.dueDate)
        b.etTime.setText(item.dueTime)

        // Setup Submission Method Dropdown
        val methods = arrayOf("Online Submission", "Campus Sitting")
        val adapter = ArrayAdapter(b.root.context, android.R.layout.simple_list_item_1, methods)
        b.actvSubmission.setAdapter(adapter)

        // Two-way Binding
        b.etModuleCode.doOnTextChanged { t, _, _, _ -> item.moduleCode = t.toString() }
        b.etModuleName.doOnTextChanged { t, _, _, _ -> item.moduleName = t.toString() }
        b.etType.doOnTextChanged { t, _, _, _ -> item.assessmentType = t.toString() }
        b.actvSubmission.doOnTextChanged { t, _, _, _ -> item.submissionMethod = t.toString() }
        b.switchTurnitin.setOnCheckedChangeListener { _, isChecked -> item.requiresTurnitin = isChecked }
        b.etDate.doOnTextChanged { t, _, _, _ -> item.dueDate = t.toString() }
        b.etTime.doOnTextChanged { t, _, _, _ -> item.dueTime = t.toString() }

        b.btnDelete.setOnClickListener {
            val currentPos = holder.adapterPosition
            if (currentPos != RecyclerView.NO_POSITION) {
                entries.removeAt(currentPos)
                notifyItemRemoved(currentPos)
            }
        }
    }

    override fun getItemCount() = entries.size

    fun getEntries(): List<ScannedAssessment> = entries
}
