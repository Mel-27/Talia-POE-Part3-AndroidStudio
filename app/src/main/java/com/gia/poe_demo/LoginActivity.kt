package com.gia.poe_demo

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class LoginActivity : AppCompatActivity() {

    private lateinit var viewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        viewModel = ViewModelProvider(this)[AuthViewModel::class.java]

        val etEmail    = findViewById<TextInputEditText>(R.id.etUsername) // your field is labelled etUsername but takes email
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val tilEmail   = findViewById<TextInputLayout>(R.id.tilUsername)
        val tilPassword = findViewById<TextInputLayout>(R.id.tilPassword)
        val btnLogin   = findViewById<android.view.View>(R.id.btnLogin)

        // Navigate to register
        findViewById<android.view.View>(R.id.tabSignUp).setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
        findViewById<android.view.View>(R.id.tvRegister).setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // Observe Firebase login result
        // ref: https://developer.android.com/topic/libraries/architecture/livedata
        viewModel.loginState.observe(this) { state ->
            when (state) {
                is AuthViewModel.AuthState.Loading -> {
                    btnLogin.isEnabled = false
                }
                is AuthViewModel.AuthState.Success -> {
                    getSharedPreferences("BudgetBeePrefs", MODE_PRIVATE).edit()
                        .putBoolean("isRegistered", true)
                        .putString("loggedInEmail", state.user?.email ?: "")
                        .putString("loggedInFullName", state.fullName)
                        .putString("firebaseUid", state.user?.uid)
                        .apply()

                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
                is AuthViewModel.AuthState.Error -> {
                    btnLogin.isEnabled = true
                    tilEmail.error    = state.message
                    tilPassword.error = state.message
                }
            }
        }

        btnLogin.setOnClickListener {
            val email    = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            // Reset errors
            tilEmail.error    = null
            tilPassword.error = null

            var isValid = true

            if (email.isEmpty()) {
                tilEmail.error = "Required"; isValid = false
            } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                tilEmail.error = "Enter a valid email address"; isValid = false
            }

            if (password.isEmpty()) {
                tilPassword.error = "Required"; isValid = false
            } else if (password.length < 8) {
                tilPassword.error = "Password must be at least 8 characters"; isValid = false
            }

            // Only call Firebase if all validation passed
            if (isValid) {
                viewModel.login(email, password)
            }
        }
    }
}