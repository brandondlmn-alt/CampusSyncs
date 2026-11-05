package com.campussync.app.activities

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.campussync.app.adapters.ModuleReviewAdapter
import com.campussync.app.databinding.ActivityRegisterBinding
import com.campussync.app.models.EnrolledModule
import com.campussync.app.utils.ModuleCatalog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Activity for student registration and profile setup.
 * Captures personal details and auto-assigns academic modules.
 */
class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private var firstName = ""
    private var lastName = ""
    private var email = ""
    private var password = ""
    private var isRosebank = true
    private var campus = ""
    private var course = ""
    private var year = 0
    private var semester = 0
    private var isSsoSetup = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        isSsoSetup = intent.getBooleanExtra("IS_SSO_SETUP", false) || auth.currentUser != null

        setupUI()

        if (isSsoSetup) {
            prefillSsoUserData()
        }
    }

    /**
     * Pre-fills the form with Google account details and hides password fields.
     */
    private fun prefillSsoUserData() {
        val currentUser = auth.currentUser ?: return

        email = currentUser.email ?: ""
        binding.etEmail.setText(email)
        binding.etEmail.isEnabled = false

        val names = currentUser.displayName?.split(" ") ?: emptyList()
        firstName = names.getOrNull(0) ?: ""
        lastName = names.getOrNull(1) ?: ""
        binding.etFirstName.setText(firstName)
        binding.etLastName.setText(lastName)

        binding.tilPassword.visibility = View.GONE
        binding.tilConfirmPassword.visibility = View.GONE
    }

    /**
     * Sets up the dropdowns, switches, and button listeners.
     */
    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { handleBack() }

        val campusAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, ModuleCatalog.campusLocations)
        binding.actvCampus.setAdapter(campusAdapter)

        val courseAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, ModuleCatalog.courses)
        binding.actvCourse.setAdapter(courseAdapter)

        val yearAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, ModuleCatalog.years)
        binding.actvYear.setAdapter(yearAdapter)

        val semAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, ModuleCatalog.semesters)
        binding.actvSemester.setAdapter(semAdapter)

        binding.switchRosebank.setOnCheckedChangeListener { _, isChecked ->
            isRosebank = isChecked
            binding.layoutInstitutionFields.visibility = if (isChecked) View.VISIBLE else View.GONE
            binding.tvManualNote.visibility = if (isChecked) View.GONE else View.VISIBLE
            binding.tvOptimizationNote.visibility = if (isChecked) View.GONE else View.VISIBLE
            binding.btnNextStep2.text = if (isChecked) "Next" else "Register"
        }

        binding.btnNextStep1.setOnClickListener { validateStep1() }
        binding.btnNextStep2.setOnClickListener {
            if (isRosebank) validateStep2() else performRegistration()
        }
        binding.btnRegister.setOnClickListener { performRegistration() }
    }

    /**
     * Validates the inputs for the first step of registration.
     */
    private fun validateStep1() {
        firstName = binding.etFirstName.text.toString().trim()
        lastName = binding.etLastName.text.toString().trim()

        if (firstName.isEmpty()) {
            binding.tilFirstName.error = "Required"
            return
        } else binding.tilFirstName.error = null

        if (lastName.isEmpty()) {
            binding.tilLastName.error = "Required"
            return
        } else binding.tilLastName.error = null

        if (!isSsoSetup) {
            email = binding.etEmail.text.toString().trim()
            password = binding.etPassword.text.toString().trim()
            val confirm = binding.etConfirmPassword.text.toString().trim()

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.tilEmail.error = "Invalid email format"
                return
            } else binding.tilEmail.error = null

            if (password.length < 6) {
                binding.tilPassword.error = "Minimum 6 characters"
                return
            } else binding.tilPassword.error = null

            if (password != confirm) {
                binding.tilConfirmPassword.error = "Passwords do not match"
                return
            } else binding.tilConfirmPassword.error = null
        }

        moveToStep(1)
    }

    /**
     * Validates the inputs for the second step of registration.
     */
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
        moveToStep(2)
    }

    /**
     * Populates the review screen with the user's selected data.
     */
    private fun setupReviewStep() {
        binding.tvReviewName.text = "Name: $firstName $lastName"
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

    /**
     * Changes the visible step in the registration flow.
     */
    private fun moveToStep(index: Int) {
        binding.viewFlipper.displayedChild = index
        binding.registrationProgress.progress = index + 1
    }

    /**
     * Handles the back navigation logic through the steps.
     */
    private fun handleBack() {
        when (binding.viewFlipper.displayedChild) {
            0 -> finish()
            1 -> moveToStep(0)
            2 -> moveToStep(1)
        }
    }

    /**
     * Executes registration or updates the profile if in SSO setup mode.
     */
    private fun performRegistration() {
        setLoading(true)
        lifecycleScope.launch {
            try {
                if (isSsoSetup && auth.currentUser != null) {
                    saveUserAndModulesToFirestore(auth.currentUser!!.uid)
                } else {
                    val result = auth.createUserWithEmailAndPassword(email, password).await()
                    val userId = result.user?.uid ?: throw Exception("Auth failed")
                    saveUserAndModulesToFirestore(userId)
                }
            } catch (e: Exception) {
                setLoading(false)
                Toast.makeText(this@RegisterActivity, e.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Saves the user's profile and assigned modules to Firestore.
     */
    private fun saveUserAndModulesToFirestore(userId: String) {
        lifecycleScope.launch {
            try {
                val batch = db.batch()

                val catalogModules = if (isRosebank) {
                    ModuleCatalog.getModulesFor(year, semester)
                } else emptyList()

                catalogModules.forEach { catalogModule ->
                    val moduleDocRef = db.collection("modules").document()
                    val moduleData = hashMapOf(
                        "id" to moduleDocRef.id,
                        "studentId" to userId,
                        "code" to catalogModule.code,
                        "name" to catalogModule.name,
                        "targetClassesPerWeek" to 2
                    )
                    batch.set(moduleDocRef, moduleData)
                }

                val userDocRef = db.collection("users").document(userId)
                val userData = hashMapOf(
                    "uid" to userId,
                    "email" to email,
                    "firstName" to firstName,
                    "lastName" to lastName,
                    "isRosebankStudent" to isRosebank,
                    "campusLocation" to campus,
                    "course" to course,
                    "yearOfStudy" to year,
                    "currentSemester" to semester,
                    "language" to "en",
                    "createdAt" to System.currentTimeMillis()
                )
                batch.set(userDocRef, userData)

                batch.commit().await()

                val intent = Intent(this@RegisterActivity, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finishAffinity()
            } catch (e: Exception) {
                setLoading(false)
                Toast.makeText(this@RegisterActivity, "Sync Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Toggles the loading state of the UI components.
     */
    private fun setLoading(isLoading: Boolean) {
        binding.loadingOverlay.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnRegister.isEnabled = !isLoading
        binding.btnNextStep1.isEnabled = !isLoading
        binding.btnNextStep2.isEnabled = !isLoading
    }
}