package com.campussync.app.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.campussync.app.adapters.MarkAdapter
import com.campussync.app.data.*
import com.campussync.app.databinding.FragmentDashboardBinding
import com.campussync.app.models.Assessment
import com.campussync.app.models.MarkListItem
import com.campussync.app.models.TimetableEntry
import com.campussync.app.utils.DateUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Dashboard Fragment that aggregates data from all features.
 * Updated to handle upcoming assessments without requiring a composite index.
 */
class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val authRepo = AuthRepository()
    private val timetableRepo = TimetableRepository()
    private val markRepo = MarkRepository()
    private val budgetRepo = BudgetRepository()
    private val geminiRepo = GeminiRepository()
    private val db = FirebaseFirestore.getInstance()
    
    private val markAdapter = MarkAdapter(onEditClick = {}, onDeleteClick = {})
    private val TAG = "DashboardFragment"
    private var currentAverage = 0.0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.rvRecentMarks.adapter = markAdapter
        loadDashboardData()
        setupAiAssistant()
        observeUpcomingAssessments()
    }

    private fun loadDashboardData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                
                binding.progressBar.visibility = View.VISIBLE

                // 1. Load User Profile
                authRepo.getCurrentUserProfile().onSuccess { user ->
                    if (_binding != null) {
                        binding.tvWelcome.text = "Hi, ${user?.firstName ?: "Student"}!"
                    }
                }

                // 2. Aggregate real-time data
                combine(
                    timetableRepo.getTimetableEntries(),
                    markRepo.getMarkEntries(),
                    budgetRepo.getCurrentMonthExpenses()
                ) { timetable, marks, expenses ->
                    DashboardData(timetable, marks, expenses)
                }.collect { data ->
                    if (_binding != null) {
                        binding.progressBar.visibility = View.GONE
                        updateUI(data)
                    }
                }
            }
        }
    }

    private fun updateUI(data: DashboardData) {
        if (_binding == null) return

        // --- Next Class ---
        val nextClass = findNextClass(data.timetable)
        if (nextClass != null) {
            binding.tvNextClass.text = "${nextClass.moduleCode} @ ${nextClass.startTime}"
            binding.tvNextClassVenue.text = nextClass.venue
        } else {
            binding.tvNextClass.text = "No classes today"
            binding.tvNextClassVenue.text = "Enjoy your break!"
        }

        // --- Academic Average ---
        currentAverage = markRepo.calculateWeightedAverage(data.marks)
        binding.tvCurrentAvg.text = String.format(Locale.getDefault(), "%.1f%%", currentAverage)

        // --- Budget Remaining ---
        viewLifecycleOwner.lifecycleScope.launch {
            budgetRepo.getBudgetSettings().onSuccess { settings ->
                if (_binding != null) {
                    val allowance = settings?.monthlyAllowance ?: 0.0
                    val spent = data.expenses.sumOf { it.amount }
                    binding.tvBudgetRemaining.text = String.format(Locale.getDefault(), "R %.0f", (allowance - spent))
                }
            }
        }

        // --- Recent Assessments ---
        val recentMarks = data.marks
            .sortedByDescending { it.id }
            .take(3)
            .map { MarkListItem.Item(it) }
        markAdapter.submitList(recentMarks)
    }

    private fun observeUpcomingAssessments() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

        // Simplified query to avoid index requirement
        db.collection("assessments")
            .whereEqualTo("studentId", uid)
            .addSnapshotListener { snapshot, _ ->
                if (_binding == null) return@addSnapshotListener
                val allAssessments = snapshot?.toObjects(Assessment::class.java) ?: emptyList()
                
                // Filter and sort client-side
                val upcoming = allAssessments
                    .filter { !it.completed && it.dueDate >= today }
                    .sortedBy { it.dueDate }
                    .take(3)
                
                updateDeadlinesUI(upcoming)
            }
    }

    private fun updateDeadlinesUI(assessments: List<Assessment>) {
        if (_binding == null) return
        
        if (assessments.isEmpty()) {
            binding.tvDeadlinesSummary.text = "No upcoming deadlines 🎉"
            return
        }

        val sb = StringBuilder()
        assessments.forEach { assessment ->
            val countdown = DateUtils.getCountdown(assessment.dueDate)
            sb.append("${assessment.moduleCode}: ${assessment.assessmentType}\n")
            sb.append("Due: ${DateUtils.formatForDisplay(assessment.dueDate, assessment.dueTime)} ($countdown)\n\n")
        }
        binding.tvDeadlinesSummary.text = sb.toString().trim()
    }

    private fun setupAiAssistant() {
        binding.btnGetAiTip.setOnClickListener {
            val prompt = if (currentAverage > 0) {
                "I am a student with a current weighted average of ${String.format("%.1f", currentAverage)}%. " +
                "Give me one short, highly practical study tip. Keep it under 30 words."
            } else {
                "I am a student. Give me one short study tip. Keep it under 30 words."
            }

            setAiLoading(true)
            viewLifecycleOwner.lifecycleScope.launch {
                val result = geminiRepo.generateContent(prompt)
                if (_binding != null) {
                    setAiLoading(false)
                    result.fold(
                        onSuccess = { tip -> binding.tvAiResponse.text = tip.trim() },
                        onFailure = { e -> 
                            binding.tvAiResponse.text = "Try again later"
                            Log.e(TAG, "AI Error", e)
                        }
                    )
                }
            }
        }
    }

    private fun setAiLoading(isLoading: Boolean) {
        if (_binding == null) return
        binding.aiProgressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnGetAiTip.isEnabled = !isLoading
        if (isLoading) binding.tvAiResponse.text = "Thinking..."
    }

    private fun findNextClass(entries: List<TimetableEntry>): TimetableEntry? {
        if (entries.isEmpty()) return null
        val now = Calendar.getInstance()
        val days = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
        val currentDay = days[now.get(Calendar.DAY_OF_WEEK) - 1]
        val currentTime = String.format("%02d:%02d", now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE))

        return entries
            .filter { it.dayOfWeek == currentDay && it.startTime >= currentTime }
            .minByOrNull { it.startTime }
    }

    data class DashboardData(
        val timetable: List<TimetableEntry>,
        val marks: List<com.campussync.app.models.MarkEntry>,
        val expenses: List<com.campussync.app.models.Expense>
    )

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
