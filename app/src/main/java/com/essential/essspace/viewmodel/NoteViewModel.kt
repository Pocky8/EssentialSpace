package com.essential.essspace.viewmodel

import androidx.lifecycle.*
import com.essential.essspace.room.Note
import com.essential.essspace.room.NoteRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class NoteViewModel(private val repository: NoteRepository) : ViewModel() {

    private val _capturedPhoto = MutableLiveData<File?>()
    val capturedPhoto: LiveData<File?> get() = _capturedPhoto

    private val _recordedAudio = MutableLiveData<File?>()
    val recordedAudio: LiveData<File?> get() = _recordedAudio

    // Expose all notes as a StateFlow
    val allNotes: StateFlow<List<Note>> = repository.getAllNotesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setCapturedPhoto(file: File) {
        _capturedPhoto.value = file
    }

    fun setRecordedAudio(file: File) {
        _recordedAudio.value = file
    }

    fun saveNote(text: String?) {
        val photoPath = _capturedPhoto.value?.absolutePath
        val audioPath = _recordedAudio.value?.absolutePath

        val note = Note(
            photoPath = photoPath,
            audioPath = audioPath,
            text = text
        )

        viewModelScope.launch {
            repository.insertNote(note)
        }
    }
}