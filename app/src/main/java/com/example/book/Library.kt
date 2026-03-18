package com.example.book

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.tabs.TabLayout

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

        // --- TAB SELECTION LOGIC ---
        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)

        // Set the listener to handle clicks on the Tabs
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    1 -> { // RESOURCES tab index
                        val intent = Intent(this@Library, ReaderActivity::class.java)
                        startActivity(intent)
                    }
                    2 -> { // MY NOTES tab index
                        val intent = Intent(this@Library, MyNotes::class.java)
                        startActivity(intent)
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {
                // Allows navigation even if the tab is already "active"
                when (tab?.position) {
                    1 -> startActivity(Intent(this@Library, ReaderActivity::class.java))
                    2 -> startActivity(Intent(this@Library, MyNotes::class.java))
                }
            }
        })

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

        // --- NAVIGATION LOGIC END ---
    }
}