package com.example.book.data

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id

@Entity
data class Book(
    @Id var id: Long = 0,
    var title: String? = null,
    var author: String? = null,
    var uri: String? = null,        // Store the Uri as a String
    var coverPath: String? = null,  // Path to a local thumbnail
    var lastReadLocation: String? = null, // The Readium "Locator" JSON
    var addedDate: Long = System.currentTimeMillis()
)