package com.campussync.app.activities

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.campussync.app.R
import com.campussync.app.adapters.ModuleReviewAdapter
import com.campussync.app.databinding.ActivityRegisterBinding
import com.campussync.app.models.EnrolledModule
import com.campussync.app.utils.ModuleCatalog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Extended RegisterActivity with a 3-step wizard.
 * Captures account details, Rosebank International academic profile, and auto-assigns modules.
 */
class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // Registration state
    private var email = ""
    private var password = ""
    private var isRosebank = true
    private var campus = ""
    private var course = ""
    private var year = 0
    private var semester = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { handleBack() }

        // Setup Dropdowns for Step 2
        val campusAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, ModuleCatalog.campusLocations)
        binding.actvCampus.setAdapter(campusAdapter)

        val courseAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, ModuleCatalog.courses)
        binding.actvCourse.setAdapter(courseAdapter)

        val yearAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, ModuleCatalog.years)
        binding.actvYear.setAdapter(yearAdapter)

        val semAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, ModuleCatalog.semesters)
        binding.actvSemester.setAdapter(semAdapter)

        // Switch Logic to toggle institutional fields
        binding.switchRosebank.setOnCheckedChangeListener { _, isChecked ->
            isRosebank = isChecked
            binding.layoutInstitutionFields.visibility = if (isChecked) View.VISIBLE else View.GONE
            binding.tvManualNote.visibility = if (isChecked) View.GONE else View.VISIBLE
            binding.tvOptimizationNote.visibility = if (isChecked) View.VISIBLE else View.GONE
            binding.btnNextStep2.text = if (isChecked) "Next" else "Register"
        }

        // Navigation Buttons
        binding.btnNextStep1.setOnClickListener { validateStep1() }
        binding.btnNextStep2.setOnClickListener {
            if (isRosebank) validateStep2() else performRegistration()
        }
        binding.btnRegister.setOnClickListener { performRegistration() }
    }

    private fun validateStep1() {
        email = binding.etEmail.text.toString().trim()
        password = binding.etPassword.text.toString().trim()
        val confirm = binding.etConfirmPassword.text.toString().trim()

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Invalid email format"
            return
        }
        binding.tilEmail.error = null

        if (password.length < 6) {
            binding.tilPassword.error = "Minimum 6 characters"
            return
        }
        binding.tilPassword.error = null

        if (password != confirm) {
            binding.tilConfirmPassword.error = "Passwords do not match"
            return
        }
        binding.tilConfirmPassword.error = null

        moveToStep(1) // Moves to Step 2
    }

    private fun validateStep2() {
        campus = binding.actvCampus.text.toString()
        course = binding.actvCourse.text.toString()
        val yearStr = binding.actvYear.text.toString()
        val semStr = binding.actvSemester.text.toString()

        if (campus.isEmpty() || course.isEmpty() || yearStr.isEmpty() || semStr.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        year = yearStr.toInt()
        semester = semStr.toInt()

        setupReviewStep()
        moveToStep(2) // Moves to Step 3
    }

    private fun setupReviewStep() {
        binding.tvReviewEmail.text = "Email: $email"
        binding.tvReviewCampus.text = "Campus: $campus"
        binding.tvReviewCourse.text = "Course: $course"
        binding.tvReviewYear.text = "Year: $year"
        binding.tvReviewSemester.text = "Semester: $semester"

        binding.tvReviewModulesHeader.text = "Your modules for Year $year, Semester $semester:"

        val catalogModules = ModuleCatalog.getModulesFor(year, semester)
        val modules = catalogModules.map { EnrolledModule(it.code, it.name) }
        
        binding.rvModuleReview.layoutManager = LinearLayoutManager(this)
        binding.rvModuleReview.adapter = ModuleReviewAdapter(modules)
    }

    private fun moveToStep(index: Int) {
        binding.viewFlipper.displayedChild = index
        binding.registrationProgress.progress = index + 1
    }

    private fun handleBack() {
        when (binding.viewFlipper.displayedChild) {
            0 -> finish()
            1 -> moveToStep(0)
            2 -> moveToStep(1)
        }
    }

    private fun performRegistration() {
        setLoading(true)

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val userId = result.user?.uid ?: ""
                saveUserToFirestore(userId)
            }
            .addOnFailureListener { e ->
                setLoading(false)
                Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
            }
    }

    private fun saveUserToFirestore(userId: String) {
        val modules = if (isRosebank) {
            ModuleCatalog.getModulesFor(year, semester)
                .map { mapOf("code" to it.code, "name" to it.name) }
        } else emptyList()

        val userData = hashMapOf(
            "uid" to userId,
            "email" to email,
            "firstName" to "",
            "lastName" to "",
            "isRosebankStudent" to isRosebank,
            "campusLocation" to campus,
            "course" to course,
            "yearOfStudy" to year,
            "currentSemester" to semester,
            "enrolledModules" to modules,
            "language" to "en",
            "createdAt" to System.currentTimeMillis(),
            "updatedAt" to System.currentTimeMillis()
        )

        db.collection("users").document(userId).set(userData)
            .addOnSuccessListener {
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finishAffinity()
            }
            .addOnFailureListener { e ->
                setLoading(false)
                Toast.makeText(this, "Firestore Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.loadingOverlay.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnRegister.isEnabled = !isLoading
        binding.btnNextStep1.isEnabled = !isLoading
        binding.btnNextStep2.isEnabled = !isLoading
        binding.toolbar.navigationIcon?.alpha = if (isLoading) 128 else 255
        binding.toolbar.isEnabled = !isLoading
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (binding.loadingOverlay.visibility == View.VISIBLE) return
        handleBack()
    }
}
