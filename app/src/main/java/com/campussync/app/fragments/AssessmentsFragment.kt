package com.campussync.app.fragments

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
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
import com.campussync.app.R
import com.campussync.app.activities.ScannerAssessmentActivity
import com.campussync.app.adapters.AssessmentAdapter
import com.campussync.app.data.AssessmentRepository
import com.campussync.app.databinding.FragmentAssessmentsBinding
import com.campussync.app.models.Assessment
import com.campussync.app.utils.AssessmentNotificationReceiver
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

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
        if (!isGranted) {
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
            onNotifyClick = { assessment -> showNotificationOptions(assessment) },
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
                if (error != null) return@addSnapshotListener

                allAssessments = snapshot?.toObjects(Assessment::class.java)?.sortedBy { it.dueDate } ?: emptyList()
                applyFilter(binding.chipGroupFilters.checkedChipId)
            }
    }

    private fun toggleCompletion(assessment: Assessment, isChecked: Boolean) {
        lifecycleScope.launch {
            repository.updateCompletionStatus(assessment.id, isChecked).onSuccess {
                if (isChecked) {
                    cancelReminder(assessment) // Cancel notifications if task is done
                }
            }.onFailure {
                Toast.makeText(requireContext(), "Failed to update status", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showNotificationOptions(assessment: Assessment) {
        if (assessment.completed) {
            Toast.makeText(requireContext(), "Assessment is already completed", Toast.LENGTH_SHORT).show()
            return
        }

        val options = arrayOf("1 Hour Before", "3 Hours Before", "1 Day Before", "At Due Time", "Cancel Reminder")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Set Reminder")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> scheduleReminder(assessment, 60)
                    1 -> scheduleReminder(assessment, 180)
                    2 -> scheduleReminder(assessment, 1440)
                    3 -> scheduleReminder(assessment, 0)
                    4 -> {
                        cancelReminder(assessment)
                        Toast.makeText(requireContext(), "Reminder cancelled", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .show()
    }

    private fun scheduleReminder(assessment: Assessment, minutesBefore: Int) {
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
            val dueDateTime = sdf.parse("${assessment.dueDate} ${assessment.dueTime}") ?: return
            
            val calendar = Calendar.getInstance()
            calendar.time = dueDateTime
            calendar.add(Calendar.MINUTE, -minutesBefore)

            if (calendar.timeInMillis <= System.currentTimeMillis()) {
                Toast.makeText(requireContext(), "Due date has already passed", Toast.LENGTH_SHORT).show()
                return
            }

            val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(requireContext(), AssessmentNotificationReceiver::class.java).apply {
                putExtra("MODULE_CODE", assessment.moduleCode)
                putExtra("TYPE", assessment.assessmentType)
                putExtra("ID", assessment.id.hashCode())
            }

            val pendingIntent = PendingIntent.getBroadcast(
                requireContext(),
                assessment.id.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // API 31+ requires exact alarm permission for setExact
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                startActivity(Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                return
            }

            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
            Toast.makeText(requireContext(), "Reminder set for ${SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(calendar.time)}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Failed to set reminder", Toast.LENGTH_SHORT).show()
        }
    }

    private fun cancelReminder(assessment: Assessment) {
        val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(requireContext(), AssessmentNotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            requireContext(),
            assessment.id.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
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
