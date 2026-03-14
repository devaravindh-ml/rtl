package com.example.book.database

import android.content.Context
import com.example.book.EmbeddingGenerator
import io.objectbox.Box
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BookDataProcessor(
    private val context: Context,
    private val embeddingGenerator: EmbeddingGenerator
) {

    // ObjectBox table for BookChunk
    private val bookBox: Box<BookChunk> =
        ObjectBox.store.boxFor(BookChunk::class.java)

    suspend fun processBookText(rawText: String) = withContext(Dispatchers.IO) {

        // Split book into chunks (paragraphs)
        val chunks = rawText
            .split(Regex("\n\n|\r\n\r\n"))
            .map { it.trim() }
            .filter { it.isNotBlank() && it.length > 50 }

        println("Total chunks found: ${chunks.size}")

        chunks.forEachIndexed { index, paragraph ->

            try {

                // Generate embedding
                val vector = embeddingGenerator.generateEmbedding(paragraph)

                // Save to ObjectBox
                val newChunk = BookChunk(
                    text = paragraph,
                    embedding = vector
                )

                bookBox.put(newChunk)

                println("Saved chunk $index")

            } catch (e: Exception) {

                println("Failed to process chunk $index: ${e.message}")
            }
        }

        println("Book indexing complete. Total stored: ${bookBox.count()}")
    }
}