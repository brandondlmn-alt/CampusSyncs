package com.campussync.app.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.campussync.app.data.AuthRepository
import com.campussync.app.databinding.ActivityRegisterBinding
import com.campussync.app.models.User
import kotlinx.coroutines.launch

/**
 * Activity for user registration.
 * Handles input validation, Firebase Auth account creation, and Firestore profile creation.
 */
class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val authRepository = AuthRepository()
    private val TAG = "RegisterActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.d(TAG, "Register screen launched")

        binding.btnRegister.setOnClickListener {
            attemptRegister()
        }

        binding.tvGoToLogin.setOnClickListener {
            finish() // Return to Login
        }
    }

    private fun attemptRegister() {
        val firstName = binding.etFirstName.text.toString().trim()
        val lastName = binding.etLastName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val institution = binding.etInstitution.text.toString().trim()
        val course = binding.etCourse.text.toString().trim()
        val year = binding.etYear.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val confirmPassword = binding.etConfirmPassword.text.toString().trim()

        // Validation
        var isValid = true

        if (firstName.isEmpty()) {
            binding.tilFirstName.error = "Required"
            isValid = false
        } else binding.tilFirstName.error = null

        if (lastName.isEmpty()) {
            binding.tilLastName.error = "Required"
            isValid = false
        } else binding.tilLastName.error = null

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Valid email required"
            isValid = false
        } else binding.tilEmail.error = null

        if (institution.isEmpty()) {
            binding.tilInstitution.error = "Required"
            isValid = false
        } else binding.tilInstitution.error = null

        if (password.length < 6) {
            binding.tilPassword.error = "Min 6 characters"
            isValid = false
        } else binding.tilPassword.error = null

        if (password != confirmPassword) {
            binding.tilConfirmPassword.error = "Passwords do not match"
            isValid = false
        } else binding.tilConfirmPassword.error = null

        if (!isValid) return

        setLoading(true)
        Log.d(TAG, "Attempting registration for: $email")

        val userProfile = User(
            email = email,
            firstName = firstName,
            lastName = lastName,
            institution = institution,
            course = course,
            yearOfStudy = year
        )

        lifecycleScope.launch {
            val result = authRepository.register(email, password, userProfile)
            setLoading(false)

            result.fold(
                onSuccess = {
                    Log.d(TAG, "Registration successful for user: ${it.uid}")
                    navigateToMain()
                },
                onFailure = { e ->
                    Log.e(TAG, "Registration failed", e)
                    Toast.makeText(this@RegisterActivity, "Registration Failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finishAffinity()
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnRegister.isEnabled = !isLoading
        // Disable inputs while loading
        binding.etFirstName.isEnabled = !isLoading
        binding.etLastName.isEnabled = !isLoading
        binding.etEmail.isEnabled = !isLoading
        binding.etPassword.isEnabled = !isLoading
        binding.etConfirmPassword.isEnabled = !isLoading
    }
}
