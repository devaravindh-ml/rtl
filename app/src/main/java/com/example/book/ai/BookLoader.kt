package com.example.book.ai

import android.content.Context

class BookLoader(private val context: Context) {

    fun loadBook(): String {
        return context.assets.open("book.txt")
            .bufferedReader()
            .use { it.readText() }
    }
}