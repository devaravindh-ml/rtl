package com.example.book.database

import android.content.Context
import io.objectbox.BoxStore
// This imports the Java class into your Kotlin file
import com.example.book.MyObjectBox

object ObjectBox {
    lateinit var store: BoxStore
        private set

    fun init(context: Context) {
        // Kotlin handles the Java 'builder()' perfectly
        store = MyObjectBox.builder()
            .androidContext(context.applicationContext)
            .build()
    }
}