package com.essential.essspace.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.essential.essspace.room.Note
import com.essential.essspace.room.NoteRepository
import kotlinx.coroutines.launch

class NoteViewModel(private val repository: NoteRepository) : ViewModel() {

    fun insertNote(note: Note) {
        viewModelScope.launch {
            repository.insertNote(note)
        }
    }

    fun deleteNoteById(id: Long) {
        viewModelScope.launch {
            repository.deleteNoteById(id)
        }
    }

    // Add other methods as needed, e.g., getAllNotes(), getNoteById(id), etc.
}
