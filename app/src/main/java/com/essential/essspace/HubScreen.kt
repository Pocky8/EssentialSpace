package com.essential.essspace // Or your ui.screens package

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.essential.essspace.NotesListScreen
import com.essential.essspace.viewmodel.NotesListViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HubScreen(
    notesViewModel: NotesListViewModel,
    onNavigateToCamera: () -> Unit,
    onNavigateToAudio: () -> Unit,
    onTakeScreenshot: () -> Unit,
    onNavigateToGalleryImage: () -> Unit, // New callback
    onNavigateToNoteDetail: (Int) -> Unit,
    onAddNewTextNote: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showBottomSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Essspace Notes") }
                // Actions for the top bar icon were removed previously
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showBottomSheet = true }) {
                Icon(Icons.Filled.Add, "Add Note")
            }
        }
    ) { paddingValues ->
        NotesListScreen(
            notesViewModel = notesViewModel,
            onNoteClick = onNavigateToNoteDetail,
            modifier = Modifier.padding(paddingValues)
        )

        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                sheetState = sheetState,
                windowInsets = WindowInsets(0.dp) // Consume insets
            ) {
                CaptureOptionsMenuSheet(
                    onDismiss = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) { showBottomSheet = false }
                        }
                    },
                    onTextNoteClick = onAddNewTextNote,
                    onCameraClick = onNavigateToCamera,
                    onAudioClick = onNavigateToAudio,
                    onScreenshotClick = onTakeScreenshot,
                    onGalleryImageClick = onNavigateToGalleryImage // New parameter
                )
            }
        }
    }
}

@Composable
fun CaptureOptionsMenuSheet(
    onDismiss: () -> Unit,
    onTextNoteClick: () -> Unit,
    onCameraClick: () -> Unit,
    onAudioClick: () -> Unit,
    onScreenshotClick: () -> Unit,
    onGalleryImageClick: () -> Unit // New parameter
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Create New", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 16.dp))

        Button(onClick = { onTextNoteClick(); onDismiss() }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Icon(Icons.Filled.TextFields, "Text Note", modifier = Modifier.padding(end = 8.dp))
            Text("Text Note")
        }
        Button(onClick = { onCameraClick(); onDismiss() }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Icon(Icons.Filled.PhotoCamera, "Photo", modifier = Modifier.padding(end = 8.dp))
            Text("Photo + Optional Audio")
        }
        Button(onClick = { onGalleryImageClick(); onDismiss() }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) { // New Button
            Icon(Icons.Filled.Image, "Gallery Image", modifier = Modifier.padding(end = 8.dp))
            Text("Image from Gallery + OCR")
        }
        Button(onClick = { onAudioClick(); onDismiss() }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Icon(Icons.Filled.Mic, "Audio", modifier = Modifier.padding(end = 8.dp))
            Text("Audio Recording")
        }
        Button(onClick = { onScreenshotClick(); onDismiss() }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Icon(Icons.Filled.Screenshot, "Screenshot", modifier = Modifier.padding(end = 8.dp))
            Text("Screenshot + Optional Audio")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text("Cancel")
        }
        Spacer(modifier = Modifier.height(16.dp)) // For system navigation bar
    }
}