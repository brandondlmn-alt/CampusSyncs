package com.campussync.app.activities

import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.campussync.app.adapters.ScannedEntryAdapter
import com.campussync.app.data.GeminiRepository
import com.campussync.app.databinding.ActivityScannerBinding
import com.campussync.app.models.ScannedTimetableEntry
import com.campussync.app.models.TimetableEntry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

/**
 * Activity for extracting timetable data from an image using AI vision.
 */
class ScannerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityScannerBinding
    private val repo = GeminiRepository()
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var adapter: ScannedEntryAdapter? = null

    private val picker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { startExtraction(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbarPick)
        binding.toolbarPick.setNavigationOnClickListener { finish() }
        binding.btnChooseGallery.setOnClickListener { picker.launch("image/*") }
        binding.btnRescan.setOnClickListener { binding.viewFlipper.displayedChild = 0 }
        binding.btnSaveAll.setOnClickListener { saveAll() }
    }

    private fun startExtraction(uri: Uri) {
        setLoading(true)
        lifecycleScope.launch {
            try {
                val bytes = contentResolver.openInputStream(uri)?.readBytes() ?: throw Exception("Read failed")
                if (bytes.size > 7 * 1024 * 1024) throw Exception("Image too large")

                val result = repo.extractTimetableFromImage(
                    Base64.encodeToString(bytes, Base64.NO_WRAP),
                    contentResolver.getType(uri) ?: "image/jpeg"
                )

                result.fold(
                    onSuccess = { entries ->
                        if (entries.isEmpty()) showError("No entries found")
                        else showReview(entries)
                    },
                    onFailure = { e -> showError(e.message ?: "Error") }
                )
            } catch (e: Exception) {
                showError(e.message ?: "An error occurred")
            }
            setLoading(false)
        }
    }

    private fun showReview(entries: List<ScannedTimetableEntry>) {
        binding.viewFlipper.displayedChild = 1
        binding.tvReviewHeader.text = "${entries.size} entries found."
        adapter = ScannedEntryAdapter(entries.toMutableList())

        binding.rvScannedEntries.layoutManager = LinearLayoutManager(this)
        binding.rvScannedEntries.adapter = adapter
    }

    private fun saveAll() {
        val valid = adapter?.getEntries()
            ?.filter { it.moduleCode.isNotBlank() && it.moduleCode != "N/A" } ?: emptyList()

        if (valid.isEmpty()) {
            Toast.makeText(this, "No valid entries", Toast.LENGTH_SHORT).show()
            return
        }

        val uid = auth.currentUser?.uid ?: return
        val batch = db.batch()

        valid.forEach { entry ->
            val doc = db.collection("timetable").document()
            batch.set(doc, TimetableEntry(
                id = doc.id,
                studentId = uid,
                moduleCode = entry.moduleCode,
                moduleName = entry.moduleName,
                dayOfWeek = entry.dayOfWeek,
                startTime = entry.startTime,
                endTime = entry.endTime,
                venue = entry.venue
            ))
        }

        batch.commit().addOnSuccessListener {
            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setLoading(load: Boolean) {
        binding.layoutPick.visibility = if (load) View.GONE else View.VISIBLE
        binding.layoutScanning.visibility = if (load) View.VISIBLE else View.GONE
    }

    private fun showError(m: String) {
        binding.tvError.text = m
        binding.tvError.visibility = View.VISIBLE
    }
}