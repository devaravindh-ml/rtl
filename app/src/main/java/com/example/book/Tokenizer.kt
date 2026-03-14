package com.example.book

object Tokenizer {

    fun simpleTokenize(text: String): Pair<LongArray, LongArray> {

        val words = text.lowercase().split(" ")

        val maxLength = 128

        val ids = LongArray(maxLength) {0}
        val mask = LongArray(maxLength) {0}

        for (i in words.indices.take(maxLength)) {
            ids[i] = words[i].hashCode().toLong()
            mask[i] = 1
        }

        return Pair(ids, mask)
    }
}