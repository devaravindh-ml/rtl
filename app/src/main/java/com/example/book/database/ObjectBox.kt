package com.example.book.database

import android.content.Context
import io.objectbox.BoxStore
import com.example.book.MyObjectBox

object ObjectBox {
    lateinit var store: BoxStore
        private set

    // Helper to check if it's ready
    val isInitialized: Boolean
        get() = this::store.isInitialized

    fun init(context: Context) {
        if (isInitialized) return // Prevent double-initialization

        store = MyObjectBox.builder()
            .androidContext(context.applicationContext)
            .build()
    }
}