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
 * Activity for scanning a timetable image using AI.
 * Steps: 1. Pick Image, 2. Review and Save.
 */
class ScannerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScannerBinding
    private val geminiRepo = GeminiRepository()
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private var scannedAdapter: ScannedEntryAdapter? = null
    private val TAG = "ScannerActivity"

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            startExtraction(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
    }

    private fun setupUI() {
        // Step 1 UI
        setSupportActionBar(binding.toolbarPick)
        binding.toolbarPick.setNavigationOnClickListener { finish() }
        binding.btnChooseGallery.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        // Step 2 UI
        binding.toolbarReview.setNavigationOnClickListener { 
            binding.viewFlipper.displayedChild = 0 
        }
        binding.btnRescan.setOnClickListener { 
            binding.viewFlipper.displayedChild = 0 
            binding.tvError.visibility = View.GONE
        }
        binding.btnSaveAll.setOnClickListener { saveAllEntries() }
    }

    private fun startExtraction(uri: Uri) {
        setLoading(true)
        binding.tvError.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes() ?: throw Exception("Could not read image")
                inputStream.close()

                if (bytes.size > 7 * 1024 * 1024) {
                    throw Exception("Image too large. Max 7MB.")
                }

                val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                val mimeType = contentResolver.getType(uri) ?: "image/jpeg"

                val result = geminiRepo.extractTimetableFromImage(base64, mimeType)

                result.fold(
                    onSuccess = { entries ->
                        setLoading(false)
                        if (entries.isEmpty()) {
                            showError("No timetable entries detected. Try a clearer image.")
                        } else {
                            showReview(entries)
                        }
                    },
                    onFailure = { e ->
                        setLoading(false)
                        showError(e.message ?: "Failed to process image")
                    }
                )
            } catch (e: Exception) {
                setLoading(false)
                showError(e.message ?: "An error occurred")
            }
        }
    }

    private fun showReview(entries: List<ScannedTimetableEntry>) {
        binding.viewFlipper.displayedChild = 1
        binding.tvReviewHeader.text = "${entries.size} entries found. Tap to edit before saving."
        
        scannedAdapter = ScannedEntryAdapter(entries.toMutableList())
        binding.rvScannedEntries.apply {
            layoutManager = LinearLayoutManager(this@ScannerActivity)
            adapter = scannedAdapter
        }
    }

    private fun saveAllEntries() {
        val entries = scannedAdapter?.getEntries() ?: return
        val validEntries = entries.filter { it.moduleCode.isNotBlank() && it.moduleCode != "N/A" }

        if (validEntries.isEmpty()) {
            Toast.makeText(this, "No valid entries to save.", Toast.LENGTH_SHORT).show()
            return
        }

        val uid = auth.currentUser?.uid ?: return
        val batch = db.batch()

        validEntries.forEach { scanned ->
            val docRef = db.collection("timetable").document()
            val entry = TimetableEntry(
                id = docRef.id,
                studentId = uid,
                moduleCode = scanned.moduleCode,
                moduleName = scanned.moduleName,
                dayOfWeek = scanned.dayOfWeek,
                startTime = scanned.startTime,
                endTime = scanned.endTime,
                venue = scanned.venue
            )
            batch.set(docRef, entry)
        }

        binding.btnSaveAll.isEnabled = false
        batch.commit().addOnSuccessListener {
            Toast.makeText(this, "${validEntries.size} entries saved to your timetable", Toast.LENGTH_SHORT).show()
            finish()
        }.addOnFailureListener { e ->
            binding.btnSaveAll.isEnabled = true
            Toast.makeText(this, "Failed to save: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.layoutPick.visibility = if (isLoading) View.GONE else View.VISIBLE
        binding.layoutScanning.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnChooseGallery.isEnabled = !isLoading
    }

    private fun showError(message: String) {
        binding.tvError.text = message
        binding.tvError.visibility = View.VISIBLE
        Log.e(TAG, message)
    }
}
