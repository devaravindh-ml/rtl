package com.example.book

import android.animation.ObjectAnimator
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.animation.AlphaAnimation
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import com.example.book.database.BookChunk
import com.example.book.database.BookChunk_
import com.example.book.database.BookDataProcessor
import com.example.book.database.ObjectBox
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import com.google.android.material.bottomsheet.BottomSheetDialog

class ChatAssistant : AppCompatActivity(), TextToSpeech.OnInitListener {

    private var embeddingGenerator: EmbeddingGenerator? = null
    private var isReady = false
    private val SPEECH_REQUEST_CODE = 123

    private lateinit var chatContainer: LinearLayout
    private lateinit var chatScrollView: androidx.core.widget.NestedScrollView
    private lateinit var tvStatus: TextView
    private lateinit var tts: TextToSpeech
    private lateinit var loadingOverlay: CardView
    private lateinit var tvProgressStatus: TextView
    private lateinit var etMessage: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_chat_assistant)

        chatContainer = findViewById(R.id.chatContainer)
        chatScrollView = findViewById(R.id.chatScrollView)
        tvStatus = findViewById(R.id.tvOnlineStatus)
        loadingOverlay = findViewById(R.id.loadingOverlay)
        tvProgressStatus = findViewById(R.id.tvProgressStatus)
        etMessage = findViewById(R.id.etMessage)

        val btnSend = findViewById<FloatingActionButton>(R.id.btnSendAi)
        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val btnVoice = findViewById<ImageButton>(R.id.btnVoiceInput)
        val chipUniversal = findViewById<TextView>(R.id.chipUniversal)
        val chipSystems = findViewById<TextView>(R.id.chipSystems)

        tts = TextToSpeech(this, this)

        btnBack.setOnClickListener { finish() }
        btnVoice.setOnClickListener { startVoiceInput() }

        chipUniversal.setOnClickListener { sendQuickQuery("Explain Universal Values") }
        chipSystems.setOnClickListener { sendQuickQuery("How to apply Systems Change") }

        lifecycleScope.launch {
            try {
                updateStatus("● LOADING AI")
                embeddingGenerator = EmbeddingGenerator.create(applicationContext, "embedding.onnx")

                val bookBox = ObjectBox.store.boxFor(BookChunk::class.java)
                if (bookBox.count() <= 1L) {
                    val generator = embeddingGenerator
                    if (generator != null) {
                        withContext(Dispatchers.Main) { loadingOverlay.visibility = View.VISIBLE }
                        withContext(Dispatchers.IO) {
                            bookBox.removeAll()
                            val content = assets.open("rtl_book.txt").bufferedReader().use { it.readText() }
                            BookDataProcessor(generator).processBookText(content)
                        }
                        withContext(Dispatchers.Main) { loadingOverlay.visibility = View.GONE }
                    }
                }

                isReady = true
                updateStatus("● READY")
                addChatBubble("AI is ready. Ask me anything about the book!", false)
            } catch (e: Exception) {
                updateStatus("● ERROR")
                Toast.makeText(this@ChatAssistant, "Startup failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        btnSend.setOnClickListener {
            val question = etMessage.text.toString().trim()
            if (question.isNotEmpty() && isReady) {
                etMessage.text.clear()
                performSearch(question)
            }
        }
    }

    private fun addChatBubble(text: String, isUser: Boolean, chunk: BookChunk? = null) {
        val horizontalContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = if (isUser) android.view.Gravity.END else android.view.Gravity.START
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 12, 0, 12)
            }
        }

        // 1. LEFT SIDE: Custom Book Icon (Only for AI)
        if (!isUser) {
            val bookIconContainer = android.widget.FrameLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    (40 * resources.displayMetrics.density).toInt(),
                    (40 * resources.displayMetrics.density).toInt()
                ).apply {
                    setMargins(0, 0, 8, 0)
                    gravity = android.view.Gravity.TOP
                }
                background = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.OVAL
                    setColor(android.graphics.Color.WHITE)
                    setStroke(1, android.graphics.Color.parseColor("#E2E8F0"))
                }
                elevation = 2f
            }
            val iconView = ImageView(this).apply {
                setImageResource(R.drawable.ic_custom_book)
                setColorFilter(android.graphics.Color.parseColor("#3B82F6"))
                setPadding(16, 16, 16, 16)
            }
            bookIconContainer.addView(iconView)
            horizontalContainer.addView(bookIconContainer)
        }

        // 2. MIDDLE: Bubble Wrapper (Weight 1f makes room for the TTS button on the right)
        val bubbleWrapper = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )

            if (!isUser) {
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(android.graphics.Color.parseColor("#F0F9FF")) // Light Blue Background
                    cornerRadius = 24f
                }
                setPadding(8, 8, 8, 8)
            }
        }

        val textView = TextView(this).apply {
            this.setText(text)
            this.setPadding(40, 32, 40, 32)
            this.textSize = 15f
            this.setTextColor(if (isUser) android.graphics.Color.WHITE else android.graphics.Color.parseColor("#1E293B"))
            this.setBackgroundResource(if (isUser) R.drawable.bg_bubble_user else R.drawable.bg_bubble_ai)
            this.elevation = 2f

            // --- RESTORED LOGIC: Double Tap to Define Word ---
            val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
                override fun onDoubleTap(e: MotionEvent): Boolean {
                    // Flash animation for feedback
                    val flash = AlphaAnimation(1f, 0.5f).apply {
                        duration = 100
                        repeatCount = 1
                        repeatMode = android.view.animation.Animation.REVERSE
                    }
                    startAnimation(flash)

                    // Get word meaning logic
                    val offset = getOffsetForPosition(e.x, e.y)
                    val word = getWordAtOffset(text, offset)
                    if (word.isNotEmpty()) {
                        showDefinitionPopup(word.lowercase())
                    }
                    return true
                }
            })

            setOnTouchListener { v, event ->
                gestureDetector.onTouchEvent(event)
                true
            }
        }
        bubbleWrapper.addView(textView)

        // View in Book Button Logic
        if (!isUser && chunk != null) {
            val btnSource = Button(this).apply {
                this.setText("View in Book 📖")
                this.textSize = 12f
                this.isAllCaps = false
                this.setTextColor(android.graphics.Color.parseColor("#3B82F6"))
                this.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                this.setOnClickListener {
                    val intent = Intent(context, ReaderActivity::class.java).apply {
                        putExtra("CHUNK_ID", chunk.id)
                        flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    }
                    startActivity(intent)
                }
            }
            bubbleWrapper.addView(btnSource)
        }

        horizontalContainer.addView(bubbleWrapper)

        // 3. RIGHT SIDE: Speaking Voice Button (Now Visible and anchored to bottom right)
        if (!isUser) {
            val playButton = ImageButton(this).apply {
                setImageResource(android.R.drawable.ic_media_play)
                setColorFilter(android.graphics.Color.parseColor("#3B82F6"))
                background = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.OVAL
                    setColor(android.graphics.Color.parseColor("#EEF2FF"))
                }
                val size = (40 * resources.displayMetrics.density).toInt()
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    gravity = android.view.Gravity.BOTTOM
                    setMargins(8, 0, 8, 8)
                }

                setOnClickListener {
                    if (tts.isSpeaking) {
                        tts.stop()
                        setImageResource(android.R.drawable.ic_media_play)
                    } else {
                        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "SpeechID")
                        setImageResource(android.R.drawable.ic_media_pause)
                    }
                }
            }
            horizontalContainer.addView(playButton)
        }

        chatContainer.addView(horizontalContainer)
        chatScrollView.post { chatScrollView.fullScroll(android.view.View.FOCUS_DOWN) }
    }

    private fun getWordAtOffset(text: String, offset: Int): String {
        if (offset < 0 || offset >= text.length) return ""
        var start = offset
        while (start > 0 && text[start - 1].isLetterOrDigit()) start--
        var end = offset
        while (end < text.length && text[end].isLetterOrDigit()) end++
        return text.substring(start, end).replace(Regex("[^a-zA-Z]"), "")
    }

    private fun showDefinitionPopup(word: String) {
        Toast.makeText(this, "Searching for '$word'...", Toast.LENGTH_SHORT).show()
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = "https://api.dictionaryapi.dev/api/v2/entries/en/$word"
                val response = java.net.URL(url).readText()
                val marker = "\"definition\":\""
                val startIdx = response.indexOf(marker)
                val result = if (startIdx != -1) {
                    val s = startIdx + marker.length
                    response.substring(s, response.indexOf("\"", s))
                } else "Definition not found."
                withContext(Dispatchers.Main) { displayPopup(word, result) }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { Toast.makeText(this@ChatAssistant, "Word not found.", Toast.LENGTH_SHORT).show() }
            }
        }
    }

    private fun displayPopup(word: String, definition: String) {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.layout_dictionary_bottom_sheet, null)
        val tvWord = view.findViewById<TextView>(R.id.tvWordTitle)
        val tvDef = view.findViewById<TextView>(R.id.tvDefinition)
        val btnCopy = view.findViewById<Button>(R.id.btnCopy)
        val btnDismiss = view.findViewById<Button>(R.id.btnDismiss)
        tvWord.text = word.replaceFirstChar { it.uppercase() }
        tvDef.text = definition
        btnCopy.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("definition", "$word: $definition")
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show()
        }
        btnDismiss.setOnClickListener { dialog.dismiss() }
        dialog.setContentView(view)
        (view.parent as View).setBackgroundColor(android.graphics.Color.TRANSPARENT)
        dialog.show()
    }

    private fun startVoiceInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
        }
        try { startActivityForResult(intent, SPEECH_REQUEST_CODE) } catch (e: Exception) {}
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val res = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            etMessage.setText(res?.get(0) ?: "")
        }
    }

    private fun sendQuickQuery(text: String) { if (isReady) performSearch(text) }

    private fun performSearch(query: String) {
        addChatBubble(query, true)

        val thinkingBubble = TextView(this).apply {
            this.text = "AI is thinking..."
            this.setPadding(32, 16, 32, 16)
            this.textSize = 12f
            this.alpha = 0.6f
            this.setTextColor(android.graphics.Color.GRAY)
        }
        chatContainer.addView(thinkingBubble)

        lifecycleScope.launch(Dispatchers.Default) {
            val bookBox = ObjectBox.store.boxFor(BookChunk::class.java)
            Log.d("SEARCH_DEBUG", "Total Chunks in DB: ${bookBox.count()}")
            withContext(Dispatchers.Main) { updateStatus("● SEARCHING BOOK...") }

            // Generate the vector for the user's query
            val queryEmbedding = embeddingGenerator?.generateEmbedding(query) ?: return@launch

            // --- This is the Symmetric Search in action ---
            val queryBuilder = bookBox.query(
                BookChunk_.embedding.nearestNeighbors(queryEmbedding, 1)
            ).build()

            val results = queryBuilder.findWithScores()
            // ----------------------------------------------

            withContext(Dispatchers.Main) {
                chatContainer.removeView(thinkingBubble)
                updateStatus("● READY")
            }

            if (results.isNotEmpty()) {
                val result = results[0]
                val score = result.score
                val bestMatch = result.get()

                // Symmetric Search: using 0.40f as the strict threshold for strong matches
                if (score > 0.40f) {
                    withContext(Dispatchers.Main) {
                        // Add (Score: $score) to the text so you can see it on your screen
                        addChatBubble("Match: ${String.format("%.2f", score)}\n\n${bestMatch.text}", false, chunk = bestMatch)
                    }

                } else {
                    withContext(Dispatchers.Main) {
                        addChatBubble("I'm not quite sure about that based on the book.", false)
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    addChatBubble("I'm sorry, no information was found.", false)
                }
            }
            queryBuilder.close()
        }
    }

    private suspend fun updateStatus(text: String) = withContext(Dispatchers.Main) {
        tvStatus.text = text
        if (text.contains("READY")) tvStatus.setTextColor(android.graphics.Color.parseColor("#10B981"))
    }

    override fun onInit(status: Int) { if (status == TextToSpeech.SUCCESS) tts.language = Locale.US }

    override fun onDestroy() {
        super.onDestroy()
        embeddingGenerator?.close()
        if (::tts.isInitialized) tts.shutdown()
    }
}