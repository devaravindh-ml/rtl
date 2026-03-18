package com.example.book.database

import com.example.book.EmbeddingGenerator
import io.objectbox.Box
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BookDataProcessor(
    private val embeddingGenerator: EmbeddingGenerator
) {
    private val bookBox: Box<BookChunk> = ObjectBox.store.boxFor(BookChunk::class.java)

    suspend fun processBookText(rawText: String) = withContext(Dispatchers.IO) {
        // 1. Reset everything
        bookBox.removeAll()

        // 2. SMART CHUNKING: Split by double newlines or sentences to preserve meaning
        // Symmetric search works best when chunks are meaningful paragraphs.
        val paragraphs = rawText.split("\n\n")
            .filter { it.trim().length > 20 } // Ignore empty lines/tiny fragments
            .map { it.trim() }

        val bookObjectsToSave = mutableListOf<BookChunk>()

        for (paragraph in paragraphs) {
            // If a paragraph is too long (over 1000 chars), split it further
            if (paragraph.length > 1000) {
                val subChunks = paragraph.chunked(800)
                for (sub in subChunks) {
                    val vector = embeddingGenerator.generateEmbedding(sub)
                    bookObjectsToSave.add(BookChunk(text = sub, embedding = vector))
                }
            } else {
                val vector = embeddingGenerator.generateEmbedding(paragraph)
                bookObjectsToSave.add(BookChunk(text = paragraph, embedding = vector))
            }
        }

        // 3. Batch Save (Much faster than put() inside the loop)
        bookBox.put(bookObjectsToSave)

        // 4. Verification
        println("DEBUG: Processed ${bookObjectsToSave.size} semantic chunks.")
        println("DEBUG: Final count in Database: ${bookBox.count()}")
    }
}