package com.example.book.database

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id

@Entity
data class BookChunk(

    @Id
    var id: Long = 0,

    var text: String = "",

    var embedding: FloatArray? = null
)