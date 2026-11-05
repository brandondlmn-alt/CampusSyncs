package com.campussync.app.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.lifecycleScope
import com.campussync.app.data.AuthRepository
import com.campussync.app.databinding.ActivityLoginBinding
import com.campussync.app.models.User
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch

/**
 * Activity that handles user login and authentication via Firebase.
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val authRepository = AuthRepository()
    private val TAG = "LoginActivity"

    private val webClientId = "37829392477-2ok9e5qi8cf57etld53a7l0c369ab45h.apps.googleusercontent.com"
    private lateinit var credentialManager: CredentialManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.d(TAG, "Login screen launched")
        credentialManager = CredentialManager.create(this)

        binding.btnLogin.setOnClickListener {
            attemptLogin()
        }

        binding.btnGoogleLogin.setOnClickListener {
            signInWithGoogle()
        }

        binding.tvGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        binding.tvForgotPassword.setOnClickListener {
            handleForgotPassword()
        }
    }

    /**
     * Initiates the Google Sign-In process.
     */
    private fun signInWithGoogle() {
        setLoading(true)
        executeGoogleSignIn(filterAuthorized = true)
    }

    /**
     * Executes the Google Sign-In request using Credential Manager.
     */
    private fun executeGoogleSignIn(filterAuthorized: Boolean) {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(filterAuthorized)
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        lifecycleScope.launch {
            try {
                val result = credentialManager.getCredential(
                    context = this@LoginActivity,
                    request = request,
                )
                handleSignIn(result)
            } catch (e: NoCredentialException) {
                if (filterAuthorized) {
                    Log.w(TAG, "No authorized accounts found, retrying with all accounts.")
                    executeGoogleSignIn(filterAuthorized = false)
                } else {
                    setLoading(false)
                    Log.e(TAG, "No Google accounts found on device.", e)
                    Toast.makeText(this@LoginActivity, "No Google accounts found.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                setLoading(false)
                Log.e(TAG, "Google Sign-In failed", e)
                Toast.makeText(this@LoginActivity, "Sign-In failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Extracts the ID token from the credential response and authenticates with Firebase.
     */
    private fun handleSignIn(result: GetCredentialResponse) {
        val credential = result.credential

        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            try {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                firebaseAuthWithGoogle(idToken)
            } catch (e: GoogleIdTokenParsingException) {
                setLoading(false)
                Log.e(TAG, "Received an invalid google id token response", e)
                Toast.makeText(this, "Invalid Google token", Toast.LENGTH_SHORT).show()
            }
        } else {
            setLoading(false)
            Log.e(TAG, "Unexpected type of credential")
        }
    }

    /**
     * Authenticates the user with Firebase using the Google ID token.
     */
    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)

        lifecycleScope.launch {
            val result = authRepository.signInWithGoogle(credential)
            setLoading(false)

            result.fold(
                onSuccess = { user ->
                    Log.d(TAG, "Google Login successful for user: ${user?.uid}")
                    checkUserProfileAndNavigate(user)
                },
                onFailure = { e ->
                    Log.e(TAG, "Firebase Google Login failed", e)
                    Toast.makeText(this@LoginActivity, "Firebase Login Failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    /**
     * Attempts a standard email and password login.
     */
    private fun attemptLogin() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        if (email.isEmpty()) {
            binding.tilEmail.error = "Email is required"
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Invalid email format"
            return
        }
        binding.tilEmail.error = null

        if (password.isEmpty()) {
            binding.tilPassword.error = "Password is required"
            return
        }
        binding.tilPassword.error = null

        setLoading(true)
        Log.d(TAG, "Attempting login for: $email")

        lifecycleScope.launch {
            val result = authRepository.login(email, password)
            setLoading(false)

            result.fold(
                onSuccess = { user ->
                    Log.d(TAG, "Login successful for user: ${user?.uid}")
                    checkUserProfileAndNavigate(user)
                },
                onFailure = { e ->
                    Log.e(TAG, "Login failed", e)
                    Toast.makeText(this@LoginActivity, "Login Failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    /**
     * Determines the correct navigation destination based on profile completeness.
     */
    private fun checkUserProfileAndNavigate(user: User?) {
        if (user != null && user.course.isNotEmpty() && user.yearOfStudy > 0) {
            navigateToMain()
        } else {
            Toast.makeText(this, "Please complete your academic profile.", Toast.LENGTH_LONG).show()
            val intent = Intent(this, RegisterActivity::class.java)
            intent.putExtra("IS_SSO_SETUP", true)
            startActivity(intent)
            finish()
        }
    }

    /**
     * Handles the forgotten password flow.
     */
    private fun handleForgotPassword() {
        val email = binding.etEmail.text.toString().trim()
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Enter a valid email to reset password"
            return
        }
        binding.tilEmail.error = null

        lifecycleScope.launch {
            val result = authRepository.resetPassword(email)
            result.fold(
                onSuccess = {
                    Toast.makeText(this@LoginActivity, "Password reset email sent", Toast.LENGTH_SHORT).show()
                },
                onFailure = { e ->
                    Toast.makeText(this@LoginActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    /**
     * Navigates the user to the main dashboard.
     */
    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    /**
     * Toggles the loading state of the UI components.
     */
    private fun setLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !isLoading
        binding.etEmail.isEnabled = !isLoading
        binding.etPassword.isEnabled = !isLoading
        binding.btnGoogleLogin.isEnabled = !isLoading
    }
}