package com.campussync.app.fragments

import android.content.Intent
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
import com.campussync.app.activities.ScannerActivity
import com.campussync.app.adapters.TimetableAdapter
import com.campussync.app.data.ModuleRepository
import com.campussync.app.data.TimetableRepository
import com.campussync.app.databinding.DialogAddTimetableEntryBinding
import com.campussync.app.databinding.FragmentTimetableBinding
import com.campussync.app.models.Module
import com.campussync.app.models.TimetableEntry
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Fragment for viewing and managing the student's weekly class schedule.
 */
class TimetableFragment : Fragment() {

    private var _binding: FragmentTimetableBinding? = null
    private val binding get() = _binding!!

    private val repository = TimetableRepository()
    private val moduleRepository = ModuleRepository()
    private lateinit var adapter: TimetableAdapter
    private val TAG = "TimetableFragment"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTimetableBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupRecyclerView()
        observeTimetable()

        binding.fabAddEntry.setOnClickListener {
            checkModulesAndShowDialog(null)
        }
    }

    private fun setupToolbar() {
        binding.toolbar.inflateMenu(R.menu.timetable_menu)
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            if (menuItem.itemId == R.id.action_scan) {
                startActivity(Intent(requireContext(), ScannerActivity::class.java))
                true
            } else false
        }
    }

    private fun setupRecyclerView() {
        adapter = TimetableAdapter(
            onEditClick = { entry -> checkModulesAndShowDialog(entry) },
            onDeleteClick = { entry -> confirmDeletion(entry) }
        )
        binding.rvTimetable.adapter = adapter
    }

    private fun observeTimetable() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            repository.getTimetableEntries().collect { entries ->
                binding.progressBar.visibility = View.GONE
                adapter.submitList(entries)
                binding.tvEmptyState.visibility = if (entries.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun checkModulesAndShowDialog(entry: TimetableEntry?) {
        lifecycleScope.launch {
            val modules = moduleRepository.getModules().first()
            if (modules.isEmpty()) {
                Toast.makeText(requireContext(), "Please add modules in Settings first", Toast.LENGTH_LONG).show()
            } else {
                showAddEditDialog(entry, modules)
            }
        }
    }

    private fun showAddEditDialog(entry: TimetableEntry?, modules: List<Module>) {
        val dialogBinding = DialogAddTimetableEntryBinding.inflate(layoutInflater)
        val isEdit = entry != null

        val moduleCodes = modules.map { it.code }.toTypedArray()
        dialogBinding.actvModuleCode.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, moduleCodes))

        val days = arrayOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
        dialogBinding.actvDay.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, days))

        dialogBinding.etStartTime.setOnClickListener {
            showTimePicker { time -> dialogBinding.etStartTime.setText(time) }
        }
        dialogBinding.etEndTime.setOnClickListener {
            showTimePicker { time -> dialogBinding.etEndTime.setText(time) }
        }

        if (isEdit) {
            dialogBinding.tvDialogTitle.text = "Edit Class"
            dialogBinding.actvModuleCode.setText(entry?.moduleCode, false)
            dialogBinding.etModuleName.setText(entry?.moduleName)
            dialogBinding.actvDay.setText(entry?.dayOfWeek, false)
            dialogBinding.etStartTime.setText(entry?.startTime)
            dialogBinding.etEndTime.setText(entry?.endTime)
            dialogBinding.etVenue.setText(entry?.venue)
        }

        dialogBinding.actvModuleCode.setOnItemClickListener { _, _, position, _ ->
            val selectedModule = modules.find { it.code == moduleCodes[position] }
            dialogBinding.etModuleName.setText(selectedModule?.name)
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .setPositiveButton(if (isEdit) "Update" else "Add", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            val button = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            button.setOnClickListener {
                val code = dialogBinding.actvModuleCode.text.toString().trim()
                val name = dialogBinding.etModuleName.text.toString().trim()
                val day = dialogBinding.actvDay.text.toString()
                val start = dialogBinding.etStartTime.text.toString()
                val end = dialogBinding.etEndTime.text.toString()
                val venue = dialogBinding.etVenue.text.toString().trim()

                if (code.isEmpty() || day.isEmpty() || start.isEmpty() || end.isEmpty()) {
                    Toast.makeText(requireContext(), "Please fill all required fields", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val newEntry = (entry ?: TimetableEntry()).copy(
                    moduleCode = code,
                    moduleName = name,
                    dayOfWeek = day,
                    startTime = start,
                    endTime = end,
                    venue = venue
                )

                lifecycleScope.launch {
                    val result = if (isEdit) repository.updateEntry(newEntry) else repository.addEntry(newEntry)
                    result.fold(
                        onSuccess = {
                            dialog.dismiss()
                            Snackbar.make(binding.root, "Timetable updated", Snackbar.LENGTH_SHORT).show()
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

    private fun showTimePicker(onTimeSelected: (String) -> Unit) {
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(Calendar.getInstance().get(Calendar.HOUR_OF_DAY))
            .setMinute(0)
            .setTitleText("Select Time")
            .build()

        picker.addOnPositiveButtonClickListener {
            val time = String.format("%02d:%02d", picker.hour, picker.minute)
            onTimeSelected(time)
        }
        picker.show(childFragmentManager, "time_picker")
    }

    private fun confirmDeletion(entry: TimetableEntry) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Entry")
            .setMessage("Delete ${entry.moduleCode} class?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    repository.deleteEntry(entry.id).fold(
                        onSuccess = { Snackbar.make(binding.root, "Entry deleted", Snackbar.LENGTH_SHORT).show() },
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
