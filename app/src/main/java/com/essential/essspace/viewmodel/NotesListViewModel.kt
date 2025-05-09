package com.essential.essspace.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.essential.essspace.room.Note
import com.essential.essspace.room.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class NotesListViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: NoteRepository
    val allNotes: Flow<List<Note>>

    init {
        // NoteRepository expects a Context, which 'application' (Application class) is.
        repository = NoteRepository(application) // CORRECTED: Pass the application context
        allNotes = repository.getAllNotesFlow()
    }

    fun insertNote(note: Note) = viewModelScope.launch {
        repository.insertNote(note)
    }

    fun updateNote(note: Note) = viewModelScope.launch {
        repository.updateNote(note)
    }

    fun getNoteById(id: Int): Flow<Note?> {
        return repository.getNoteByIdFlow(id)
    }

    fun deleteNoteById(id: Int) = viewModelScope.launch {
        repository.deleteNoteById(id)
    }
}