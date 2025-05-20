package com.essential.essspace

import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.essential.essspace.room.Note
import com.essential.essspace.viewmodel.NotesListViewModel
import dev.jeziellago.compose.markdowntext.MarkdownText
import java.io.File // Import File
import java.io.IOException
import java.util.regex.Matcher

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(
    noteId: Int,
    navController: NavHostController,
    notesViewModel: NotesListViewModel = viewModel()
) {
    val noteState = notesViewModel.getNoteByIdFlow(noteId).collectAsState(initial = null)
    val note = noteState.value
    var textContent by remember(note) { mutableStateOf(note?.text ?: "") }
    val context = LocalContext.current
    var mediaPlayer: MediaPlayer? by remember { mutableStateOf(null) }
    var isPlayingAudio by remember { mutableStateOf(false) }
    var isEditingContent by remember(note) { mutableStateOf(false) } // Renamed from isEditingMarkdown

    // Ensure these state variables are defined at the top level of the Composable
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showFullScreenImageDialog by remember { mutableStateOf(false) }
    var imagePathForDialog by remember { mutableStateOf<String?>(null) }

    val uriHandler = LocalUriHandler.current

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    fun playAudio(audioPath: String) {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer().apply {
                try {
                    setDataSource(audioPath)
                    prepareAsync() // Prepare asynchronously to not block UI
                    setOnPreparedListener {
                        start()
                        isPlayingAudio = true
                        Log.d("NoteDetailScreen", "MediaPlayer prepared and started.")
                    }
                    setOnCompletionListener {
                        isPlayingAudio = false
                        // Consider releasing mediaPlayer here or allowing replay
                        // For now, just update state
                        Log.d("NoteDetailScreen", "MediaPlayer playback completed.")
                    }
                    setOnErrorListener { mp, what, extra ->
                        Log.e("NoteDetailScreen", "MediaPlayer error: what: $what, extra: $extra")
                        isPlayingAudio = false
                        Toast.makeText(context, "Error playing audio.", Toast.LENGTH_SHORT).show()
                        // Optionally release and nullify mediaPlayer here
                        true // Error handled
                    }
                } catch (e: IOException) {
                    Log.e("NoteDetailScreen", "MediaPlayer IOException: Failed to set data source or prepare", e)
                    Toast.makeText(context, "Error initializing audio player.", Toast.LENGTH_SHORT).show()
                    mediaPlayer = null // Reset on error
                    isPlayingAudio = false
                }
            }
        } else {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
                isPlayingAudio = false
                Log.d("NoteDetailScreen", "MediaPlayer paused.")
            } else {
                mediaPlayer?.start()
                isPlayingAudio = true
                Log.d("NoteDetailScreen", "MediaPlayer resumed.")
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(note?.title ?: "Note Details") },
                navigationIcon = {
                    IconButton(onClick = {
                        // If editing, consider prompting to save changes or automatically save
                        if (isEditingContent) {
                            note?.let {
                                val updatedNote = it.copy(text = textContent, lastModified = System.currentTimeMillis())
                                notesViewModel.updateNote(updatedNote)
                            }
                        }
                        navController.popBackStack()
                    }) {
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (note != null) { // Show edit/done only if note is loaded
                        IconButton(onClick = { isEditingContent = !isEditingContent }) {
                            Icon(
                                imageVector = if (isEditingContent) Icons.Filled.Done else Icons.Filled.Edit,
                                contentDescription = if (isEditingContent) "Done Editing" else "Edit Content"
                            )
                        }
                    }
                    IconButton(onClick = {
                        note?.let {
                            val updatedNote = it.copy(text = textContent, lastModified = System.currentTimeMillis())
                            notesViewModel.updateNote(updatedNote)
                            isEditingContent = false // Exit edit mode on save
                            // navController.popBackStack() // Optional: pop back after save
                            Toast.makeText(context, "Note Saved", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(Icons.Filled.Save, "Save Note")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (note == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues), contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Log.d("NoteDetailScreen", "Note loaded: ID=${note.id}, Title='${note.title}', PhotoPath='${note.photoPath}', AudioPath='${note.audioPath}', OCR='${note.ocrText?.take(50)}', Transcribed='${note.transcribedText?.take(50)}'")
                // Media content (Image/Audio)
                note.photoPath?.let { path ->
                    val file = File(path)
                    if (file.exists()) {
                        Image(
                            painter = rememberAsyncImagePainter(model = file),
                            contentDescription = "Note Image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 300.dp) // Constrain height in the detail view
                                .clickable {
                                    imagePathForDialog = path
                                    showFullScreenImageDialog = true
                                },
                            contentScale = ContentScale.Fit
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    } else {
                        Text("Photo not found at path: $path", color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // Display combined text (OCR and/or Transcription)
                val textContent = remember(note.text, note.ocrText, note.transcribedText) {
                    note.text ?: "" // note.text should already be the combined content
                }

                if (textContent.isNotBlank()) {
                    MarkdownText(
                        markdown = textContent,
                        color = MaterialTheme.colorScheme.onBackground, // Explicitly set text color
                        fontSize = 16.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }


                if (note.links.isNotEmpty()) {
                    Text("Links:", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    note.links.forEach { link ->
                        TextButton(onClick = {
                            try {
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(link))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(context, "Could not open link", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Text(link, fontSize = 14.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }


                note.audioPath?.let { path ->
                    Text("Audio Recording:", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    // Basic placeholder for audio playback functionality
                    Button(onClick = {
                        // TODO: Implement audio playback
                        android.widget.Toast.makeText(context, "Playback not yet implemented.", android.widget.Toast.LENGTH_SHORT).show()
                    }) {
                        Text("Play Audio: ${File(path).name}")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    if (showDeleteConfirmDialog && note != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Note") },
            text = { Text("Are you sure you want to delete this note?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        notesViewModel.deleteNote(note)
                        showDeleteConfirmDialog = false
                        navController.popBackStack()
                    }
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showFullScreenImageDialog && imagePathForDialog != null) {
        Dialog(onDismissRequest = { showFullScreenImageDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f)) // Semi-transparent background
                    .clickable { showFullScreenImageDialog = false }, // Dismiss on background click
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = rememberAsyncImagePainter(model = File(imagePathForDialog!!)),
                    contentDescription = "Full Screen Image",
                    modifier = Modifier
                        .fillMaxWidth(0.95f) // Use most of the width
                        .fillMaxHeight(0.85f), // Use most of the height
                    contentScale = ContentScale.Fit // Fit the image within the bounds
                )
            }
        }
    }
}