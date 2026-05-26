package com.gia.poe_demo

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class RegisterActivity : AppCompatActivity() {

    private lateinit var viewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        viewModel = ViewModelProvider(this)[AuthViewModel::class.java]

        val etFullName        = findViewById<TextInputEditText>(R.id.etFullName)
        val etEmail           = findViewById<TextInputEditText>(R.id.etEmail)
        val etUsername        = findViewById<TextInputEditText>(R.id.etUsername)
        val etPassword        = findViewById<TextInputEditText>(R.id.etPassword)
        val etConfirmPassword = findViewById<TextInputEditText>(R.id.etConfirmPassword)
        val tilFullName       = findViewById<TextInputLayout>(R.id.tilFullName)
        val tilEmail          = findViewById<TextInputLayout>(R.id.tilEmail)
        val tilUsername       = findViewById<TextInputLayout>(R.id.tilUsername)
        val tilPassword       = findViewById<TextInputLayout>(R.id.tilPassword)
        val tilConfirmPassword = findViewById<TextInputLayout>(R.id.tilConfirmPassword)
        val btnRegister       = findViewById<android.view.View>(R.id.btnRegister)

        // Navigate to login
        findViewById<android.view.View>(R.id.tabLogin).setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
        findViewById<android.view.View>(R.id.tvLogin).setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        // Observe Firebase registration result
        // ref: https://developer.android.com/topic/libraries/architecture/livedata
        viewModel.registerState.observe(this) { state ->
            when (state) {
                is AuthViewModel.AuthState.Loading -> {
                    btnRegister.isEnabled = false
                }
                is AuthViewModel.AuthState.Success -> {
                    // Save to SharedPreferences (existing)
                    getSharedPreferences("BudgetBeePrefs", MODE_PRIVATE).edit()
                        .putBoolean("isRegistered", true)
                        .putString("loggedInUsername", state.username)
                        .putString("loggedInFullName", state.fullName)
                        .putString("loggedInEmail", state.user?.email ?: "")
                        .putString("firebaseUid", state.user?.uid)
                        .apply()

                    // ============================================================
                    // Save user session to SessionManager for cloud sync
                    // ============================================================
                    val sessionManager = SessionManager(this@RegisterActivity)
                    sessionManager.saveUserSession(
                        userId = state.user?.uid ?: "",
                        username = state.username,
                        email = state.user?.email ?: "",
                        fullName = state.fullName
                    )
                    android.util.Log.d("RegisterActivity", "User session saved: ${state.username} (${state.user?.uid})")

                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
                is AuthViewModel.AuthState.Error -> {
                    btnRegister.isEnabled = true
                    tilEmail.error = state.message
                }
            }
        }

        btnRegister.setOnClickListener {
            val fullName        = etFullName.text.toString().trim()
            val email           = etEmail.text.toString().trim()
            val username        = etUsername.text.toString().trim()
            val password        = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()

            // Reset all errors
            tilFullName.error       = null
            tilEmail.error          = null
            tilUsername.error       = null
            tilPassword.error       = null
            tilConfirmPassword.error = null

            var isValid = true

            if (fullName.isEmpty()) {
                tilFullName.error = "Required"; isValid = false
            } else if (!fullName.matches(Regex("^[a-zA-Z ]+$"))) {
                tilFullName.error = "Full name must contain letters only"; isValid = false
            }

            if (email.isEmpty()) {
                tilEmail.error = "Required"; isValid = false
            } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                tilEmail.error = "Enter a valid email address"; isValid = false
            }

            if (username.isEmpty()) {
                tilUsername.error = "Required"; isValid = false
            }

            if (password.isEmpty()) {
                tilPassword.error = "Required"; isValid = false
            } else if (password.length < 8) {
                tilPassword.error = "Password must be at least 8 characters"; isValid = false
            } else if (!password.any { it.isUpperCase() }) {
                tilPassword.error = "Must contain an uppercase letter"; isValid = false
            } else if (!password.any { it.isDigit() }) {
                tilPassword.error = "Must contain a number"; isValid = false
            } else if (!password.any { !it.isLetterOrDigit() }) {
                tilPassword.error = "Must contain a special character"; isValid = false
            }

            if (confirmPassword.isEmpty()) {
                tilConfirmPassword.error = "Required"; isValid = false
            } else if (password != confirmPassword) {
                tilConfirmPassword.error = "Passwords do not match"; isValid = false
            }

            // Only call Firebase if all validation passed
            if (isValid) {
                viewModel.register(email, password, fullName, username)
            }
        }
    }
}