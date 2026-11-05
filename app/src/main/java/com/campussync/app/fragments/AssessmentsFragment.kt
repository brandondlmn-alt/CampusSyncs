package com.campussync.app.fragments

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.campussync.app.R
import com.campussync.app.activities.ScannerAssessmentActivity
import com.campussync.app.adapters.AssessmentAdapter
import com.campussync.app.data.AssessmentRepository
import com.campussync.app.databinding.FragmentAssessmentsBinding
import com.campussync.app.models.Assessment
import com.campussync.app.utils.SingleReminderWorker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class AssessmentsFragment : Fragment() {

    private var _binding: FragmentAssessmentsBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val repository = AssessmentRepository()
    private lateinit var adapter: AssessmentAdapter
    private var allAssessments = listOf<Assessment>()
    private val TAG = "AssessmentsFragment"

    // Permission request for Android 13+
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted && isAdded) {
            Toast.makeText(requireContext(), "Notifications are disabled. You won't receive reminders.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAssessmentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkNotificationPermission()
        setupUI()
        observeAssessments()
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun setupUI() {
        binding.toolbar.inflateMenu(R.menu.assessment_menu)
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            if (menuItem.itemId == R.id.action_scan_pas) {
                startActivity(Intent(requireContext(), ScannerAssessmentActivity::class.java))
                true
            } else false
        }

        adapter = AssessmentAdapter(
            onCompleteToggle = { assessment, isChecked -> toggleCompletion(assessment, isChecked) },
            onNotifyClick = { assessment -> showReminderOptionsDialog(assessment) },
            onClick = { assessment -> /* Future: Edit Dialog */ }
        )
        binding.rvAssessments.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAssessments.adapter = adapter

        binding.chipGroupFilters.setOnCheckedChangeListener { _, checkedId -> applyFilter(checkedId) }
    }

    private fun observeAssessments() {
        val uid = auth.currentUser?.uid ?: return
        binding.progressBar.visibility = View.VISIBLE

        db.collection("assessments")
            .whereEqualTo("studentId", uid)
            .addSnapshotListener { snapshot, error ->
                if (_binding == null) return@addSnapshotListener
                binding.progressBar.visibility = View.GONE
                if (error != null) {
                    Log.e(TAG, "Error observing assessments", error)
                    return@addSnapshotListener
                }

                allAssessments = snapshot?.toObjects(Assessment::class.java)?.sortedBy { it.dueDate } ?: emptyList()
                applyFilter(binding.chipGroupFilters.checkedChipId)
            }
    }

    private fun toggleCompletion(assessment: Assessment, isChecked: Boolean) {

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                repository.updateCompletionStatus(assessment.id, isChecked).onSuccess {
                    if (isChecked) {
                        WorkManager.getInstance(requireContext()).cancelAllWorkByTag("reminder_${assessment.id}")
                    }
                }.onFailure {
                    if (isAdded && _binding != null) {
                        Toast.makeText(requireContext(), "Failed to update status", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: CancellationException) {
                // Ignore normal navigation
            }
        }
    }

    private fun showReminderOptionsDialog(assessment: Assessment) {
        if (assessment.completed) {
            if (isAdded && _binding != null) {
                Toast.makeText(requireContext(), "Assessment is already completed", Toast.LENGTH_SHORT).show()
            }
            return
        }

        val options = arrayOf("Set Custom Timer", "Cancel Existing Reminder")

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Reminder Settings")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openCustomDateTimePicker(assessment)
                    1 -> {
                        WorkManager.getInstance(requireContext()).cancelAllWorkByTag("reminder_${assessment.id}")
                        if (isAdded && _binding != null) {
                            Toast.makeText(requireContext(), "Reminder cancelled", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .show()
    }

    private fun openCustomDateTimePicker(assessment: Assessment) {
        val calendar = Calendar.getInstance()

        // 1. Date Picker
        val datePicker = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                // 2. Time Picker
                val timePicker = TimePickerDialog(
                    requireContext(),
                    { _, hourOfDay, minute ->
                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        calendar.set(Calendar.MINUTE, minute)
                        calendar.set(Calendar.SECOND, 0)

                        scheduleCustomReminder(assessment, calendar.timeInMillis)
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true // 24-hour format
                )
                timePicker.show()

            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        datePicker.datePicker.minDate = System.currentTimeMillis() - 1000
        datePicker.setTitle("Select Reminder Date")
        datePicker.show()
    }

    private fun scheduleCustomReminder(assessment: Assessment, triggerTimeMillis: Long) {
        val currentTimeMillis = System.currentTimeMillis()

        if (triggerTimeMillis <= currentTimeMillis) {
            if (isAdded && _binding != null) {
                Toast.makeText(requireContext(), "Reminder time has already passed.", Toast.LENGTH_SHORT).show()
            }
            return
        }

        val delay = triggerTimeMillis - currentTimeMillis
        val notificationId = assessment.id.hashCode()

        val workData = Data.Builder()
            .putString("moduleName", assessment.moduleName)
            .putString("type", assessment.assessmentType)
            .putInt("notificationId", notificationId)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<SingleReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(workData)
            .addTag("reminder_${assessment.id}")
            .build()

        WorkManager.getInstance(requireContext()).enqueue(workRequest)

        val format = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
        val formattedTime = format.format(Date(triggerTimeMillis))

        if (isAdded && _binding != null) {
            Toast.makeText(requireContext(), "Sticky reminder set for $formattedTime", Toast.LENGTH_LONG).show()
        }
    }

    private fun applyFilter(chipId: Int) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val filtered = when (chipId) {
            R.id.chipUpcoming -> allAssessments.filter { !it.completed && it.dueDate >= today }
            R.id.chipCompleted -> allAssessments.filter { it.completed }
            R.id.chipPast -> allAssessments.filter { it.dueDate < today && !it.completed }
            else -> allAssessments
        }
        adapter.submitList(filtered)
        binding.layoutEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}