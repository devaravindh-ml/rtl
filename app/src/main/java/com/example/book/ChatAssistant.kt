package com.example.book

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
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
                initBookDatabase()
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

    private fun addChatBubble(text: String, isUser: Boolean) {
        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            val sideMargin = (resources.displayMetrics.widthPixels * 0.15).toInt()
            setMargins(if (isUser) sideMargin else 24, 16, if (isUser) 24 else sideMargin, 16)
            gravity = if (isUser) android.view.Gravity.END else android.view.Gravity.START
        }

        val textView = TextView(this).apply {
            this.text = text
            this.setPadding(48, 32, 48, 32)
            this.textSize = 16f
            this.setTextColor(if (isUser) android.graphics.Color.WHITE else android.graphics.Color.parseColor("#1E293B"))
            this.setBackgroundResource(if (isUser) R.drawable.bg_bubble_user else R.drawable.bg_bubble_ai)
            this.layoutParams = layoutParams
            this.elevation = 2f

            val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
                override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                    toggleSpeech(text)
                    return true
                }

                override fun onDoubleTap(e: MotionEvent): Boolean {
                    // Visual Effect: Brief flash animation
                    val flash = AlphaAnimation(1f, 0.5f)
                    flash.duration = 100
                    flash.repeatCount = 1
                    flash.repeatMode = android.view.animation.Animation.REVERSE
                    startAnimation(flash)

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

        chatContainer.addView(textView)
        chatScrollView.post { chatScrollView.fullScroll(android.view.View.FOCUS_DOWN) }
    }

    private fun TextView.getOffsetForPosition(x: Float, y: Float): Int {
        if (layout == null) return -1
        val touchX = x - totalPaddingLeft
        val touchY = y - totalPaddingTop
        val line = layout.getLineForVertical(touchY.toInt())
        return layout.getOffsetForHorizontal(line, touchX)
    }

    private fun getWordAtOffset(text: String, offset: Int): String {
        if (offset < 0 || offset >= text.length) return ""
        var start = offset
        while (start > 0 && text[start - 1].isLetterOrDigit()) start--
        var end = offset
        while (end < text.length && text[end].isLetterOrDigit()) end++
        return text.substring(start, end).replace(Regex("[^a-zA-Z]"), "")
    }

    private fun toggleSpeech(text: String) {
        if (::tts.isInitialized) {
            if (tts.isSpeaking) { tts.stop() }
            else { tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "SpeechID") }
        }
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
        // 1. Initialize the BottomSheetDialog
        val dialog = BottomSheetDialog(this)

        // 2. Inflate the custom layout we created (layout_dictionary_bottom_sheet.xml)
        val view = layoutInflater.inflate(R.layout.layout_dictionary_bottom_sheet, null)

        // 3. Bind the views from the XML
        val tvWord = view.findViewById<TextView>(R.id.tvWordTitle)
        val tvDef = view.findViewById<TextView>(R.id.tvDefinition)
        val btnCopy = view.findViewById<Button>(R.id.btnCopy)
        val btnDismiss = view.findViewById<Button>(R.id.btnDismiss)

        // 4. Set the content
        // Capitalizes the first letter of the word for a title look
        tvWord.text = word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        tvDef.text = definition

        // 5. Logic for the 'Copy' button
        btnCopy.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("definition", "$word: $definition")
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show()
        }

        // 6. Logic for the 'Dismiss' button
        btnDismiss.setOnClickListener {
            dialog.dismiss()
        }

        // 7. Configure and show the dialog
        dialog.setContentView(view)

        // This removes the default gray background from the dialog container
        // so our rounded CardView corners are visible
        (view.parent as View).setBackgroundColor(android.graphics.Color.TRANSPARENT)

        dialog.show()
    }
    // --- REMAINDER OF YOUR LOGIC ---

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

    private suspend fun initBookDatabase() {
        val bookBox = ObjectBox.store.boxFor(BookChunk::class.java)
        if (bookBox.count() <= 1L) {
            val generator = embeddingGenerator ?: return
            withContext(Dispatchers.Main) { loadingOverlay.visibility = View.VISIBLE }
            withContext(Dispatchers.IO) {
                bookBox.removeAll()
                val content = assets.open("rtl_book.txt").bufferedReader().use { it.readText() }
                BookDataProcessor(generator).processBookText(content)
            }
            withContext(Dispatchers.Main) { loadingOverlay.visibility = View.GONE }
        }
    }

    private fun performSearch(query: String) {
        addChatBubble(query, true)
        lifecycleScope.launch(Dispatchers.Default) {
            val bookBox = ObjectBox.store.boxFor(BookChunk::class.java)
            val queryEmbedding = embeddingGenerator?.generateEmbedding(query) ?: return@launch
            var bestMatch: BookChunk? = null
            var maxScore = -1f
            for (chunk in bookBox.all) {
                val score = cosineSimilarity(queryEmbedding, chunk.embedding ?: continue)
                if (score > maxScore) { maxScore = score; bestMatch = chunk }
            }
            val resultText = if (maxScore > 0.35f) bestMatch?.text ?: "" else "No answer found."
            withContext(Dispatchers.Main) { addChatBubble(resultText, false) }
        }
    }

    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        var dot = 0f; var nA = 0f; var nB = 0f
        for (i in a.indices) { dot += a[i] * b[i]; nA += a[i] * a[i]; nB += b[i] * b[i] }
        return if (nA == 0f || nB == 0f) 0f else (dot / (Math.sqrt(nA.toDouble()) * Math.sqrt(nB.toDouble()))).toFloat()
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