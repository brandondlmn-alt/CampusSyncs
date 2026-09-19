package com.campussync.app.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.campussync.app.databinding.ActivitySplashBinding
import com.google.firebase.auth.FirebaseAuth

/**
 * Splash screen that appears on app startup.
 * Checks for authentication state and navigates accordingly.
 */
@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private val TAG = "SplashActivity"
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.d(TAG, "Splash screen started")

        // Wait for 2 seconds then check login status
        Handler(Looper.getMainLooper()).postDelayed({
            checkAuthStatus()
        }, 2000)
    }

    private fun checkAuthStatus() {
        if (auth.currentUser != null) {
            Log.d(TAG, "User is signed in, navigating to MainActivity")
            startActivity(Intent(this, MainActivity::class.java))
        } else {
            Log.d(TAG, "User is not signed in, navigating to LoginActivity")
            startActivity(Intent(this, LoginActivity::class.java))
        }
        finish() // Close splash activity
    }
}
