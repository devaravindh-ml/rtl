package com.example.book

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class IntroActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Enable Edge-to-Edge display
        enableEdgeToEdge()
        setContentView(R.layout.activity_intro)

        // 2. Handle System Bars (Padding for Notch/Navigation bar)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.btnStartReading)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // We only care about the bottom inset for this button usually
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, systemBars.bottom)
            insets
        }

        // 3. Find the button and set the Click Listener
        val btnStartReading: AppCompatButton = findViewById(R.id.btnStartReading)

        btnStartReading.setOnClickListener {
            // Create the Intent to move to ReaderActivity
            val intent = Intent(this, ReaderActivity::class.java)
            startActivity(intent)

            // Optional: Call finish() if you don't want the user to go back to Intro
            // finish()
        }
    }
}