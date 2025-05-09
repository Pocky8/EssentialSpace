package com.essential.essspace

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.essential.essspace.room.Note
import com.essential.essspace.ui.screens.CreateTextNoteScreen
import com.essential.essspace.ui.theme.EssentialSpaceTheme
import com.essential.essspace.viewmodel.NotesListViewModel

class MainActivity : ComponentActivity() {
    private lateinit var mainNavController: NavHostController
    private lateinit var notesViewModel: NotesListViewModel

    // State for data passed between screens (photo capture flow OR screenshot-to-audio flow)
    private var capturedPhotoPath: String? by mutableStateOf(null)
    private var ocrTextForCapturedPhoto: String? by mutableStateOf(null) // Holds OCR from camera OR screenshot if going to audio

    // State specifically for screenshot flow before dialog decision
    private var showAudioPromptDialog by mutableStateOf(false)
    private var photoPathForScreenshotDialog: String? by mutableStateOf(null)
    private var ocrTextFromScreenshotForDialog: String? by mutableStateOf(null)

    private val screenshotReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ScreenshotService.ACTION_SCREENSHOT_TAKEN_PROMPT_AUDIO) {
                val path = intent.getStringExtra("photoPath")
                val ocr = intent.getStringExtra("ocrText")
                if (path != null) {
                    Log.d("MainActivity", "BroadcastReceiver: Screenshot path: $path, OCR: ${ocr?.take(50)}")
                    photoPathForScreenshotDialog = path
                    ocrTextFromScreenshotForDialog = ocr
                    showAudioPromptDialog = true
                } else {
                    Log.e("MainActivity", "BroadcastReceiver: ACTION_SCREENSHOT_TAKEN_PROMPT_AUDIO but path is null")
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        notesViewModel = ViewModelProvider(this)[NotesListViewModel::class.java]

        val intentFilter = IntentFilter(ScreenshotService.ACTION_SCREENSHOT_TAKEN_PROMPT_AUDIO)
        // Use ContextCompat.registerReceiver with RECEIVER_NOT_EXPORTED for internal broadcasts.
        ContextCompat.registerReceiver(
            /* context = */ this,
            /* receiver = */ screenshotReceiver,
            /* filter = */ intentFilter,
            /* flags = */ ContextCompat.RECEIVER_NOT_EXPORTED
        )

        setContent {
            EssentialSpaceTheme {
                mainNavController = rememberNavController()

                NavHost(navController = mainNavController, startDestination = Screen.Home.route) {
                    composable(Screen.Home.route) {
                        HubScreen(
                            notesViewModel = notesViewModel,
                            onNavigateToCamera = {
                                clearTemporaryNoteData() // Clear any previous data
                                mainNavController.navigate(Screen.Camera.route)
                            },
                            onNavigateToAudio = {
                                clearTemporaryNoteData() // Reset photo data if going directly to audio
                                mainNavController.navigate(Screen.Audio.route)
                            },
                            onTakeScreenshot = {
                                Toast.makeText(this@MainActivity, "Starting screenshot capture...", Toast.LENGTH_SHORT).show()
                                val intent = Intent(this@MainActivity, ScreenshotCaptureActivity::class.java)
                                startActivity(intent)
                            },
                            onNavigateToNoteDetail = { noteId ->
                                mainNavController.navigate(Screen.NoteDetail.createRoute(noteId))
                            },
                            onAddNewTextNote = { mainNavController.navigate(Screen.CreateTextNote.route) }
                        )
                    }
                    composable(Screen.Camera.route) {
                        CameraScreen(
                            onPhotoTaken = { photoPath, ocrResult -> // Already expects two params
                                capturedPhotoPath = photoPath
                                ocrTextForCapturedPhoto = ocrResult // This will now receive OCR from CameraScreen
                                mainNavController.navigate(Screen.Audio.route)
                            },
                            onCancel = {
                                mainNavController.popBackStack()
                            }
                        )
                    }
                    composable(Screen.Audio.route) {
                        AudioRecordScreen(
                            photoPath = capturedPhotoPath, // This will be from camera OR screenshot
                            onComplete = { _, transcribedTextResult -> // audioPath is always null from AudioRecordScreen
                                val noteTitle: String
                                val combinedNoteTextBuilder = StringBuilder()

                                if (capturedPhotoPath != null) { // Indicates it's for a photo/screenshot
                                    noteTitle = "Note for Image"
                                    // Append OCR text if available
                                    if (!ocrTextForCapturedPhoto.isNullOrBlank()) {
                                        combinedNoteTextBuilder.append(ocrTextForCapturedPhoto)
                                    }

                                    // Append transcribed text if available, adding a separator if OCR was also present
                                    if (!transcribedTextResult.isNullOrBlank()) {
                                        if (combinedNoteTextBuilder.isNotEmpty()) {
                                            combinedNoteTextBuilder.append("\n\n---\n\n") // Separator
                                        }
                                        combinedNoteTextBuilder.append(transcribedTextResult)
                                    }
                                } else { // Pure audio note
                                    noteTitle = if (!transcribedTextResult.isNullOrBlank()) {
                                        "Transcribed Note"
                                    } else {
                                        "Audio Note"
                                    }
                                    if (!transcribedTextResult.isNullOrBlank()) {
                                        combinedNoteTextBuilder.append(transcribedTextResult)
                                    }
                                }

                                val finalNoteText = if (combinedNoteTextBuilder.toString().isBlank()) null else combinedNoteTextBuilder.toString()

                                val newNote = Note(
                                    title = noteTitle,
                                    photoPath = capturedPhotoPath,
                                    audioPath = null,
                                    text = finalNoteText, // Combined OCR and Transcription
                                    transcribedText = transcribedTextResult,
                                    ocrText = if (capturedPhotoPath != null) ocrTextForCapturedPhoto else null
                                )
                                notesViewModel.insertNote(newNote)
                                clearTemporaryNoteData()
                                popToHome()
                            },
                            onCancel = {
                                clearTemporaryNoteData()
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
                            NoteDetailScreen(
                                noteId = noteId,
                                navController = mainNavController,
                                notesViewModel = notesViewModel
                            )
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

                if (showAudioPromptDialog && photoPathForScreenshotDialog != null) {
                    AlertDialog(
                        onDismissRequest = {
                            // If dismissed (e.g., back button or touch outside), save as is
                            saveScreenshotNoteAsIs()
                            clearScreenshotDialogData()
                        },
                        title = { Text("Screenshot Captured") },
                        // Updated text: Does not show OCR content
                        text = { Text("Do you want to add audio to this screenshot note?") },
                        confirmButton = {
                            TextButton(onClick = {
                                // User wants to record audio for the screenshot
                                capturedPhotoPath = photoPathForScreenshotDialog
                                // Pass the screenshot's OCR text to the audio recording flow
                                ocrTextForCapturedPhoto = ocrTextFromScreenshotForDialog
                                mainNavController.navigate(Screen.Audio.route)
                                clearScreenshotDialogData()
                            }) { Text("Record Audio") }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                saveScreenshotNoteAsIs()
                                clearScreenshotDialogData()
                            }) { Text("Save As Is") }
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.d("MainActivity", "onNewIntent called.")
    }

    private fun saveScreenshotNoteAsIs() {
        if (photoPathForScreenshotDialog == null) {
            Log.e("MainActivity", "saveScreenshotNoteAsIs: photoPathForScreenshotDialog is null")
            return
        }

        val note = Note(
            title = "Screenshot Note",
            photoPath = photoPathForScreenshotDialog,
            audioPath = null,
            // OCR text from screenshot becomes the main content
            text = ocrTextFromScreenshotForDialog,
            transcribedText = null,
            // Also store the OCR text in its dedicated field
            ocrText = ocrTextFromScreenshotForDialog
        )
        notesViewModel.insertNote(note)
        Toast.makeText(this, "Screenshot note saved.", Toast.LENGTH_SHORT).show()
        popToHome()
    }

    private fun clearTemporaryNoteData() {
        // This is for data used by CameraScreen -> AudioRecordScreen or Screenshot -> AudioRecordScreen
        capturedPhotoPath = null
        ocrTextForCapturedPhoto = null
    }

    private fun clearScreenshotDialogData() {
        // This is for data used specifically by the screenshot dialog
        photoPathForScreenshotDialog = null
        ocrTextFromScreenshotForDialog = null
        showAudioPromptDialog = false
    }

    private fun popToHome() {
        if (::mainNavController.isInitialized) {
            mainNavController.navigate(Screen.Home.route) {
                popUpTo(mainNavController.graph.startDestinationId) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(screenshotReceiver)
        Log.d("MainActivity", "onDestroy: Unregistered screenshotReceiver.")
    }
}

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Camera : Screen("camera")
    object Audio : Screen("audio")
    object NoteDetail : Screen("noteDetail/{noteId}") {
        fun createRoute(noteId: Int) = "noteDetail/$noteId"
    }
    object CreateTextNote : Screen("createTextNote")
}