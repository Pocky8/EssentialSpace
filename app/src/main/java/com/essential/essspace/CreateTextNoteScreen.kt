package com.essential.essspace.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.essential.essspace.room.Note
import com.essential.essspace.viewmodel.NotesListViewModel
import kotlinx.coroutines.job

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTextNoteScreen(
    notesViewModel: NotesListViewModel,
    onNoteSaved: () -> Unit,
    onCancel: () -> Unit
) {
    var titleState by remember { mutableStateOf("") }
    var contentState by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    val saveNote = {
        if (titleState.isNotBlank() || contentState.isNotBlank()) {
            val newNote = Note(
                title = titleState.ifBlank { null }, // Save null if title is blank
                text = contentState.ifBlank { null }, // Save null if content is blank
                photoPath = null,
                audioPath = null
            )
            notesViewModel.insertNote(newNote)
            onNoteSaved()
        } else {
            onCancel() // If both are blank, just cancel
        }
    }

    BackHandler {
        saveNote()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Note") },
                navigationIcon = {
                    IconButton(onClick = {
                        // Save on explicit back navigation from top bar too
                        saveNote()
                    }) {
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = saveNote) {
                        Icon(Icons.Filled.Done, "Save Note")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Title TextField
            BasicTextField(
                value = titleState,
                onValueChange = { titleState = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                textStyle = MaterialTheme.typography.headlineSmall.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                singleLine = true,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { innerTextField ->
                    Box(modifier = Modifier.fillMaxWidth()) {
                        if (titleState.isEmpty()) {
                            Text(
                                "Title",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                        innerTextField()
                    }
                }
            )

            Divider()

            // Content BasicTextField (seamless)
            BasicTextField(
                value = contentState,
                onValueChange = { contentState = it },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 8.dp)
                    .focusRequester(focusRequester),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { innerTextField ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (contentState.isEmpty()) {
                            Text(
                                "Start writing your note here...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                        innerTextField()
                    }
                }
            )
            // Request focus on the content field when the screen appears
            LaunchedEffect(Unit) {
                coroutineContext.job.invokeOnCompletion { // Ensure this runs after composition
                    focusRequester.requestFocus()
                }
            }
        }
    }
}