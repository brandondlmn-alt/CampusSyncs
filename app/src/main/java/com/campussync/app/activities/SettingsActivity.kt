package com.campussync.app.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.campussync.app.R
import com.campussync.app.data.AuthRepository
import com.campussync.app.databinding.ActivitySettingsBinding
import com.campussync.app.databinding.DialogChangePasswordBinding
import com.campussync.app.models.User
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

/**
 * Settings Activity for user profile management, preferences, and security.
 * Allows users to update their profile information, change their password, and toggle preferences.
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val authRepository = AuthRepository()
    private val TAG = "SettingsActivity"
    private var currentUser: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupLanguageDropdown()
        loadUserProfile()

        binding.btnUpdateProfile.setOnClickListener {
            updateProfile()
        }

        binding.btnManageModules.setOnClickListener {
            startActivity(Intent(this, ModulesActivity::class.java))
        }

        binding.btnChangePassword.setOnClickListener {
            showChangePasswordDialog()
        }

        binding.btnLogout.setOnClickListener {
            performLogout()
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupLanguageDropdown() {
        val languages = arrayOf("English", "isiZulu", "Afrikaans", "Sesotho", "isiXhosa")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, languages)
        binding.actvLanguage.setAdapter(adapter)
    }

    private fun loadUserProfile() {
        setLoading(true)
        lifecycleScope.launch {
            val result = authRepository.getCurrentUserProfile()
            setLoading(false)
            result.fold(
                onSuccess = { user ->
                    currentUser = user
                    user?.let { populateFields(it) }
                },
                onFailure = { e ->
                    Log.e(TAG, "Failed to load profile", e)
                    Toast.makeText(this@SettingsActivity, "Error loading profile", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    private fun populateFields(user: User) {
        binding.etFirstName.setText(user.firstName)
        binding.etLastName.setText(user.lastName)
        binding.etInstitution.setText(user.institution)
        binding.etCourse.setText(user.course)
        binding.etYear.setText(user.yearOfStudy)
        binding.actvLanguage.setText(user.language, false)
        binding.switchNotifications.isChecked = user.notificationsEnabled
    }

    private fun updateProfile() {
        val firstName = binding.etFirstName.text.toString().trim()
        val lastName = binding.etLastName.text.toString().trim()
        val institution = binding.etInstitution.text.toString().trim()
        val course = binding.etCourse.text.toString().trim()
        val year = binding.etYear.text.toString().trim()
        val language = binding.actvLanguage.text.toString()
        val notificationsEnabled = binding.switchNotifications.isChecked

        if (firstName.isEmpty() || lastName.isEmpty()) {
            binding.tilFirstName.error = "Required"
            binding.tilLastName.error = "Required"
            return
        }

        val updatedUser = currentUser?.copy(
            firstName = firstName,
            lastName = lastName,
            institution = institution,
            course = course,
            yearOfStudy = year,
            language = language,
            notificationsEnabled = notificationsEnabled
        ) ?: return

        setLoading(true)
        lifecycleScope.launch {
            val result = authRepository.updateUserProfile(updatedUser)
            setLoading(false)
            result.fold(
                onSuccess = {
                    currentUser = updatedUser
                    Snackbar.make(binding.root, "Profile updated successfully", Snackbar.LENGTH_SHORT).show()
                },
                onFailure = { e ->
                    Log.e(TAG, "Update failed", e)
                    Toast.makeText(this@SettingsActivity, "Update failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    private fun showChangePasswordDialog() {
        val dialogBinding = DialogChangePasswordBinding.inflate(LayoutInflater.from(this))
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setPositiveButton("Change", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            val button = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            button.setOnClickListener {
                val currentPw = dialogBinding.etCurrentPassword.text.toString()
                val newPw = dialogBinding.etNewPassword.text.toString()
                val confirmPw = dialogBinding.etConfirmNewPassword.text.toString()

                if (currentPw.isEmpty()) {
                    dialogBinding.tilCurrentPassword.error = "Required"
                    return@setOnClickListener
                }
                if (newPw.length < 6) {
                    dialogBinding.tilNewPassword.error = "Min 6 characters"
                    return@setOnClickListener
                }
                if (newPw != confirmPw) {
                    dialogBinding.tilConfirmNewPassword.error = "Passwords do not match"
                    return@setOnClickListener
                }

                setLoading(true)
                lifecycleScope.launch {
                    val result = authRepository.changePassword(currentPw, newPw)
                    setLoading(false)
                    result.fold(
                        onSuccess = {
                            dialog.dismiss()
                            Snackbar.make(binding.root, "Password changed successfully", Snackbar.LENGTH_SHORT).show()
                        },
                        onFailure = { e ->
                            Log.e(TAG, "Password change failed", e)
                            Toast.makeText(this@SettingsActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
        dialog.show()
    }

    private fun performLogout() {
        authRepository.logout()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnUpdateProfile.isEnabled = !isLoading
        binding.btnManageModules.isEnabled = !isLoading
        binding.btnChangePassword.isEnabled = !isLoading
        binding.btnLogout.isEnabled = !isLoading
    }
}
