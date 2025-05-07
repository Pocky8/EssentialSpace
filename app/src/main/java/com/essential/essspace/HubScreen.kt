package com.essential.essspace

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add // FAB in NotesListScreen
import androidx.compose.material.icons.filled.AudioFile // Example Icon
import androidx.compose.material.icons.filled.CameraAlt // Example Icon
import androidx.compose.material.icons.filled.Close // Example Icon
import androidx.compose.material.icons.filled.Menu // FAB in HubScreen
import androidx.compose.material.icons.filled.Screenshot // Example Icon
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HubScreen(
    onNavigateToCamera: () -> Unit,
    onNavigateToAudio: () -> Unit,
    onTakeScreenshot: () -> Unit,
    onAddNewNoteFromHub: () -> Unit, // This is for the FAB in NotesListScreen
    onScreenshotIconClick: () -> Unit // This is for the top-bar icon in NotesListScreen
) {
    var showCaptureMenu by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showCaptureMenu = true }) {
                Icon(Icons.Filled.Menu, contentDescription = "Open Capture Options")
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            NotesListScreen( // Assuming NotesListScreen is defined elsewhere
                onAddNewNote = onAddNewNoteFromHub,
                onScreenshot = onScreenshotIconClick // Pass the specific screenshot action for NotesListScreen's top bar
            )
        }

        if (showCaptureMenu) {
            ModalBottomSheet(
                onDismissRequest = { showCaptureMenu = false },
                sheetState = sheetState,
            ) {
                CaptureOptionsMenuSheet(
                    onCameraClick = {
                        showCaptureMenu = false
                        onNavigateToCamera()
                    },
                    onAudioClick = {
                        showCaptureMenu = false
                        onNavigateToAudio()
                    },
                    onScreenshotClick = {
                        showCaptureMenu = false
                        onTakeScreenshot()
                    },
                    onDismiss = { showCaptureMenu = false }
                )
            }
        }
    }
}

@Composable
fun CaptureOptionsMenuSheet(
    onCameraClick: () -> Unit,
    onAudioClick: () -> Unit,
    onScreenshotClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Capture Options", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onCameraClick, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Icon(Icons.Filled.CameraAlt, contentDescription = "Camera", modifier = Modifier.padding(end = 8.dp))
            Text("Camera")
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = onAudioClick, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Icon(Icons.Filled.AudioFile, contentDescription = "Audio", modifier = Modifier.padding(end = 8.dp))
            Text("Audio Only")
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = onScreenshotClick, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Icon(Icons.Filled.Screenshot, contentDescription = "Screenshot", modifier = Modifier.padding(end = 8.dp))
            Text("Screenshot")
        }
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onDismiss) {
            Icon(Icons.Filled.Close, contentDescription = "Dismiss", modifier = Modifier.padding(end = 8.dp))
            Text("Back to Hub")
        }
    }
}