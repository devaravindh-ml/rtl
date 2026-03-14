package com.example.book.ai

import kotlin.math.sqrt

class VectorSearch {

    data class ChunkEmbedding(
        val text: String,
        val embedding: FloatArray
    )

    private val database = mutableListOf<ChunkEmbedding>()

    fun addChunk(text: String, embedding: FloatArray) {
        database.add(ChunkEmbedding(text, embedding))
    }

    fun search(queryEmbedding: FloatArray, topK: Int = 3): List<String> {

        return database
            .map { chunk ->
                val similarity = cosineSimilarity(queryEmbedding, chunk.embedding)
                Pair(chunk.text, similarity)
            }
            .sortedByDescending { it.second }
            .take(topK)
            .map { it.first }
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

        return (dot / (sqrt(normA) * sqrt(normB)))
    }
}