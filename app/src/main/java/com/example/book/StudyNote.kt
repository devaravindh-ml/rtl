package com.example.book

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Removed the outer "class StudyNote" wrapper to make these first-class citizens
data class StudyNote(
    val id: Long = 0,
    val title: String,
    val content: String,
    val timestamp: String = getCurrentTimestamp(),
    val type: NoteType
)

enum class NoteType {
    DEFINITION, REFLECTION, ACTION_ITEM
}

// Helper function to auto-generate the "2h ago" style or date
fun getCurrentTimestamp(): String {
    val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    return sdf.format(Date())
}