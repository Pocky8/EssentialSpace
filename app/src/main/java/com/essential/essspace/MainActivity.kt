package com.essential.essspace

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.runtime.getValue
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.rememberUpdatedState
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.essential.essspace.room.Note
import com.essential.essspace.AudioRecordScreen
import com.essential.essspace.CameraScreen
import com.essential.essspace.ui.screens.CreateTextNoteScreen
import com.essential.essspace.HubScreen
import com.essential.essspace.NoteDetailScreen
import com.essential.essspace.ui.theme.EssentialSpaceTheme
import com.essential.essspace.GalleryImagePickerScreen
import com.essential.essspace.viewmodel.NotesListViewModel

class MainActivity : ComponentActivity() {
    private lateinit var mainNavController: NavHostController
    private lateinit var notesViewModel: NotesListViewModel

    // Receiver to capture screenshot broadcast (if still used)
    private val screenshotProcessedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ScreenshotService.ACTION_SCREENSHOT_PROCESSED) {
                val photoPath = intent.getStringExtra(ScreenshotService.EXTRA_PHOTO_PATH)
                val ocrText = intent.getStringExtra(ScreenshotService.EXTRA_OCR_TEXT)
                Log.d("MainActivity", "Received screenshot broadcast. Path: $photoPath, OCR text: ${ocrText?.take(100)}")
                if (photoPath != null) {
                    // Instead of auto-saving, trigger the ViewModel to show the dialog
                    notesViewModel.handleScreenshotProcessed(photoPath, ocrText)
                } else {
                    Toast.makeText(context, "Error processing screenshot (no path).", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        notesViewModel = ViewModelProvider(this)[NotesListViewModel::class.java]
        val filter = IntentFilter(ScreenshotService.ACTION_SCREENSHOT_PROCESSED)
        ContextCompat.registerReceiver(this, screenshotProcessedReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        Log.d("MainActivity", "Receiver registered.")

        setContent {
            EssentialSpaceTheme {
                mainNavController = rememberNavController()
                NavHost(navController = mainNavController, startDestination = Screen.Home.route) {
                    composable(Screen.Home.route) {
                        HubScreen(
                            notesViewModel = notesViewModel,
                            onNavigateToCamera = {
                                notesViewModel.prepareForNewCapture()
                                mainNavController.navigate(Screen.Camera.route)
                            },
                            onNavigateToAudio = { // This is for direct audio note
                                notesViewModel.prepareForNewCapture()
                                mainNavController.navigate(Screen.Audio.route)
                            },
                            onNavigateToGalleryImage = { // New navigation
                                notesViewModel.prepareForNewCapture()
                                mainNavController.navigate(Screen.GalleryImagePicker.route)
                            },
                            onTakeScreenshot = {
                                startActivity(Intent(this@MainActivity, ScreenshotCaptureActivity::class.java))
                            },
                            onNavigateToNoteDetail = { noteId ->
                                mainNavController.navigate(Screen.NoteDetail.createRoute(noteId))
                            },
                            onAddNewTextNote = {
                                mainNavController.navigate(Screen.CreateTextNote.route)
                            }
                        )
                    }
                    composable(Screen.Camera.route) {
                        CameraScreen(
                            onPhotoTaken = { photoPath, ocrResult ->
                                notesViewModel.setCapturedDataForNote(photoPath, ocrResult)
                                mainNavController.navigate(Screen.Audio.route) {
                                    popUpTo(Screen.Camera.route) { inclusive = true }
                                }
                            },
                            onCancel = {
                                notesViewModel.prepareForNewCapture()
                                mainNavController.popBackStack()
                            }
                        )
                    }
                    composable(Screen.GalleryImagePicker.route) { // New route
                        GalleryImagePickerScreen(
                            onImageProcessed = { imagePath, ocrText ->
                                notesViewModel.setCapturedDataForNote(imagePath, ocrText)
                                // Navigate to Audio screen for optional audio, similar to camera flow
                                mainNavController.navigate(Screen.Audio.route) {
                                    popUpTo(Screen.GalleryImagePicker.route) { inclusive = true }
                                }
                            },
                            onCancel = {
                                notesViewModel.prepareForNewCapture()
                                mainNavController.popBackStack()
                            }
                        )
                    }
                    composable(Screen.Audio.route) {
                        AudioRecordScreen(
                            photoPath = notesViewModel.capturedPhotoPathForNote,
                            onComplete = { audioPath, transcribedTextResult ->
                                val currentPhotoPath = notesViewModel.capturedPhotoPathForNote
                                val currentOcrText = notesViewModel.ocrTextForCapturedPhotoNote

                                val noteTitle = when {
                                    currentPhotoPath != null && audioPath != null -> "Photo & Audio Note"
                                    currentPhotoPath != null && !transcribedTextResult.isNullOrBlank() -> "Photo & Transcription Note" // Changed
                                    currentPhotoPath != null -> "Photo Note"
                                    audioPath != null && !transcribedTextResult.isNullOrBlank() -> "Audio & Transcription Note" // New
                                    audioPath != null -> "Audio Note"
                                    !transcribedTextResult.isNullOrBlank() -> "Transcription Note" // Changed
                                    else -> "Note"
                                }

                                val combinedText = StringBuilder()
                                if (!currentOcrText.isNullOrBlank()) {
                                    combinedText.append(currentOcrText)
                                }
                                if (!transcribedTextResult.isNullOrBlank()) {
                                    if (combinedText.isNotEmpty()) {
                                        combinedText.append("\n\n") // Use actual newlines
                                    }
                                    combinedText.append(transcribedTextResult)
                                }

                                Log.d("MainActivity", "Combined text for note: [${combinedText.toString().ifBlank { "null" }}]") // Added log

                                val newNote = Note(
                                    title = noteTitle,
                                    photoPath = currentPhotoPath,
                                    audioPath = audioPath,
                                    text = combinedText.toString().ifBlank { null }, // Use combined text
                                    transcribedText = transcribedTextResult,
                                    ocrText = currentOcrText,
                                    isMarkdown = true
                                )
                                notesViewModel.insertNote(newNote)
                                notesViewModel.prepareForNewCapture()
                                popToHome()
                            },
                            onSkipAudio = {
                                val currentPhotoPath = notesViewModel.capturedPhotoPathForNote
                                val currentOcrText = notesViewModel.ocrTextForCapturedPhotoNote
                                if (currentPhotoPath != null) { // Only save if there's a photo
                                    val noteTitle = "Photo Note"
                                    val newNote = Note(
                                        title = noteTitle,
                                        photoPath = currentPhotoPath,
                                        audioPath = null,
                                        text = currentOcrText, // Use OCR text if skipping audio
                                        transcribedText = null,
                                        ocrText = currentOcrText,
                                        isMarkdown = true // Default to true
                                    )
                                    notesViewModel.insertNote(newNote)
                                }
                                notesViewModel.prepareForNewCapture()
                                popToHome()
                            },
                            onCancel = {
                                notesViewModel.prepareForNewCapture()
                                mainNavController.popBackStack()
                            }
                        )
                    }
                    composable(
                        route = Screen.NoteDetail.route,
                        arguments = listOf(navArgument("noteId") { type = NavType.IntType })
                    ) { backStackEntry ->
                        val noteId = backStackEntry.arguments?.getInt("noteId")
                        if (noteId != null) {
                            NoteDetailScreen(noteId = noteId, navController = mainNavController, notesViewModel = notesViewModel)
                        } else {
                            Toast.makeText(this@MainActivity, "Error: Note ID not found.", Toast.LENGTH_LONG).show()
                            mainNavController.popBackStack()
                        }
                    }
                    composable(Screen.CreateTextNote.route) {
                        CreateTextNoteScreen(
                            notesViewModel = notesViewModel,
                            onNoteSaved = { popToHome() },
                            onCancel = { mainNavController.popBackStack() }
                        )
                    }
                }

                // Prompt dialog for an optionally recorded audio on a captured photo note.
                // This dialog appears if a photo was captured (from CameraScreen or screenshot) and the flag is set.
                val showDialog by rememberUpdatedState(notesViewModel.showScreenshotAudioPromptDialog)
                if (showDialog && notesViewModel.photoPathForScreenshotDialog != null) {
                    AlertDialog(
                        onDismissRequest = {
                            // If dismissed, save the note without audio.
                            if (notesViewModel.photoPathForScreenshotDialog != null) {
                                autoSavePhotoNote(notesViewModel.photoPathForScreenshotDialog!!, notesViewModel.ocrTextFromScreenshotDialog)
                            } else {
                                // Fallback if data is somehow null, just clear dialog state
                                notesViewModel.clearScreenshotDialogData()
                            }
                        },
                        title = { Text("Photo Captured") },
                        text = { Text("Do you want to add audio to this photo note?") },
                        confirmButton = {
                            TextButton(onClick = {
                                // Prepare for audio recording and navigate
                                // ViewModel will clear its own dialog data upon this action
                                notesViewModel.prepareForAudioWithScreenshotData()
                                mainNavController.navigate(Screen.Audio.route)
                            }) {
                                Text("Record Audio")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                if (notesViewModel.photoPathForScreenshotDialog != null) {
                                    autoSavePhotoNote(notesViewModel.photoPathForScreenshotDialog!!, notesViewModel.ocrTextFromScreenshotDialog)
                                } else {
                                    notesViewModel.clearScreenshotDialogData()
                                }
                            }) {
                                Text("Save As Is")
                            }
                        }
                    )
                }
            }
        }
    }

    private fun autoSavePhotoNote(photoPath: String, ocrText: String?) {
        val noteTitle = "Photo Note" // Simplified title
        val note = Note(
            title = noteTitle,
            photoPath = photoPath,
            audioPath = null,
            text = ocrText, // Use OCR text here
            transcribedText = null,
            ocrText = ocrText,
            isMarkdown = true
        )
        notesViewModel.insertNote(note)
        Toast.makeText(this, "Photo note saved.", Toast.LENGTH_SHORT).show()
        notesViewModel.clearScreenshotDialogData() // Clear ViewModel state for the dialog
        popToHome()
    }

    private fun popToHome() {
        if (::mainNavController.isInitialized) {
            mainNavController.navigate(Screen.Home.route) {
                popUpTo(mainNavController.graph.startDestinationId) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        Log.d("MainActivity", "onNewIntent called.")
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(screenshotProcessedReceiver)
        Log.d("MainActivity", "Receiver unregistered.")
    }
}

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Camera : Screen("camera")
    object GalleryImagePicker : Screen("galleryImagePicker") // New screen route
    object Audio : Screen("audio")
    object NoteDetail : Screen("noteDetail/{noteId}") {
        fun createRoute(noteId: Int) = "noteDetail/$noteId"
    }
    object CreateTextNote : Screen("createTextNote")
}