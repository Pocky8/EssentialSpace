package com.essential.essspace.viewmodel

import android.app.Application
import android.content.Context // Required for ConnectivityManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.essential.essspace.BuildConfig
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.essential.essspace.room.Note
import com.essential.essspace.room.AppDatabase
import com.essential.essspace.room.NoteRepository
import com.essential.essspace.CloudSummarizer // Import CloudSummarizer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import android.util.Patterns // Import Patterns for URL matching
import com.essential.essspace.util.OcrTextUtil // Ensure this is imported
import java.util.regex.Matcher

class NotesListViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: NoteRepository
    private val _notesStateFlow: StateFlow<List<Note>>

    // Instantiate CloudSummarizer directly
    private val cloudSummarizer = CloudSummarizer()

    var capturedPhotoPathForNote by mutableStateOf<String?>(null)
        private set
    var ocrTextForCapturedPhotoNote by mutableStateOf<String?>(null)
        private set
    var audioFilePathForNote by mutableStateOf<String?>(null)
    var transcribedTextForNote by mutableStateOf<String?>(null) // Added to hold transcribed text
        private set

    var showScreenshotAudioPromptDialog by mutableStateOf(false)
        private set
    var photoPathForScreenshotDialog by mutableStateOf<String?>(null)
        private set
    var ocrTextFromScreenshotDialog by mutableStateOf<String?>(null)
        private set

    var summarizationStatus by mutableStateOf<String?>(null)
        private set

    init {
        val noteDao = AppDatabase.getDatabase(application).noteDao()
        repository = NoteRepository(application)
        _notesStateFlow = repository.getAllNotesFlow()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    }

    private fun isOnline(): Boolean {
        val connectivityManager =
            getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }

    fun getAllNotes(): StateFlow<List<Note>> {
        return _notesStateFlow
    }

    private fun extractUrls(text: String?): List<String> {
        if (text.isNullOrBlank()) return emptyList()

        // Prepare a copy of the text specifically for robust link extraction
        val textForLinkFinding = OcrTextUtil.prepareOcrTextForLinkExtraction(text)
        Log.d("ViewModel", "Original text for link extraction: ${text.take(100)}")
        Log.d("ViewModel", "Prepared text for link extraction: ${textForLinkFinding.take(100)}")


        val links = mutableListOf<String>()
        // Use the prepared text for matching
        val matcher: Matcher = Patterns.WEB_URL.matcher(textForLinkFinding)
        while (matcher.find()) {
            var url = matcher.group()
            // Further refinement: ensure scheme if missing, as Patterns.WEB_URL can match domains without it
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "http://$url" // Default to http, or try to be smarter if possible
            }
            links.add(url)
        }
        Log.d("ViewModel", "Extracted links: $links from prepared text: ${textForLinkFinding.take(100)}")
        return links.distinct() // Return only unique links
    }

    fun insertNote(note: Note, callback: ((Long) -> Unit)? = null) = viewModelScope.launch {
        // 'note.text' should already be the readable combined text.
        // 'note.ocrText' should be the readable OCR text.
        // 'note.transcribedText' is the readable transcribed text.
        val links = extractUrls(note.text) // extractUrls will use prepareOcrTextForLinkExtraction internally
        val noteWithLinks = note.copy(links = links)
        val rowId = repository.insertNote(noteWithLinks)
        Log.d("ViewModel", "Note inserted with ID: $rowId, Title: ${noteWithLinks.title}, Links: ${noteWithLinks.links}, Readable Text: ${noteWithLinks.text?.take(100)}")
        callback?.invoke(rowId)
    }

    fun updateNote(note: Note) = viewModelScope.launch {
        val links = extractUrls(note.text) // extractUrls will use prepareOcrTextForLinkExtraction internally
        val updatedNoteWithLinks = note.copy(links = links)
        repository.updateNote(updatedNoteWithLinks)
        Log.d("ViewModel", "Note updated: ID: ${updatedNoteWithLinks.id}, Links: ${updatedNoteWithLinks.links}, Readable Text: ${updatedNoteWithLinks.text?.take(100)}")
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

    fun summarizeNote(noteId: Int, textToSummarize: String) {
        if (textToSummarize.isBlank()) {
            summarizationStatus = "Nothing to summarize."
            viewModelScope.launch {
                kotlinx.coroutines.delay(2000)
                summarizationStatus = null
            }
            return
        }
        if (!isOnline()) {
            summarizationStatus = "No internet connection for summarization."
            viewModelScope.launch {
                kotlinx.coroutines.delay(2000)
                summarizationStatus = null
            }
            return
        }

        summarizationStatus = "Summarizing..."
        viewModelScope.launch {
            // TODO: Get API Key from secure storage/settings
            val apiKey = BuildConfig.github_fix // User-provided Hugging Face API Key
            if (apiKey.isBlank()) {
                summarizationStatus = "API Key not set for summarization."
                Log.e("ViewModel", "Hugging Face API Key is not set.")
                kotlinx.coroutines.delay(3000)
                summarizationStatus = null
                return@launch
            }

            val result = cloudSummarizer.summarize(text = textToSummarize, apiKey = apiKey)

            result.fold(
                onSuccess = { summary ->
                    // Fetch the note, update it, and save
                    val noteToUpdate = getNoteByIdFlow(noteId).firstOrNull() // Get current value
                    noteToUpdate?.let {
                        val updatedNote = it.copy(
                            summary = summary,
                            lastModified = System.currentTimeMillis()
                            // Potentially re-extract links if summary is added to note.text
                            // links = extractUrls(it.text) // Or extractUrls(it.text + "\n" + summary) if summary is appended
                        )
                        updateNote(updatedNote) // This will call the updated updateNote which extracts links
                        summarizationStatus = "Summary generated!"
                        Log.i("ViewModel", "Note $noteId summarized and updated.")
                    } ?: run {
                        summarizationStatus = "Error: Note not found to update summary."
                        Log.e("ViewModel", "Note $noteId not found after summarization.")
                    }
                },
                onFailure = { exception ->
                    summarizationStatus = "Summarization failed: ${exception.message}"
                    Log.e("ViewModel", "Summarization failed for note $noteId", exception)
                }
            )
            // Clear status after a few seconds
            kotlinx.coroutines.delay(3000)
            summarizationStatus = null
        }
    }

    fun prepareForNewCapture() {
        Log.d("ViewModel", "Clearing temporary note data.")
        capturedPhotoPathForNote = null
        ocrTextForCapturedPhotoNote = null
        audioFilePathForNote = null
        transcribedTextForNote = null // Clear transcribed text too
    }

    fun setCapturedDataForNote(photoPath: String?, ocrText: String?, transcribedText: String? = null) {
        Log.d("ViewModel", "Captured data - Photo: $photoPath, OCR: ${ocrText?.take(50)}, Transcribed: ${transcribedText?.take(50)}")
        capturedPhotoPathForNote = photoPath
        ocrTextForCapturedPhotoNote = ocrText
        if (transcribedText != null) { // Only update if new transcribed text is provided
            this.transcribedTextForNote = transcribedText
        }
    }

    fun handleScreenshotProcessed(path: String, ocrText: String?) {
        Log.d("ViewModel", "Screenshot processed - Path: $path, OCR: ${ocrText?.take(50)}")
        photoPathForScreenshotDialog = path
        ocrTextFromScreenshotDialog = ocrText
        // transcribedTextForNote should be null here, it's for audio screen
        transcribedTextForNote = null
        showScreenshotAudioPromptDialog = true
    }

    fun prepareForAudioWithScreenshotData() {
        Log.d("ViewModel", "Transferring screenshot data for audio transcription.")
        capturedPhotoPathForNote = photoPathForScreenshotDialog
        ocrTextForCapturedPhotoNote = ocrTextFromScreenshotDialog
        // transcribedTextForNote is NOT set here, it will come from AudioRecordScreen
        // audioFilePathForNote should also be cleared if we are starting a new audio recording flow
        audioFilePathForNote = null
        clearScreenshotDialogData() // This will hide the dialog as we navigate
    }

    fun clearScreenshotDialogData() {
        Log.d("ViewModel", "Clearing screenshot dialog data.")
        showScreenshotAudioPromptDialog = false
        // Keep photoPathForScreenshotDialog and ocrTextFromScreenshotDialog
        // if they are needed by prepareForAudioWithScreenshotData.
        // However, if the dialog is simply dismissed to save "as is",
        // MainActivity's autoSavePhotoNote will use them and then this method is called.
        // If navigating to audio, prepareForAudioWithScreenshotData copies them first.
        // So, it's safe to nullify them here after their purpose is served.
        photoPathForScreenshotDialog = null
        ocrTextFromScreenshotDialog = null
    }

    // No onCleared needed for CloudSummarizer as it doesn't hold TFLite resources
}