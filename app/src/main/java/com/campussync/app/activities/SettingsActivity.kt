package com.campussync.app.activities

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.lifecycleScope
import com.campussync.app.data.AuthRepository
import com.campussync.app.databinding.ActivitySettingsBinding
import com.campussync.app.databinding.DialogChangePasswordBinding
import com.campussync.app.models.User
import com.campussync.app.utils.NotificationHelper
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

/**
 * Settings Activity for user profile management, preferences, and security.
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val authRepository = AuthRepository()
    private val TAG = "SettingsActivity"
    private var currentUser: User? = null

    private val languages = listOf(
        Pair("System Default", ""),
        Pair("English", "en"),
        Pair("Afrikaans", "af"),
        Pair("isiZulu", "zu"),
        Pair("isiXhosa", "xh"),
        Pair("isiNdebele", "nr"),
        Pair("Sepedi", "nso"),
        Pair("Sesotho", "st"),
        Pair("Setswana", "tn"),
        Pair("siSwati", "ss"),
        Pair("Tshivenda", "ve"),
        Pair("Xitsonga", "ts")
    )

    private val reminderOptions = listOf("1 Day Before", "2 Days Before", "3 Days Before")
    private var selectedReminderDays = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupLanguageDropdown()
        setupReminderDropdown()
        loadUserProfile()

        binding.btnUpdateProfile.setOnClickListener { updateProfile() }
        binding.btnManageModules.setOnClickListener { startActivity(Intent(this, ModulesActivity::class.java)) }
        binding.btnChangePassword.setOnClickListener { showChangePasswordDialog() }
        binding.btnLogout.setOnClickListener { performLogout() }

        // NEW: Test Notification Button
        binding.btnTestNotification.setOnClickListener {
            if (currentUser?.notificationsEnabled == true) {
                NotificationHelper.showDeadlineNotification(
                    this,
                    "Test Notification 🔔",
                    "This is how your deadline alerts will look! You chose to be reminded $selectedReminderDays day(s) before."
                )
                Toast.makeText(this, "Test notification sent!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Please enable notifications first.", Toast.LENGTH_SHORT).show()
            }
        }

        // Check if the user has blocked notifications at the system level
        checkNotificationPermission()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun setupLanguageDropdown() {
        val languageNames = languages.map { it.first }
        val adapter = NoFilterArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, languageNames)
        binding.actvLanguage.setAdapter(adapter)

        val currentLocale = AppCompatDelegate.getApplicationLocales()
        val currentLocaleCode = if (currentLocale.isEmpty) "" else currentLocale[0]?.language

        val currentLangName = languages.find { it.second == currentLocaleCode }?.first ?: "System Default"
        binding.actvLanguage.setText(currentLangName, false)

        binding.actvLanguage.setOnItemClickListener { _, _, position, _ ->
            val selectedLanguageCode = languages[position].second
            setAppLanguage(selectedLanguageCode)
        }
    }

    private fun setupReminderDropdown() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, reminderOptions)
        binding.actvReminderDays.setAdapter(adapter)
        binding.actvReminderDays.setText(reminderOptions[0], false)

        binding.actvReminderDays.setOnItemClickListener { _, _, position, _ ->
            selectedReminderDays = position + 1
        }
    }

    private fun setAppLanguage(languageCode: String) {
        Toast.makeText(this, "Changing language...", Toast.LENGTH_SHORT).show()

        val appLocale: LocaleListCompat = if (languageCode.isEmpty()) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(languageCode)
        }

        AppCompatDelegate.setApplicationLocales(appLocale)
    }

    /**
     * Checks if the user has blocked notifications at the system level.
     */
    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                AlertDialog.Builder(this)
                    .setTitle("Notifications Disabled")
                    .setMessage("To receive deadline alerts, you need to enable notifications in your phone's settings.")
                    .setPositiveButton("Open Settings") { _, _ ->
                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        intent.putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                        startActivity(intent)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
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
                }
            )
        }
    }

    private fun populateFields(user: User) {
        binding.etFirstName.setText(user.firstName)
        binding.etLastName.setText(user.lastName)
        binding.etInstitution.setText(user.campusLocation)
        binding.etCourse.setText(user.course)
        binding.etYear.setText(user.yearOfStudy.toString())
        binding.switchNotifications.isChecked = user.notificationsEnabled

        selectedReminderDays = user.notificationDaysBefore
        val displayString = when (selectedReminderDays) {
            1 -> reminderOptions[0]
            2 -> reminderOptions[1]
            3 -> reminderOptions[2]
            else -> reminderOptions[0]
        }
        binding.actvReminderDays.setText(displayString, false)
    }

    private fun updateProfile() {
        val firstName = binding.etFirstName.text.toString().trim()
        val lastName = binding.etLastName.text.toString().trim()
        val institution = binding.etInstitution.text.toString().trim()
        val course = binding.etCourse.text.toString().trim()
        val year = binding.etYear.text.toString().trim()

        val currentLocale = AppCompatDelegate.getApplicationLocales()
        val currentLocaleCode = if (currentLocale.isEmpty) "en" else currentLocale[0]?.language ?: "en"

        val notificationsEnabled = binding.switchNotifications.isChecked

        if (firstName.isEmpty() || lastName.isEmpty()) {
            binding.tilFirstName.error = "Required"
            binding.tilLastName.error = "Required"
            return
        }

        val updatedUser = currentUser?.copy(
            firstName = firstName,
            lastName = lastName,
            campusLocation = institution,
            course = course,
            yearOfStudy = year.toIntOrNull() ?: 0,
            language = currentLocaleCode,
            notificationsEnabled = notificationsEnabled,
            notificationDaysBefore = selectedReminderDays,
            updatedAt = System.currentTimeMillis()
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
        binding.btnTestNotification.isEnabled = !isLoading
    }
}

class NoFilterArrayAdapter(context: Context, resource: Int, private val items: List<String>) :
    ArrayAdapter<String>(context, resource, items) {
    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val results = FilterResults()
                results.values = items
                results.count = items.size
                return results
            }
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                if (results != null && results.count > 0) {
                    notifyDataSetChanged()
                }
            }
        }
    }
}