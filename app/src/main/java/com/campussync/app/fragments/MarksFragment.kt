package com.campussync.app.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.campussync.app.R
import com.campussync.app.adapters.MarkAdapter
import com.campussync.app.data.MarkRepository
import com.campussync.app.data.ModuleRepository
import com.campussync.app.databinding.DialogAddMarkBinding
import com.campussync.app.databinding.FragmentMarksBinding
import com.campussync.app.models.MarkEntry
import com.campussync.app.models.MarkListItem
import com.campussync.app.models.Module
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Fragment for tracking academic marks.
 * Groups assessments by Module and displays both individual and module-level weighted averages.
 */
class MarksFragment : Fragment() {

    private var _binding: FragmentMarksBinding? = null
    private val binding get() = _binding!!

    private val repository = MarkRepository()
    private val moduleRepository = ModuleRepository()
    private lateinit var adapter: MarkAdapter
    private val TAG = "MarksFragment"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMarksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeData()

        binding.fabAddMark.setOnClickListener {
            checkModulesAndShowDialog(null)
        }
    }

    private fun setupRecyclerView() {
        adapter = MarkAdapter(
            onEditClick = { mark -> checkModulesAndShowDialog(mark) },
            onDeleteClick = { mark -> confirmDeletion(mark) }
        )
        binding.rvMarks.adapter = adapter
    }

    private fun observeData() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            combine(
                repository.getMarkEntries(),
                moduleRepository.getModules()
            ) { marks, modules ->
                Pair(marks, modules)
            }.collect { (marks, modules) ->
                binding.progressBar.visibility = View.GONE
                
                // 1. Calculate and display Overall Weighted Average
                val overallAverage = repository.calculateWeightedAverage(marks)
                binding.tvAverageValue.text = String.format(Locale.getDefault(), "%.1f%%", overallAverage)

                // 2. Build the grouped list (Headers + Items)
                val listItems = mutableListOf<MarkListItem>()
                val groupedMarks = marks.groupBy { it.moduleCode }
                
                // Sort modules by code for consistent display
                val sortedModules = modules.sortedBy { it.code }
                
                for (module in sortedModules) {
                    val moduleMarks = groupedMarks[module.code] ?: emptyList()
                    if (moduleMarks.isNotEmpty()) {
                        // Calculate average for this specific module
                        val moduleAverage = repository.calculateWeightedAverage(moduleMarks)
                        
                        // Add Header for the Module
                        listItems.add(MarkListItem.Header(module.code, module.name, moduleAverage))
                        
                        // Add all Mark Items belonging to this Module
                        moduleMarks.forEach { listItems.add(MarkListItem.Item(it)) }
                    }
                }
                
                // Handle cases where a mark exists for a code not in the current module list
                val orphanMarks = groupedMarks.filter { entry -> modules.none { it.code == entry.key } }
                for ((code, mMarks) in orphanMarks) {
                    val avg = repository.calculateWeightedAverage(mMarks)
                    listItems.add(MarkListItem.Header(code, "Unknown Module", avg))
                    mMarks.forEach { listItems.add(MarkListItem.Item(it)) }
                }

                adapter.submitList(listItems)
                binding.tvEmptyState.visibility = if (listItems.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun checkModulesAndShowDialog(mark: MarkEntry?) {
        lifecycleScope.launch {
            val modules = moduleRepository.getModules().first()
            if (modules.isEmpty()) {
                Toast.makeText(requireContext(), "Please add modules in Settings first", Toast.LENGTH_LONG).show()
            } else {
                showAddEditDialog(mark, modules)
            }
        }
    }

    private fun showAddEditDialog(mark: MarkEntry?, modules: List<Module>) {
        val dialogBinding = DialogAddMarkBinding.inflate(layoutInflater)
        val isEdit = mark != null

        val moduleCodes = modules.map { it.code }.toTypedArray()
        val moduleAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, moduleCodes)
        dialogBinding.actvModuleCode.setAdapter(moduleAdapter)

        val types = arrayOf("Test", "Assignment", "Exam", "Quiz", "Practical")
        val typeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, types)
        dialogBinding.actvAssessmentType.setAdapter(typeAdapter)

        if (isEdit) {
            dialogBinding.tvDialogTitle.text = "Edit Mark"
            dialogBinding.actvModuleCode.setText(mark?.moduleCode, false)
            dialogBinding.actvAssessmentType.setText(mark?.assessmentType, false)
            dialogBinding.etMark.setText(mark?.mark.toString())
            dialogBinding.etWeight.setText(mark?.weight.toString())
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .setPositiveButton(if (isEdit) "Update" else "Add", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            val button = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            button.setOnClickListener {
                val code = dialogBinding.actvModuleCode.text.toString()
                val type = dialogBinding.actvAssessmentType.text.toString()
                val markVal = dialogBinding.etMark.text.toString().toDoubleOrNull()
                val weightVal = dialogBinding.etWeight.text.toString().toDoubleOrNull()

                if (code.isEmpty() || type.isEmpty() || markVal == null || weightVal == null) {
                    Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val newMark = (mark ?: MarkEntry()).copy(
                    moduleCode = code,
                    assessmentType = type,
                    mark = markVal,
                    weight = weightVal
                )

                lifecycleScope.launch {
                    val result = if (isEdit) repository.updateMark(newMark) else repository.addMark(newMark)
                    result.fold(
                        onSuccess = {
                            dialog.dismiss()
                            Snackbar.make(binding.root, "Marks updated", Snackbar.LENGTH_SHORT).show()
                        },
                        onFailure = { e ->
                            Log.e(TAG, "Save failed", e)
                            Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
        dialog.show()
    }

    private fun confirmDeletion(mark: MarkEntry) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Mark")
            .setMessage("Remove this mark for ${mark.moduleCode}?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    repository.deleteMark(mark.id).fold(
                        onSuccess = { Snackbar.make(binding.root, "Mark removed", Snackbar.LENGTH_SHORT).show() },
                        onFailure = { Toast.makeText(requireContext(), "Delete failed", Toast.LENGTH_SHORT).show() }
                    )
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
