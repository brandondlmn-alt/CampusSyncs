package com.campussync.app.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.campussync.app.adapters.MarkAdapter
import com.campussync.app.data.*
import com.campussync.app.databinding.FragmentDashboardBinding
import com.campussync.app.models.MarkListItem
import com.campussync.app.models.TimetableEntry
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Dashboard Fragment that aggregates data from all features.
 * Shows welcome message, next class, academic average, and budget summary.
 */
class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val authRepo = AuthRepository()
    private val timetableRepo = TimetableRepository()
    private val markRepo = MarkRepository()
    private val budgetRepo = BudgetRepository()
    
    private val markAdapter = MarkAdapter(onEditClick = {}, onDeleteClick = {})
    private val TAG = "DashboardFragment"

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
    }

    private fun loadDashboardData() {
        binding.progressBar.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            // 1. Load User Profile for Welcome Message
            authRepo.getCurrentUserProfile().onSuccess { user ->
                binding.tvWelcome.text = "Hi, ${user?.firstName ?: "Student"}!"
            }

            // 2. Aggregate real-time data from all modules
            combine(
                timetableRepo.getTimetableEntries(),
                markRepo.getMarkEntries(),
                budgetRepo.getCurrentMonthExpenses()
            ) { timetable, marks, expenses ->
                DashboardData(timetable, marks, expenses)
            }.collect { data ->
                binding.progressBar.visibility = View.GONE
                updateUI(data)
            }
        }
    }

    private fun updateUI(data: DashboardData) {
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
        val average = markRepo.calculateWeightedAverage(data.marks)
        binding.tvCurrentAvg.text = String.format(Locale.getDefault(), "%.1f%%", average)

        // --- Budget Remaining ---
        // Note: Budget settings are fetched once or we could observe them. 
        // For Dashboard simplicity, we'll use the cached/fetched settings if available.
        lifecycleScope.launch {
            budgetRepo.getBudgetSettings().onSuccess { settings ->
                val allowance = settings?.monthlyAllowance ?: 0.0
                val spent = data.expenses.sumOf { it.amount }
                binding.tvBudgetRemaining.text = String.format(Locale.getDefault(), "R %.0f", (allowance - spent))
            }
        }

        // --- Recent Assessments ---
        val recentMarks = data.marks
            .sortedByDescending { it.id } // Firestore IDs are roughly chronological, or add a timestamp
            .take(3)
            .map { MarkListItem.Item(it) }
        markAdapter.submitList(recentMarks)
    }

    private fun findNextClass(entries: List<TimetableEntry>): TimetableEntry? {
        if (entries.isEmpty()) return null
        
        val now = Calendar.getInstance()
        val days = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
        val currentDay = days[now.get(Calendar.DAY_OF_WEEK) - 1]
        val currentTime = String.format("%02d:%02d", now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE))

        // Classes for today that haven't started yet
        return entries
            .filter { it.dayOfWeek == currentDay && it.startTime >= currentTime }
            .minByOrNull { it.startTime }
    }

    // Helper class to group data for the Flow combine operator
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
