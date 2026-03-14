package com.example.book

import android.app.Application
import com.example.book.database.ObjectBox
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MySublimeApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize ObjectBox
        ObjectBox.init(this)
    }
}