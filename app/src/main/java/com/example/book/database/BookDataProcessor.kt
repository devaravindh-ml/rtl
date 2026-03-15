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

        // 2. Break text into 700-character pieces
        val finalChunks = rawText.chunked(700)

        // DEBUG: Check if we actually have chunks to save
        println("DEBUG: Text length is ${rawText.length}")
        println("DEBUG: Number of chunks created: ${finalChunks.size}")

        val bookObjectsToSave = mutableListOf<BookChunk>()

        for (textPiece in finalChunks) {
            val vector = embeddingGenerator.generateEmbedding(textPiece)
            // We do NOT set the ID here. ObjectBox handles it because it's 0.
            bookObjectsToSave.add(BookChunk(text = textPiece, embedding = vector))
        }

        // 3. Save the LIST, not one by one
        bookBox.put(bookObjectsToSave)

        // 4. Final Verification
        println("DEBUG: Final count in Database: ${bookBox.count()}")
    }}