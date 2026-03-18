package com.example.book.database

import android.content.Context
import io.objectbox.BoxStore
import com.example.book.MyObjectBox // Generated after Build -> Rebuild

object ObjectBox {
    lateinit var store: BoxStore
        private set

    val isInitialized: Boolean
        get() = this::store.isInitialized

    fun init(context: Context) {
        if (isInitialized) return

        // The builder creates the database file on the device
        store = MyObjectBox.builder()
            .androidContext(context.applicationContext)
            .build()
    }
}