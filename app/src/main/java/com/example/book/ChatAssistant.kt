package com.example.book

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.book.database.BookChunk
import com.example.book.database.BookDataProcessor
import com.example.book.database.ObjectBox
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class ChatAssistant : AppCompatActivity(), TextToSpeech.OnInitListener {

    private var embeddingGenerator: EmbeddingGenerator? = null
    private var isReady = false

    private lateinit var chatTextView: TextView
    private lateinit var chatScrollView: ScrollView
    private lateinit var tvStatus: TextView
    private lateinit var tts: TextToSpeech

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_chat_assistant)

        chatTextView = findViewById(R.id.chatTextView)
        chatScrollView = findViewById(R.id.chatScrollView)
        tvStatus = findViewById(R.id.tvOnlineStatus)

        val etInput = findViewById<EditText>(R.id.etMessage)
        val btnSend = findViewById<FloatingActionButton>(R.id.btnSendAi)

        tts = TextToSpeech(this, this)

        lifecycleScope.launch {

            try {

                updateStatus("Loading AI...")

                // Load embedding model
                embeddingGenerator =
                    EmbeddingGenerator.create(applicationContext, "embedding.onnx")

                // Build vector database if needed
                initBookDatabase()

                isReady = true
                updateStatus("Ready")

                chatTextView.append("\nSystem: AI ready.\n")

            } catch (e: Exception) {

                updateStatus("Startup Error")

                Toast.makeText(
                    this@ChatAssistant,
                    "Startup failed: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        btnSend.setOnClickListener {

            val question = etInput.text.toString().trim()

            if (question.isEmpty() || !isReady) return@setOnClickListener

            etInput.text.clear()

            performSearch(question)
        }
    }

    private suspend fun initBookDatabase() {

        val bookBox = ObjectBox.store.boxFor(BookChunk::class.java)

        // DEBUG
        println("Chunks in DB: " + bookBox.count())

        if (bookBox.isEmpty) {

            val generator = embeddingGenerator ?: return

            withContext(Dispatchers.IO) {

                updateStatus("Indexing book...")

                val content = assets.open("rtl_book.txt")
                    .bufferedReader()
                    .use { it.readText() }

                BookDataProcessor(this@ChatAssistant, generator)
                    .processBookText(content)
            }
        }
    }

    private fun performSearch(query: String) {

        chatTextView.append("\nUser: $query\n")

        lifecycleScope.launch(Dispatchers.Default) {

            val bookBox = ObjectBox.store.boxFor(BookChunk::class.java)

            val queryEmbedding =
                embeddingGenerator?.generateEmbedding(query)
                    ?: return@launch

            println("Query embedding size: ${queryEmbedding.size}")

            var bestMatch: BookChunk? = null
            var maxScore = -1f

            for (chunk in bookBox.all) {

                val emb = chunk.embedding ?: continue  // ⚠️ no need for ?:

                val score = cosineSimilarity(queryEmbedding, emb)

                if (score > maxScore) {
                    maxScore = score
                    bestMatch = chunk
                }
            }

            val resultText = if (maxScore > 0.15f) {   // ⚠️ lower threshold

                "📖 Book Result:\n\n${bestMatch?.text}"

            } else {

                "❌ No relevant answer found in the book."
            }

            withContext(Dispatchers.Main) {

                chatTextView.append("\n$resultText\n")

                chatScrollView.post {
                    chatScrollView.fullScroll(ScrollView.FOCUS_DOWN)
                }

                tts.speak(resultText, TextToSpeech.QUEUE_FLUSH, null, "AI")
            }
        }
    }

    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {

        var dot = 0f
        var normA = 0f
        var normB = 0f

        for (i in a.indices) {

            dot += a[i] * b[i]

            normA += a[i] * a[i]

            normB += b[i] * b[i]
        }

        if (normA == 0f || normB == 0f) return 0f

        return (dot / (Math.sqrt(normA.toDouble()) *
                Math.sqrt(normB.toDouble()))).toFloat()
    }

    private suspend fun updateStatus(text: String) =
        withContext(Dispatchers.Main) {
            tvStatus.text = text
        }

    override fun onInit(status: Int) {

        if (status == TextToSpeech.SUCCESS) {

            tts.language = Locale.US
        }
    }

    override fun onDestroy() {

        super.onDestroy()

        embeddingGenerator?.close()

        if (::tts.isInitialized) {
            tts.shutdown()
        }
    }
}