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
import com.campussync.app.R
import com.campussync.app.adapters.ScannedAssessmentAdapter
import com.campussync.app.data.GeminiRepository
import com.campussync.app.databinding.ActivityScannerAssessmentBinding
import com.campussync.app.models.Assessment
import com.campussync.app.models.ScannedAssessment
import com.campussync.app.utils.DateUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.IOException

/**
 * Activity to scan a PAS (Programme Assessment Schedule) image and extract deadlines.
 * Filters results by the student's year of study (DIS1, DIS2, DIS3).
 */
class ScannerAssessmentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScannerAssessmentBinding
    private val geminiRepo = GeminiRepository()
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var adapter: ScannedAssessmentAdapter? = null
    private val TAG = "ScannerAssessment"

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            startExtraction(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScannerAssessmentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbarPick)
        binding.toolbarPick.setNavigationOnClickListener { finish() }
        
        binding.toolbarReview.setNavigationOnClickListener { 
            binding.viewFlipper.displayedChild = 0 
        }

        binding.btnChooseGallery.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        binding.btnRescan.setOnClickListener {
            binding.viewFlipper.displayedChild = 0
            binding.tvError.visibility = View.GONE
        }

        binding.btnSaveAll.setOnClickListener {
            saveAllEntries()
        }
    }

    private suspend fun getUserYear(): Int {
        val uid = auth.currentUser?.uid ?: return 0
        return try {
            val doc = db.collection("users").document(uid).get().await()
            (doc.getLong("yearOfStudy") ?: 0L).toInt()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch user year", e)
            0
        }
    }

    private fun yearToProgrammeCode(year: Int): String {
        return when (year) {
            1 -> "DIS1"
            2 -> "DIS2"
            3 -> "DIS3"
            else -> ""
        }
    }

    private fun startExtraction(uri: Uri) {
        setLoading(true)
        binding.tvError.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val year = getUserYear()
                val programmeCode = yearToProgrammeCode(year)
                Log.d(TAG, "Filtering assessments by programme code: $programmeCode")

                val inputStream = contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes() ?: throw Exception("Could not read image")
                inputStream.close()

                if (bytes.size > 7 * 1024 * 1024) {
                    throw Exception(getString(R.string.error_image_too_large))
                }

                val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                val mimeType = contentResolver.getType(uri) ?: "image/jpeg"

                val result = geminiRepo.extractAssessmentsFromImage(
                    imageBase64 = base64,
                    mimeType = mimeType,
                    programmeCodeFilter = programmeCode
                )

                result.fold(
                    onSuccess = { entries ->
                        setLoading(false)
                        
                        // Secondary client-side filter to ensure data integrity.
                        val filtered = if (programmeCode.isNotBlank()) {
                            entries.filter { it.programmeCode.trim().uppercase() == programmeCode }
                        } else {
                            entries
                        }

                        if (filtered.isEmpty()) {
                            if (entries.isNotEmpty()) {
                                Log.w(TAG, "Gemini returned ${entries.size} entries but none matched '$programmeCode'.")
                                showError("No assessments found for your year ($programmeCode). Check your profile or try a clearer image.")
                            } else {
                                showError(getString(R.string.error_no_entries))
                            }
                        } else {
                            showReview(filtered)
                            if (year == 0) {
                                Toast.makeText(this@ScannerAssessmentActivity, "Update your profile in Settings to filter assessments.", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    onFailure = { e ->
                        setLoading(false)
                        showError(mapError(e))
                    }
                )
            } catch (e: Exception) {
                setLoading(false)
                showError(e.message ?: getString(R.string.error_generic))
            }
        }
    }

    private fun showReview(entries: List<ScannedAssessment>) {
        binding.viewFlipper.displayedChild = 1
        binding.tvReviewHeader.text = getString(R.string.review_assessments_header, entries.size)
        
        adapter = ScannedAssessmentAdapter(entries.toMutableList())
        binding.rvScannedEntries.layoutManager = LinearLayoutManager(this)
        binding.rvScannedEntries.adapter = adapter
    }

    private fun saveAllEntries() {
        val entries = adapter?.getEntries() ?: return
        val validEntries = entries.filter { it.moduleCode.isNotBlank() && it.moduleCode != "N/A" }

        if (validEntries.isEmpty()) {
            Toast.makeText(this, "No valid entries to save.", Toast.LENGTH_SHORT).show()
            return
        }

        val uid = auth.currentUser?.uid ?: return
        val batch = db.batch()

        validEntries.forEach { scanned ->
            val docRef = db.collection("assessments").document()
            val assessment = Assessment(
                id = docRef.id,
                studentId = uid,
                moduleCode = scanned.moduleCode,
                moduleName = scanned.moduleName,
                assessmentType = scanned.assessmentType,
                submissionMethod = scanned.submissionMethod,
                requiresTurnitin = scanned.requiresTurnitin,
                dueDate = DateUtils.normalizeDate(scanned.dueDate),
                dueTime = scanned.dueTime,
                createdAt = System.currentTimeMillis(),
                completed = false
            )
            batch.set(docRef, assessment)
        }

        binding.btnSaveAll.isEnabled = false
        batch.commit().addOnSuccessListener {
            Toast.makeText(this, getString(R.string.entries_saved, validEntries.size), Toast.LENGTH_SHORT).show()
            finish()
        }.addOnFailureListener { e ->
            binding.btnSaveAll.isEnabled = true
            Toast.makeText(this, "Failed to save: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun mapError(e: Throwable): String = when {
        e is IOException -> getString(R.string.error_network)
        e.message?.contains("429") == true -> getString(R.string.error_rate_limit)
        e.message?.contains("401") == true || e.message?.contains("403") == true -> getString(R.string.error_auth)
        else -> getString(R.string.error_generic)
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
