package com.example.book.database

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.annotation.HnswIndex
import io.objectbox.annotation.VectorDistanceType

@Entity
data class BookChunk(
    @Id var id: Long = 0,
    var text: String = "",

    // Symmetric Search requires COSINE distance for text similarity
    @HnswIndex(
        dimensions = 384,
        distanceType = VectorDistanceType.COSINE
    )
    var embedding: FloatArray? = null
)