package com.pachira.prog7313poepachira

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout

class MoreActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_more)

        setupBottomNavigation()
        setupClickListeners()
    }

    private fun setupClickListeners() {
        // Profile click listener
        val profileLayout = findViewById<LinearLayout>(R.id.llProfile)
        profileLayout.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }

        // Wallets click listener
        val badgesLayout = findViewById<LinearLayout>(R.id.llBadges)
        badgesLayout.setOnClickListener {
            // Navigate to BadgesActivity if you have one
            val intent = Intent(this, BadgesActivity::class.java)
            startActivity(intent)
        }
    }
}