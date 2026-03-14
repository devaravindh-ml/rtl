package com.example.book

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Enable Edge-to-Edge display
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // 2. Handle System Bar Padding (prevents UI overlap with status/nav bars)
        val mainLayout = findViewById<View>(R.id.main)
        if (mainLayout != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainLayout) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
        }

        // 3. Navigation Logic for "Continue to Dashboard"
        val btnContinue = findViewById<MaterialButton>(R.id.btnContinue)

        btnContinue.setOnClickListener {
            // Intent to navigate from this screen to the LibraryActivity
            // Ensure you have created LibraryActivity.kt in your project
            val intent = Intent(this, Library::class.java)

            // Start the library screen
            startActivity(intent)

            // Optional: Call finish() if you don't want the user to return to the persona screen
            // finish()
        }
    }
}