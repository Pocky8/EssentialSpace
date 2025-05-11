package com.essential.essspace.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.essential.essspace.room.Note
import com.essential.essspace.room.AppDatabase
import com.essential.essspace.room.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotesListViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: NoteRepository
    // Change: Rename 'allNotes' to a private backing field
    private val _notesStateFlow: StateFlow<List<Note>>

    var capturedPhotoPathForNote by mutableStateOf<String?>(null)
        private set
    var ocrTextForCapturedPhotoNote by mutableStateOf<String?>(null)
        private set
    var audioFilePathForNote by mutableStateOf<String?>(null)
    // Remove custom function setAudioFilePathForNote; use property setter directly.

    var showScreenshotAudioPromptDialog by mutableStateOf(false)
        private set
    var photoPathForScreenshotDialog by mutableStateOf<String?>(null)
        private set
    var ocrTextFromScreenshotDialog by mutableStateOf<String?>(null)
        private set

    init {
        val noteDao = AppDatabase.getDatabase(application).noteDao()
        repository = NoteRepository(application)
        // Change: Initialize the private backing field
        _notesStateFlow = repository.getAllNotesFlow()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    }

    // Change: Add public function to expose the StateFlow
    fun getAllNotes(): StateFlow<List<Note>> {
        return _notesStateFlow
    }

    fun insertNote(note: Note, callback: ((Long) -> Unit)? = null) = viewModelScope.launch {
        val rowId = repository.insertNote(note)
        Log.d("ViewModel", "Note inserted with ID: $rowId, Title: ${note.title}")
        callback?.invoke(rowId)
    }

    fun updateNote(note: Note) = viewModelScope.launch {
        repository.updateNote(note)
        Log.d("ViewModel", "Note updated: ID: ${note.id}")
    }

    fun deleteNote(note: Note) = viewModelScope.launch {
        repository.deleteNoteById(note.id)
        Log.d("ViewModel", "Note delete requested for ID: ${note.id}")
    }

    fun deleteNoteById(noteId: Int) = viewModelScope.launch {
        repository.deleteNoteById(noteId)
        Log.d("ViewModel", "Note delete requested for ID: $noteId")
    }

    fun getNoteByIdFlow(noteId: Int): Flow<Note?> {
        return repository.getNoteByIdFlow(noteId)
    }

    fun prepareForNewCapture() {
        Log.d("ViewModel", "Clearing temporary note data.")
        capturedPhotoPathForNote = null
        ocrTextForCapturedPhotoNote = null
        audioFilePathForNote = null
    }

    fun setCapturedDataForNote(photoPath: String?, ocrText: String?) {
        Log.d("ViewModel", "Captured data - Photo: $photoPath, OCR: ${ocrText?.take(50)}")
        capturedPhotoPathForNote = photoPath
        ocrTextForCapturedPhotoNote = ocrText
    }

    fun handleScreenshotProcessed(path: String, ocrText: String?) {
        Log.d("ViewModel", "Screenshot processed - Path: $path, OCR: ${ocrText?.take(50)}")
        photoPathForScreenshotDialog = path
        ocrTextFromScreenshotDialog = ocrText
        showScreenshotAudioPromptDialog = true
    }

    fun prepareForAudioWithScreenshotData() {
        Log.d("ViewModel", "Transferring screenshot data.")
        capturedPhotoPathForNote = photoPathForScreenshotDialog
        ocrTextForCapturedPhotoNote = ocrTextFromScreenshotDialog
        clearScreenshotDialogData()
    }

    fun clearScreenshotDialogData() {
        Log.d("ViewModel", "Clearing screenshot dialog data.")
        showScreenshotAudioPromptDialog = false
        photoPathForScreenshotDialog = null
        ocrTextFromScreenshotDialog = null
    }
}