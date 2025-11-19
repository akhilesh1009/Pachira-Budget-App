package com.pachira.prog7313poepachira

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth

class ForgotPasswordActivity : AppCompatActivity() {

    // UI components
    private lateinit var etEmail: EditText
    private lateinit var btnRestPassword: Button
    private lateinit var tvBackToLogin: TextView

    // Firebase Authentication reference
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enables full-screen layout and proper insets handling
        setContentView(R.layout.activity_forgot_password)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Bind UI elements
        etEmail = findViewById(R.id.etEmail)
        btnRestPassword = findViewById(R.id.btnRestPassword)
        tvBackToLogin = findViewById(R.id.tvBackToLogin)

        // Set button click listener
        btnRestPassword.setOnClickListener {
            resetPassword()
        }

        // Set up clickable "Back to Login" text
        returnLogin()
    }

    /**
     * Sends a password reset email using Firebase Authentication
     * Reference: Android Knowledge (2024) YouTube tutorial
     */
    private fun resetPassword() {
        val email = etEmail.text.toString().trim()

        if (email.isEmpty()) {
            etEmail.error = "Email is required"
            etEmail.requestFocus()
            return
        }

        // Send password reset email
        auth.sendPasswordResetEmail(email).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Password reset email sent. Check your inbox!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Failed to send reset email: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Creates a clickable "Back to Login" text that redirects to the LoginActivity
     */
    private fun returnLogin() {
        val fullText = "Back to Login"
        val spannableString = SpannableString(fullText)

        // Determine the clickable span range for the word "Login"
        val loginStart = fullText.indexOf("Login")
        val loginEnd = loginStart + "Login".length

        // Create clickable span for "Login"
        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                startActivity(Intent(this@ForgotPasswordActivity, LoginActivity::class.java))
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.color = ContextCompat.getColor(this@ForgotPasswordActivity, R.color.purple) // Use custom purple color
                ds.isUnderlineText = true // Optional: underline text
            }
        }

        // Apply the clickable span
        spannableString.setSpan(clickableSpan, loginStart, loginEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        tvBackToLogin.text = spannableString
        tvBackToLogin.movementMethod = LinkMovementMethod.getInstance()
        tvBackToLogin.highlightColor = Color.TRANSPARENT // Remove highlight on tap
    }
}
