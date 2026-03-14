package com.example.book.ai

import ai.onnxruntime.*

class EmbeddingGenerator(modelPath: String) {

    private val env: OrtEnvironment = OrtEnvironment.getEnvironment()
    private val session: OrtSession = env.createSession(modelPath)

    fun generateEmbedding(text: String): FloatArray {

        val inputTensor = OnnxTensor.createTensor(
            env,
            arrayOf(text)
        )

        val results = session.run(mapOf("input" to inputTensor))

        val output = results[0].value as Array<FloatArray>

        return output[0]
    }
}