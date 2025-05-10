package com.essential.essspace.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.essential.essspace.room.Note
import com.essential.essspace.room.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class NotesListViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: NoteRepository
    val allNotes: Flow<List<Note>>

    // --- State for photo capture / screenshot-to-audio flow ---
    var capturedPhotoPathForNote: String? by mutableStateOf(null)
        private set
    var ocrTextForCapturedPhotoNote: String? by mutableStateOf(null)
        private set

    // --- State specifically for screenshot flow dialog ---
    var showScreenshotAudioPromptDialog: Boolean by mutableStateOf(false)
        private set
    var photoPathForScreenshotDialog: String? by mutableStateOf(null)
        private set
    var ocrTextFromScreenshotDialog: String? by mutableStateOf(null)
        private set

    init {
        repository = NoteRepository(application)
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

    // --- Methods to manage screenshot dialog state ---
    fun onScreenshotBroadcastReceived(path: String, ocrText: String?) {
        photoPathForScreenshotDialog = path
        ocrTextFromScreenshotDialog = ocrText
        showScreenshotAudioPromptDialog = true
    }

    fun clearScreenshotDialogData() {
        photoPathForScreenshotDialog = null
        ocrTextFromScreenshotDialog = null
        showScreenshotAudioPromptDialog = false
    }

    // --- Methods to manage general capture flow state (camera or screenshot chosen for audio) ---
    fun prepareForNewCapture() {
        capturedPhotoPathForNote = null
        ocrTextForCapturedPhotoNote = null
    }

    fun setCapturedDataForNote(photoPath: String?, ocrText: String?) {
        capturedPhotoPathForNote = photoPath
        ocrTextForCapturedPhotoNote = ocrText
    }

    fun prepareForAudioWithScreenshotData() {
        capturedPhotoPathForNote = photoPathForScreenshotDialog
        ocrTextForCapturedPhotoNote = ocrTextFromScreenshotDialog
        // Dialog specific data can be cleared after transferring to general capture flow
        clearScreenshotDialogData()
    }
}