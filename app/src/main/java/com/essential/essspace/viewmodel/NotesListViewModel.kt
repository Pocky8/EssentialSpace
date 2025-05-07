package com.essential.essspace.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.essential.essspace.room.Note
import com.essential.essspace.room.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class NotesListViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: NoteRepository = NoteRepository(application)

    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    val notes: StateFlow<List<Note>> = _notes.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadNotes()
    }

    fun loadNotes() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getAllNotesFlow().collectLatest { noteList ->
                _notes.value = noteList
                _isLoading.value = false
            }
        }
    }

    fun insertNote(note: Note) {
        viewModelScope.launch {
            repository.insertNote(note)
            // Flow will automatically update the list
        }
    }

    fun deleteNote(noteId: Long) {
        viewModelScope.launch {
            repository.deleteNoteById(noteId)
            // Flow will automatically update the list
        }
    }
}