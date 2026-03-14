package com.example.book

import android.content.Context
import ai.onnxruntime.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.LongBuffer
import java.util.Collections

class EmbeddingGenerator private constructor(
    private val env: OrtEnvironment,
    private val session: OrtSession,
    private val vocabulary: Map<String, Long>
) {

    companion object {

        suspend fun create(context: Context, modelPath: String): EmbeddingGenerator =
            withContext(Dispatchers.IO) {

                val env = OrtEnvironment.getEnvironment()

                val modelBytes = context.assets.open(modelPath).use { it.readBytes() }

                val session = env.createSession(modelBytes)

                val vocabMap = mutableMapOf<String, Long>()

                context.assets.open("vocab.txt").bufferedReader().useLines { lines ->
                    lines.forEachIndexed { index, word ->
                        vocabMap[word] = index.toLong()
                    }
                }

                EmbeddingGenerator(env, session, Collections.unmodifiableMap(vocabMap))
            }
    }

    suspend fun generateEmbedding(text: String): FloatArray =
        withContext(Dispatchers.Default) {

            val maxLen = 128

            val tokens = tokenize(text)
                .take(maxLen)
                .toLongArray()

            val seqLen = tokens.size

            val shape = longArrayOf(1, seqLen.toLong())

            val inputIds = OnnxTensor.createTensor(
                env,
                LongBuffer.wrap(tokens),
                shape
            )

            val attentionMask = LongArray(seqLen) { 1 }

            val maskTensor = OnnxTensor.createTensor(
                env,
                LongBuffer.wrap(attentionMask),
                shape
            )

            val typeIds = LongArray(seqLen) { 0 }

            val typeTensor = OnnxTensor.createTensor(
                env,
                LongBuffer.wrap(typeIds),
                shape
            )

            val inputs = mapOf(
                "input_ids" to inputIds,
                "attention_mask" to maskTensor,
                "token_type_ids" to typeTensor
            )

            session.run(inputs).use { results ->

                @Suppress("UNCHECKED_CAST")
                val output = results[0].value as Array<Array<FloatArray>>

                meanPooling(output[0])
            }
        }

    private fun meanPooling(tokens: Array<FloatArray>): FloatArray {

        val dim = tokens[0].size

        val result = FloatArray(dim)

        for (token in tokens) {
            for (i in token.indices) {
                result[i] += token[i]
            }
        }

        for (i in result.indices) {
            result[i] /= tokens.size
        }

        return result
    }

    private fun tokenize(text: String): List<Long> {

        val words = text
            .lowercase()
            .split(Regex("\\s+|(?=[\\p{Punct}])|(?<=[\\p{Punct}])"))
            .filter { it.isNotBlank() }

        val ids = mutableListOf<Long>()

        ids.add(101L) // CLS

        for (word in words) {
            ids.add(vocabulary[word] ?: 100L)
        }

        ids.add(102L) // SEP

        return ids
    }

    fun close() {
        session.close()
        env.close()
    }
}