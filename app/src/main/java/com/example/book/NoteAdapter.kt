package com.example.book

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class NoteAdapter(private var notes: List<StudyNote>) : RecyclerView.Adapter<NoteAdapter.NoteViewHolder>() {

    class NoteViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTag: TextView = view.findViewById(R.id.tvNoteTag)
        val tvTime: TextView = view.findViewById(R.id.tvNoteTime)
        val tvTitle: TextView = view.findViewById(R.id.tvNoteTitle)
        val tvSnippet: TextView = view.findViewById(R.id.tvNoteSnippet)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_note_preview, parent, false)
        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = notes[position]
        holder.tvTitle.text = note.title
        holder.tvSnippet.text = note.content
        holder.tvTime.text = note.timestamp
        holder.tvTag.text = note.type.name

        // Set tag color based on type
        when(note.type) {
            NoteType.DEFINITION -> holder.tvTag.setTextColor(android.graphics.Color.parseColor("#3B82F6"))
            NoteType.REFLECTION -> holder.tvTag.setTextColor(android.graphics.Color.parseColor("#64748B"))
            NoteType.ACTION_ITEM -> holder.tvTag.setTextColor(android.graphics.Color.parseColor("#10B981"))
        }
    }

    override fun getItemCount() = notes.size

    fun updateList(newList: List<StudyNote>) {
        notes = newList
        notifyDataSetChanged()
    }
}