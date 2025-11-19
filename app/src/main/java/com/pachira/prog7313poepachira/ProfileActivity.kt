package com.pachira.prog7313poepachira

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class ProfileActivity : BaseActivity() {

    // Firebase references
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    // UI components
    private lateinit var tvEmail: TextView
    private lateinit var tvMemberSince: TextView
    private lateinit var btnLogout: Button
    private lateinit var btnDeleteAccount: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Initialize Firebase authentication and database
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        // Redirect to login if user is not authenticated
        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // Bind UI elements
        tvEmail = findViewById(R.id.tvEmail)
        tvMemberSince = findViewById(R.id.tvMemberSince)
        btnLogout = findViewById(R.id.btnLogout)
        btnDeleteAccount = findViewById(R.id.btnDeleteAccount)

        // Set up click listeners
        btnLogout.setOnClickListener {
            showLogoutConfirmationDialog()
        }

        btnDeleteAccount.setOnClickListener {
            showDeleteAccountConfirmationDialog()
        }

        // Load user details from Firebase
        loadUserData()

        // Set up bottom navigation bar inherited from BaseActivity
        setupBottomNavigation()
    }

    /**
     * Load and display current user's email and account creation date
     */
    private fun loadUserData() {
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid

        // Set email directly from FirebaseAuth
        tvEmail.text = currentUser.email

        // Fetch account creation timestamp from Realtime Database
        database.child("users").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val createdAt = snapshot.child("createdAt").getValue(Long::class.java) ?: 0L

                    if (createdAt > 0) {
                        val formattedDate = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
                            .format(Date(createdAt))
                        tvMemberSince.text = "Member since: $formattedDate"
                    } else {
                        tvMemberSince.visibility = View.GONE
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    tvMemberSince.visibility = View.GONE
                }
            })
    }

    /**
     * Show a confirmation dialog for logging out
     */
    private fun showLogoutConfirmationDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.custom_logout_dialog)
        dialog.window?.setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.logout_dialog))

        // Set dialog dimensions and background dim
        val displayMetrics = resources.displayMetrics
        val width = (displayMetrics.widthPixels * 0.85).toInt()
        dialog.window?.setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT)
        dialog.window?.setDimAmount(0.9f)

        // Handle button clicks
        dialog.findViewById<Button>(R.id.btn_yes).setOnClickListener {
            auth.signOut()
            Toast.makeText(this, "Successfully logged out", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            dialog.dismiss()
        }

        dialog.findViewById<Button>(R.id.btn_no).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    /**
     * Show a confirmation dialog before deleting the account
     */
    private fun showDeleteAccountConfirmationDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.custom_delete_profile)
        dialog.window?.setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.logout_dialog))

        val displayMetrics = resources.displayMetrics
        val width = (displayMetrics.widthPixels * 0.85).toInt()
        dialog.window?.setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT)
        dialog.window?.setDimAmount(0.9f)

        dialog.findViewById<Button>(R.id.btn_yes).setOnClickListener {
            deleteAccount()
        }

        dialog.findViewById<Button>(R.id.btn_no).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    /**
     * Deletes both user authentication and their data from Firebase
     */
    private fun deleteAccount() {
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid

        // Remove user node from Realtime Database
        database.child("users").child(userId).removeValue()
            .addOnSuccessListener {
                // Remove user from Firebase Authentication
                currentUser.delete()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Account deleted successfully", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, LoginActivity::class.java))
                        finishAffinity() // Clear back stack
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to delete account: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to delete account data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
