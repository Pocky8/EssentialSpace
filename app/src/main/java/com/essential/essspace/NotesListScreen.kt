package com.essential.essspace

import android.media.MediaPlayer
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
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
    viewModel: NotesListViewModel = viewModel()
) {
    val notes by viewModel.notes.collectAsState()
    val loading by viewModel.isLoading.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadNotes() }

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("Essential Space") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddNewNote) {
                Icon(Icons.Default.Add, null)
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                loading -> {
                    Text(
                        "Loading...",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                notes.isEmpty() -> {
                    Column(
                        Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No notes found!", style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.height(8.dp))
                        Text("Tap + to add one.", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                else -> {
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(notes) { note ->
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

    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US).format(Date(note.timestamp)),
                style = MaterialTheme.typography.labelSmall
            )
            note.photoPath?.let { path ->
                File(path).takeIf { it.exists() }?.let { file ->
                    Image(
                        painter = rememberAsyncImagePainter(file),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .padding(vertical = 8.dp),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            note.audioPath?.let { ap ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton({
                        if (playing) {
                            mediaPlayer.stop(); playing = false
                        } else {
                            try {
                                mediaPlayer.reset()
                                mediaPlayer.setDataSource(ap)
                                mediaPlayer.prepare()
                                mediaPlayer.start()
                                playing = true
                                mediaPlayer.setOnCompletionListener { playing = false }
                            } catch (e: Exception) {
                                Log.e("NoteCard","play failed",e)
                            }
                        }
                    }) {
                        Icon(Icons.Default.PlayArrow, null)
                    }
                    Text(if (playing) "Playing..." else "Play audio")
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onDelete) {
                    Icon(Icons.Default.Delete, null, tint = Color.Red)
                }
            }
        }
    }
}