package com.example.book

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class Library : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Enable modern Edge-to-Edge display
        enableEdgeToEdge()
        setContentView(R.layout.activity_library)

        // 2. Handle System Bar Padding
        val rootLayout = findViewById<View>(R.id.library_main)
        if (rootLayout != null) {
            ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
        }

        // --- NAVIGATION LOGIC START ---

        // 3. AI Tutor Button
        val btnNavAiTutor = findViewById<LinearLayout>(R.id.btnNavAiTutor)
        btnNavAiTutor.setOnClickListener {
            val intent = Intent(this, ChatAssistant::class.java)
            startActivity(intent)
        }

        // 4. Insights Button
        val btnNavInsight = findViewById<LinearLayout>(R.id.btnNavInsights)
        btnNavInsight.setOnClickListener {
            val intent = Intent(this, Insights::class.java)
            startActivity(intent)
        }

        // --- RESOURCES TAB LOGIC ---

        // 5. Find the RESOURCES TextView by its ID
        val tvTabResources = findViewById<TextView>(R.id.tvTabResources)

        tvTabResources.setOnClickListener {
            // This moves the user to the Intro screen
            val intent = Intent(this, IntroActivity::class.java)
            startActivity(intent)
        }

        // --- MY NOTES TAB LOGIC ---

        // 6. Find the MY NOTES TextView by its ID
        val tvTabMyNotes = findViewById<TextView>(R.id.tvTabMyNotes)

        tvTabMyNotes.setOnClickListener {
            // This moves the user to the My Notes screen
            // NOTE: Replace 'StudyNotesActivity' with the actual name of your Notes Activity class
            val intent = Intent(this, MyNotes::class.java)
            startActivity(intent)
        }

        // --- NAVIGATION LOGIC END ---
    }
}