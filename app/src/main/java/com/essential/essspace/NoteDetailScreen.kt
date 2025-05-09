package com.essential.essspace
import android.media.MediaPlayer
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.essential.essspace.room.Note
import com.essential.essspace.viewmodel.NotesListViewModel
import java.io.IOException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(
    noteId: Int,
    navController: NavHostController,
    notesViewModel: NotesListViewModel = viewModel() // Or pass instance
) {
    val noteState = notesViewModel.getNoteById(noteId).collectAsState(initial = null)
    val note = noteState.value
    var textContent by remember(note) { mutableStateOf(note?.text ?: "") }
    val context = LocalContext.current
    var mediaPlayer: MediaPlayer? by remember { mutableStateOf(null) }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Note Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        note?.let {
                            val updatedNote = it.copy(text = textContent)
                            notesViewModel.updateNote(updatedNote)
                            navController.popBackStack() // Or show a toast
                        }
                    }) {
                        Icon(Icons.Filled.Save, "Save Note")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (note == null) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                note.photoPath?.let { path ->
                    Image(
                        painter = rememberAsyncImagePainter(model = path),
                        contentDescription = "Note Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.height(16.dp))
                }

                note.audioPath?.let { path ->
                    Button(onClick = {
                        if (mediaPlayer?.isPlaying == true) {
                            mediaPlayer?.stop()
                            mediaPlayer?.release()
                            mediaPlayer = null
                        } else {
                            mediaPlayer?.release()
                            mediaPlayer = MediaPlayer().apply {
                                try {
                                    setDataSource(path)
                                    prepareAsync() // Use prepareAsync for network streams or large files
                                    setOnPreparedListener { start() }
                                    setOnCompletionListener {
                                        it.release()
                                        mediaPlayer = null
                                    }
                                } catch (e: IOException) {
                                    e.printStackTrace()
                                    // Handle error
                                }
                            }
                        }
                    }) {
                        Icon(Icons.Filled.PlayArrow, "Play/Stop Audio")
                        Spacer(Modifier.width(8.dp))
                        Text("Play Audio")
                    }
                    Spacer(Modifier.height(16.dp))
                }

                OutlinedTextField(
                    value = textContent,
                    onValueChange = { textContent = it },
                    label = { Text("Note Text") },
                    modifier = Modifier.fillMaxWidth().weight(1f)
                )
            }
        }
    }
}