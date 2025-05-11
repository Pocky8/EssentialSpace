package com.essential.essspace
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.essential.essspace.room.Note
import com.essential.essspace.viewmodel.NotesListViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NotesListScreen(
    notesViewModel: NotesListViewModel,
    onNoteClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val notesList by notesViewModel.getAllNotes().collectAsState(initial = emptyList())

    if (notesList.isEmpty()) {
        Box(modifier = modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            Text("No notes yet. Tap '+' to add something!")
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(all = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(notesList, key = { note -> note.id }) { note ->
                NoteCard(
                    note = note,
                    onClick = { onNoteClick(note.id) },
                    onDeleteClick = { notesViewModel.deleteNoteById(note.id) }
                )
            }
        }
    }
}

@Composable
fun NoteCard(note: Note, onClick: () -> Unit, onDeleteClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            var hasPrimaryContent = false
            var contentDisplayed = false // To track if any primary content (image, text, audio) is shown

            // Display Image if available
            if (!note.photoPath.isNullOrEmpty()) {
                Image(
                    painter = rememberAsyncImagePainter(model = note.photoPath),
                    contentDescription = "Note image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f) // Maintain aspect ratio for consistent look
                        .heightIn(min = 100.dp, max = 180.dp), // Constrain height
                    contentScale = ContentScale.Crop // Crop to fill bounds, good for quality
                )
                Spacer(Modifier.height(8.dp))
                contentDisplayed = true
            }

            if (!note.title.isNullOrBlank()) {
                Text(
                    text = note.title!!,
                    style = MaterialTheme.typography.titleLarge, // Make title more prominent
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(if (note.text.isNullOrBlank()) 0.dp else 4.dp)) // Smaller spacer if no content
                hasPrimaryContent = true
            }

            // Display Text (content) if available
            if (!note.text.isNullOrBlank()) {
                Text(
                    text = note.text!!,
                    style = MaterialTheme.typography.bodyMedium, // Content style
                    maxLines = if (note.photoPath.isNullOrEmpty() && note.title.isNullOrBlank()) 8 else 4,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                hasPrimaryContent = true
            }

            // Display Audio indication if available
            if (!note.audioPath.isNullOrEmpty()) {
                Text(
                    text = "Audio Attached",
                    style = MaterialTheme.typography.bodySmall, // Smaller for indication
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(Modifier.height(4.dp))
                hasPrimaryContent = true // Audio is also primary content
            }

            // If no title, no specific text content, and no image/audio, show "Untitled Note"
            if (!hasPrimaryContent) {
                Text(
                    text = "Untitled Note",
                    style = MaterialTheme.typography.titleMedium.copy(fontStyle = FontStyle.Italic),
                    color = Color.Gray
                )
                Spacer(Modifier.height(4.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault()).format(Date(note.timestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Note",
                        tint = MaterialTheme.colorScheme.error // Use theme color for error
                    )
                }
            }
        }
    }
}