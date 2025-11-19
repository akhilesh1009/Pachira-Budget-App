package com.pachira.prog7313poepachira

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SplashActivity : AppCompatActivity() {

    private val SplashScreen: Long = 2000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        Handler().postDelayed({
            // Start the main activity
            val mainIntent = Intent(this, MainActivity::class.java)
            startActivity(mainIntent)
            finish() // Close the splash activity
        }, SplashScreen)
    }
}