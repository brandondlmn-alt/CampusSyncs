package com.campussync.app.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.campussync.app.R
import com.campussync.app.activities.ScannerAssessmentActivity
import com.campussync.app.adapters.AssessmentAdapter
import com.campussync.app.databinding.FragmentAssessmentsBinding
import com.campussync.app.models.Assessment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

/**
 * Fragment to display and filter saved assessments.
 * Uses client-side sorting and filtering to avoid Firestore index requirements.
 */
class AssessmentsFragment : Fragment() {

    private var _binding: FragmentAssessmentsBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var adapter: AssessmentAdapter
    private var allAssessments = listOf<Assessment>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAssessmentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        observeAssessments()
    }

    private fun setupUI() {
        binding.toolbar.inflateMenu(R.menu.assessment_menu)
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            if (menuItem.itemId == R.id.action_scan_pas) {
                startActivity(Intent(requireContext(), ScannerAssessmentActivity::class.java))
                true
            } else false
        }

        adapter = AssessmentAdapter { assessment ->
            // Handle assessment tap
        }
        binding.rvAssessments.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAssessments.adapter = adapter

        binding.chipGroupFilters.setOnCheckedChangeListener { _, checkedId ->
            applyFilter(checkedId)
        }
        
        binding.fabAdd.setOnClickListener {
            // Handle manual add
        }
    }

    private fun observeAssessments() {
        val uid = auth.currentUser?.uid ?: return
        binding.progressBar.visibility = View.VISIBLE

        // Simplified query: No 'orderBy' to avoid index requirements
        db.collection("assessments")
            .whereEqualTo("studentId", uid)
            .addSnapshotListener { snapshot, error ->
                if (_binding == null) return@addSnapshotListener
                binding.progressBar.visibility = View.GONE

                if (error != null) {
                    return@addSnapshotListener
                }

                // Sort and Store locally
                allAssessments = snapshot?.toObjects(Assessment::class.java)?.sortedBy { it.dueDate } ?: emptyList()
                applyFilter(binding.chipGroupFilters.checkedChipId)
            }
    }

    private fun applyFilter(chipId: Int) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        
        val filtered = when (chipId) {
            R.id.chipUpcoming -> allAssessments.filter { !it.completed && it.dueDate >= today }
            R.id.chipCompleted -> allAssessments.filter { it.completed }
            R.id.chipPast -> allAssessments.filter { it.dueDate < today }
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
