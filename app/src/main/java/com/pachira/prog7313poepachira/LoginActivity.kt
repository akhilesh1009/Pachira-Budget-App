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
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    // Declare UI elements and Firebase authentication
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var tvRegister: TextView
    private lateinit var btnGoogle: ImageView
    private lateinit var btnFacebook: ImageView
    private lateinit var btnApple: ImageView
    private lateinit var auth: FirebaseAuth
    private lateinit var cbRememberMe: CheckBox
    private lateinit var tvForgotPassword: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Handle system window insets for fullscreen layout
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize Firebase Auth instance
        auth = FirebaseAuth.getInstance()

        // Link UI elements
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvRegister = findViewById(R.id.tvRegister)
        btnGoogle = findViewById(R.id.btnGoogle)
        btnFacebook = findViewById(R.id.btnFacebook)
        btnApple = findViewById(R.id.btnApple)
        cbRememberMe = findViewById(R.id.cbRememberMe)
        tvForgotPassword = findViewById(R.id.tvForgotPassword)

        // Navigate to ForgotPasswordActivity
        tvForgotPassword.setOnClickListener {
            val intent = Intent(this, ForgotPasswordActivity::class.java)
            startActivity(intent)
        }

        // Check if activity was opened from registration screen
        val fromRegistration = intent.getBooleanExtra("FROM_REGISTRATION", false)

        if (fromRegistration) {
            // Clear saved credentials if coming from registration
            val sharedPrefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE)
            sharedPrefs.edit().clear().apply()
            cbRememberMe.isChecked = false
        } else {
            // Load saved credentials if available
            val sharedPrefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE)
            val savedEmail = sharedPrefs.getString("email", "")
            val savedPassword = sharedPrefs.getString("password", "")
            val rememberMeState = sharedPrefs.getBoolean("rememberMe", false)

            if (!savedEmail.isNullOrEmpty() && !savedPassword.isNullOrEmpty() && rememberMeState) {
                etEmail.setText(savedEmail)
                etPassword.setText(savedPassword)
                cbRememberMe.isChecked = true
            } else {
                cbRememberMe.isChecked = false
            }
        }

        // Handle login button click
        btnLogin.setOnClickListener {
            loginUser()
        }

        // Placeholder listeners for future social login integrations
        btnGoogle.setOnClickListener {
            Toast.makeText(this, "Google sign-in clicked", Toast.LENGTH_SHORT).show()
        }

        btnFacebook.setOnClickListener {
            Toast.makeText(this, "Facebook sign-in clicked", Toast.LENGTH_SHORT).show()
        }

        btnApple.setOnClickListener {
            Toast.makeText(this, "Apple sign-in clicked", Toast.LENGTH_SHORT).show()
        }

        // Setup clickable "Sign Up" text
        setRegTextView()
    }

    // Handle user login logic with Firebase Authentication
    private fun loginUser() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        // Validate input
        if (email.isEmpty()) {
            etEmail.error = "Email is required"
            etEmail.requestFocus()
            return
        }

        if (password.isEmpty()) {
            etPassword.error = "Password is required"
            etPassword.requestFocus()
            return
        }

        // Show progress feedback
        btnLogin.isEnabled = false
        btnLogin.text = "Logging in..."

        // Attempt login with Firebase
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                // Restore button state
                btnLogin.isEnabled = true
                btnLogin.text = getString(R.string.Login)

                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        // Save login info if "Remember Me" is checked
                        val sharedPrefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE)
                        val editor = sharedPrefs.edit()
                        if (cbRememberMe.isChecked) {
                            editor.putString("email", email)
                            editor.putString("password", password)
                            editor.putBoolean("rememberMe", true)
                        } else {
                            editor.clear()
                        }
                        editor.apply()

                        // Proceed to dashboard
                        Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, DashboardActivity::class.java))
                        finish()
                    } else {
                        // Unexpected error - user not found
                        Toast.makeText(this, "Login error: User not found", Toast.LENGTH_LONG).show()
                        auth.signOut()
                    }
                } else {
                    // Login failed
                    Toast.makeText(this, "Authentication failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    // Set up clickable "Sign Up" portion in TextView
    private fun setRegTextView() {
        val fullText = "Don't have an Account? Sign Up"
        val spannableString = SpannableString(fullText)

        val registerStart = fullText.indexOf("Sign Up")
        val registerEnd = registerStart + "Sign Up".length

        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                // Navigate to registration screen
                startActivity(Intent(this@LoginActivity, RegisterActivity::class.java))
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.color = ContextCompat.getColor(this@LoginActivity, R.color.purple)
                ds.isUnderlineText = true
            }
        }

        spannableString.setSpan(clickableSpan, registerStart, registerEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        tvRegister.text = spannableString
        tvRegister.movementMethod = LinkMovementMethod.getInstance()
        tvRegister.highlightColor = Color.TRANSPARENT
    }
}
