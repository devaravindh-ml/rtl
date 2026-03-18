package com.example.book

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout

class MyNotes : AppCompatActivity() {

    private lateinit var rvRecentNotes: RecyclerView
    private lateinit var noteAdapter: NoteAdapter
    private lateinit var chipGroupFilters: ChipGroup
    private lateinit var llEmptyState: LinearLayout

    // This is our Master List (The "ALL" source)
    private var allNotesList = mutableListOf<StudyNote>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.my_notes)

        // 1. Initialize UI Components
        rvRecentNotes = findViewById(R.id.rvRecentNotes)
        chipGroupFilters = findViewById(R.id.chipGroupFilters)
        llEmptyState = findViewById(R.id.llEmptyState)
        val fabAddNote = findViewById<FloatingActionButton>(R.id.fabAddNote)
        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)

        // 2. Load Dummy Data (Simulating saved definitions/notes)
        loadSampleData()

        // 3. Setup RecyclerView
        noteAdapter = NoteAdapter(allNotesList)
        rvRecentNotes.layoutManager = LinearLayoutManager(this)
        rvRecentNotes.adapter = noteAdapter

        // 4. "ALL" and Filter Logic
        setupFilterLogic()

        // 5. FAB Logic
        fabAddNote.setOnClickListener {
            // Logic to open a "Create Note" screen could go here
        }

        // Initial Empty State Check
        updateEmptyState(allNotesList.isEmpty())
    }

    private fun setupFilterLogic() {
        chipGroupFilters.setOnCheckedStateChangeListener { group, checkedIds ->
            val selectedChipId = checkedIds.firstOrNull()

            if (selectedChipId == null) {
                // Default to ALL if nothing selected
                showFilteredList("All")
            } else {
                val selectedChip = findViewById<Chip>(selectedChipId)
                showFilteredList(selectedChip.text.toString())
            }
        }
    }

    private fun showFilteredList(filterName: String) {
        val filteredList = when (filterName) {
            "Definitions" -> allNotesList.filter { it.type == NoteType.DEFINITION }
            "Reflections" -> allNotesList.filter { it.type == NoteType.REFLECTION }
            "Action Items" -> allNotesList.filter { it.type == NoteType.ACTION_ITEM }
            else -> allNotesList // This handles the "ALL" case
        }

        noteAdapter.updateList(filteredList)
        updateEmptyState(filteredList.isEmpty())
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        llEmptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
        rvRecentNotes.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun loadSampleData() {
        allNotesList.add(StudyNote(1, "Interpret", "To explain the meaning of information, words, or actions in leadership.", "2h ago", NoteType.DEFINITION))
        allNotesList.add(StudyNote(2, "Systems Thinking", "Shift from looking at parts to looking at the whole system.", "5h ago", NoteType.DEFINITION))
        allNotesList.add(StudyNote(3, "Personal Goal", "Try to listen more than I speak in the Monday meeting.", "Yesterday", NoteType.ACTION_ITEM))
        allNotesList.add(StudyNote(4, "Book Reflection", "The chapter on Universal Values really changed my perspective.", "Feb 20", NoteType.REFLECTION))
    }
}