package com.essential.essspace.viewmodel

import android.content.Context
import androidx.lifecycle.*
import com.essential.essspace.room.Note
import com.essential.essspace.room.NoteRepository
import kotlinx.coroutines.launch
import java.io.File

class NoteViewModel(private val repository: NoteRepository) : ViewModel() {

    private val _capturedPhoto = MutableLiveData<File?>()
    val capturedPhoto: LiveData<File?> get() = _capturedPhoto

    private val _recordedAudio = MutableLiveData<File?>()
    val recordedAudio: LiveData<File?> get() = _recordedAudio

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

        // Save on background thread
        viewModelScope.launch {
            repository.insertNote(note)
        }
    }
}



