package com.campussync.app.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.campussync.app.adapters.AccommodationAdapter
import com.campussync.app.adapters.BursaryAdapter
import com.campussync.app.data.ResourceRepository
import com.campussync.app.databinding.FragmentResourcesBinding
import com.campussync.app.models.Accommodation
import com.campussync.app.models.Bursary
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch

/**
 * Fragment to display academic resources: Bursaries and Student Accommodation.
 * Read-only for students. Includes search/filtering functionality.
 */
class ResourcesFragment : Fragment() {

    private var _binding: FragmentResourcesBinding? = null
    private val binding get() = _binding!!

    private val repository = ResourceRepository()
    private val bursaryAdapter = BursaryAdapter()
    private val accommodationAdapter = AccommodationAdapter()

    private var allBursaries = listOf<Bursary>()
    private var allAccommodation = listOf<Accommodation>()

    private val TAG = "ResourcesFragment"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResourcesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerViews()
        setupTabLayout()
        setupSearch()
        loadData()
    }

    private fun setupRecyclerViews() {
        binding.rvBursaries.adapter = bursaryAdapter
        binding.rvAccommodation.adapter = accommodationAdapter
    }

    private fun setupTabLayout() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> { // Bursaries
                        binding.rvBursaries.visibility = View.VISIBLE
                        binding.rvAccommodation.visibility = View.GONE
                        performSearch(binding.etSearch.text.toString())
                    }
                    1 -> { // Accommodation
                        binding.rvBursaries.visibility = View.GONE
                        binding.rvAccommodation.visibility = View.VISIBLE
                        performSearch(binding.etSearch.text.toString())
                    }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                performSearch(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun loadData() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            // Load Bursaries
            repository.getBursaries().fold(
                onSuccess = { bursaries ->
                    allBursaries = bursaries
                    bursaryAdapter.submitList(bursaries)
                },
                onFailure = { e ->
                    Log.e(TAG, "Failed to load bursaries", e)
                    Toast.makeText(requireContext(), "Error loading bursaries", Toast.LENGTH_SHORT).show()
                }
            )

            // Load Accommodation
            repository.getAccommodation().fold(
                onSuccess = { accommodation ->
                    allAccommodation = accommodation
                    accommodationAdapter.submitList(accommodation)
                },
                onFailure = { e ->
                    Log.e(TAG, "Failed to load accommodation", e)
                }
            )
            
            binding.progressBar.visibility = View.GONE
            updateEmptyState()
        }
    }

    private fun performSearch(query: String) {
        val filteredQuery = query.lowercase().trim()
        
        if (binding.tabLayout.selectedTabPosition == 0) {
            val filtered = allBursaries.filter { 
                it.name.lowercase().contains(filteredQuery) || 
                it.provider.lowercase().contains(filteredQuery) 
            }
            bursaryAdapter.submitList(filtered)
        } else {
            val filtered = allAccommodation.filter { 
                it.name.lowercase().contains(filteredQuery) || 
                it.location.lowercase().contains(filteredQuery) 
            }
            accommodationAdapter.submitList(filtered)
        }
        updateEmptyState()
    }

    private fun updateEmptyState() {
        val isEmpty = if (binding.tabLayout.selectedTabPosition == 0) {
            bursaryAdapter.currentList.isEmpty()
        } else {
            accommodationAdapter.currentList.isEmpty()
        }
        binding.tvEmptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
