package com.essential.essspace

import android.media.MediaPlayer
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add // Correct Import
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu // Correct Import
import androidx.compose.material.icons.filled.PlayArrow
// For a more specific screenshot icon, consider:
// import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.essential.essspace.room.Note
import com.essential.essspace.viewmodel.NotesListViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesListScreen(
    onAddNewNote: () -> Unit,
    onScreenshot: () -> Unit,
    viewModel: NotesListViewModel = viewModel()
) {
    val notes by viewModel.notes.collectAsState()
    val loading by viewModel.isLoading.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadNotes() }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Essential Space") },
                actions = {
                    IconButton(onClick = onScreenshot) {
                        // Using Menu icon for screenshot action as per your previous code.
                        // Consider Icons.Filled.CameraAlt or similar for better UX.
                        Icon(Icons.Filled.Menu, contentDescription = "Take Screenshot")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddNewNote) {
                Icon(Icons.Filled.Add, contentDescription = "Add New Note")
            }
        }
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                notes.isEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No notes found!", style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.height(8.dp))
                        Text("Tap + to add one.", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(notes, key = { note -> note.id }) { note ->
                            NoteCard(note) { viewModel.deleteNote(note.id.toLong()) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteCard(note: Note, onDelete: () -> Unit) {
    val mediaPlayer = remember { MediaPlayer() }
    var playing by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            if (mediaPlayer.isPlaying) {
                mediaPlayer.stop()
            }
            mediaPlayer.release()
        }
    }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US).format(Date(note.timestamp)),
                style = MaterialTheme.typography.labelSmall
            )

            note.photoPath?.let { path ->
                val file = File(path)
                if (file.exists()) {
                    Image(
                        painter = rememberAsyncImagePainter(model = file),
                        contentDescription = "Note Photo",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .padding(vertical = 8.dp),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Log.w("NoteCard", "Photo file not found: $path")
                    // Optionally display a placeholder or error message
                }
            }

            note.audioPath?.let { audioPath ->
                val audioFile = File(audioPath)
                if (audioFile.exists()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            if (playing) {
                                if (mediaPlayer.isPlaying) {
                                    mediaPlayer.stop()
                                }
                                mediaPlayer.reset()
                                playing = false
                            } else {
                                try {
                                    mediaPlayer.reset()
                                    mediaPlayer.setDataSource(audioPath)
                                    mediaPlayer.prepareAsync() // Use async preparation
                                    mediaPlayer.setOnPreparedListener { mp ->
                                        mp.start()
                                        playing = true
                                    }
                                    mediaPlayer.setOnCompletionListener { mp ->
                                        playing = false
                                        mp.reset()
                                    }
                                    mediaPlayer.setOnErrorListener { mp, what, extra ->
                                        Log.e("NoteCard", "MediaPlayer Error: what $what, extra $extra for $audioPath")
                                        playing = false
                                        mp.reset()
                                        true // Indicates the error was handled
                                    }
                                } catch (e: Exception) {
                                    Log.e("NoteCard", "MediaPlayer setup failed for $audioPath", e)
                                    playing = false
                                }
                            }
                        }) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = "Play Audio")
                        }
                        Text(text = if (playing) "Playing..." else "Play audio")
                    }
                } else {
                    Log.w("NoteCard", "Audio file not found: $audioPath")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete Note", tint = Color.Red)
                }
            }
        }
    }
}