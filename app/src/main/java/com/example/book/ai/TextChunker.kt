package com.example.book.ai

object TextChunker {

    fun chunkText(text: String, chunkSize: Int = 2000): List<String> {
        val chunks = mutableListOf<String>()
        var start = 0

        while (start < text.length) {
            val end = (start + chunkSize).coerceAtMost(text.length)
            chunks.add(text.substring(start, end))
            start = end
        }

        return chunks
    }
}