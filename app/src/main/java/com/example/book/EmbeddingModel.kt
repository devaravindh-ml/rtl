package com.example.book

import ai.onnxruntime.*
import android.content.Context
import java.nio.LongBuffer

class EmbeddingModel(context: Context) {

    private val env: OrtEnvironment = OrtEnvironment.getEnvironment()
    private val session: OrtSession

    init {
        val modelBytes = context.assets.open("embedding.onnx").readBytes()
        session = env.createSession(modelBytes)
    }

    fun embed(inputIds: LongArray, attentionMask: LongArray): FloatArray {

        val inputShape = longArrayOf(1, inputIds.size.toLong())

        val idsTensor = OnnxTensor.createTensor(
            env,
            LongBuffer.wrap(inputIds),
            inputShape
        )

        val maskTensor = OnnxTensor.createTensor(
            env,
            LongBuffer.wrap(attentionMask),
            inputShape
        )

        val inputs = mapOf(
            "input_ids" to idsTensor,
            "attention_mask" to maskTensor
        )

        val result = session.run(inputs)

        val output = result[0].value as Array<FloatArray>

        return output[0]   // 384-dimension embedding
    }
}