package com.example.book.assistant

import android.content.Context
import com.example.book.ai.BookLoader
import com.example.book.ai.TextChunker
import com.example.book.ai.EmbeddingGenerator
import com.example.book.ai.VectorSearch

class BookAssistant(private val context: Context) {

    private val vectorSearch = VectorSearch()
    private lateinit var embedder: EmbeddingGenerator

    fun initialize() {

        embedder = EmbeddingGenerator("embedding.onnx")

        val loader = BookLoader(context)
        val bookText = loader.loadBook()

        val chunks = TextChunker.chunkText(bookText)

        chunks.forEach {
            val embedding = embedder.generateEmbedding(it)
            vectorSearch.addChunk(it, embedding)
        }
    }

    fun askQuestion(question: String): String {

        val questionEmbedding = embedder.generateEmbedding(question)

        val contextChunks = vectorSearch.search(questionEmbedding)

        val context = contextChunks.joinToString("\n")

        return """
        Context:
        $context

        Question:
        $question
        """.trimIndent()
    }
}