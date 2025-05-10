package com.essential.essspace

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.* // Keep this for other composable states if any
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
    private var capturedPhotoPath: String? = null
    private lateinit var notesViewModel: NotesListViewModel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        notesViewModel = ViewModelProvider(this)[NotesListViewModel::class.java]

        setContent {
            EssentialSpaceTheme {
                mainNavController = rememberNavController()

                // Observe ViewModel state for dialog and capture flow
                val showDialog = notesViewModel.showScreenshotAudioPromptDialog
                val photoPathForDialog = notesViewModel.photoPathForScreenshotDialog // Used in AlertDialog logic
                // val ocrTextForDialog = notesViewModel.ocrTextFromScreenshotDialog // Used in saveScreenshotNoteAsIs

                NavHost(navController = mainNavController, startDestination = Screen.Home.route) {
                    composable(Screen.Home.route) {
                        HubScreen(
                            notesViewModel = notesViewModel,
                            onNavigateToCamera = {
                                notesViewModel.prepareForNewCapture()
                                mainNavController.navigate(Screen.Camera.route)
                            },
                            onNavigateToAudio = {
                                notesViewModel.prepareForNewCapture()
                                mainNavController.navigate(Screen.Audio.route)
                            },
                            onTakeScreenshot = {
                                // Directly start ScreenshotCaptureActivity
                                startActivity(Intent(this@MainActivity, ScreenshotCaptureActivity::class.java))
                            },

                            onNavigateToNoteDetail = { noteId ->
                                mainNavController.navigate(Screen.NoteDetail.createRoute(noteId))
                            },
                            onAddNewTextNote = { mainNavController.navigate(Screen.CreateTextNote.route) }
                        )
                    }
                    composable(Screen.Camera.route) {
                        CameraScreen(
                            onPhotoTaken = { photoPath, ocrResult ->
                                notesViewModel.setCapturedDataForNote(photoPath, ocrResult)
                                mainNavController.navigate(Screen.Audio.route)
                            },
                            onCancel = {
                                notesViewModel.prepareForNewCapture() // Clear data on cancel
                                mainNavController.popBackStack()
                            }
                        )
                    }
                    composable(Screen.Audio.route) {
                        AudioRecordScreen(
                            photoPath = notesViewModel.capturedPhotoPathForNote, // Read from ViewModel
                            onComplete = { _, transcribedTextResult ->
                                val noteTitle: String
                                val combinedNoteTextBuilder = StringBuilder()

                                val currentCapturedPhotoPath = notesViewModel.capturedPhotoPathForNote
                                val currentOcrText = notesViewModel.ocrTextForCapturedPhotoNote

                                if (currentCapturedPhotoPath != null) {
                                    noteTitle = "Note for Image"
                                    if (!currentOcrText.isNullOrBlank()) {
                                        combinedNoteTextBuilder.append(currentOcrText)
                                    }
                                    if (!transcribedTextResult.isNullOrBlank()) {
                                        if (combinedNoteTextBuilder.isNotEmpty()) {
                                            combinedNoteTextBuilder.append("\n\n---\n\n")
                                        }
                                        combinedNoteTextBuilder.append(transcribedTextResult)
                                    }
                                } else {
                                    noteTitle = if (!transcribedTextResult.isNullOrBlank()) "Transcribed Note" else "Audio Note"
                                    if (!transcribedTextResult.isNullOrBlank()) {
                                        combinedNoteTextBuilder.append(transcribedTextResult)
                                    }
                                }

                                val finalNoteText = if (combinedNoteTextBuilder.toString().isBlank()) null else combinedNoteTextBuilder.toString()

                                val newNote = Note(
                                    title = noteTitle,
                                    photoPath = currentCapturedPhotoPath,
                                    audioPath = null, // AudioRecordScreen doesn't provide audioPath
                                    text = finalNoteText,
                                    transcribedText = transcribedTextResult,
                                    ocrText = if (currentCapturedPhotoPath != null) currentOcrText else null
                                )
                                notesViewModel.insertNote(newNote)
                                notesViewModel.prepareForNewCapture() // Clear data after saving
                                popToHome()
                            },
                            onCancel = {
                                notesViewModel.prepareForNewCapture() // Clear data on cancel
                                mainNavController.popBackStack()
                            }
                        )
                    }
                    // ... (NoteDetail and CreateTextNote composables remain similar)
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

                if (showDialog && photoPathForDialog != null) {
                    Log.d("MainActivity", "AlertDialog: Composing. OCR for dialog from ViewModel: ${notesViewModel.ocrTextFromScreenshotDialog?.take(100)}")
                    AlertDialog(
                        onDismissRequest = {
                            Log.d("MainActivity", "AlertDialog: Dismissed.")
                            saveScreenshotNoteAsIs() // Uses ViewModel data internally now
                            // notesViewModel.clearScreenshotDialogData() // Done in saveScreenshotNoteAsIs or if navigating
                        },
                        title = { Text("Screenshot Captured") },
                        text = { Text("Do you want to add audio to this screenshot note?") },
                        confirmButton = {
                            TextButton(onClick = {
                                Log.d("MainActivity", "AlertDialog: 'Record Audio' clicked.")
                                notesViewModel.prepareForAudioWithScreenshotData() // This sets capturedPhotoPathForNote & ocrTextForCapturedPhotoNote and clears dialog data
                                mainNavController.navigate(Screen.Audio.route)
                                // notesViewModel.clearScreenshotDialogData() // Now handled by prepareForAudioWithScreenshotData
                            }) { Text("Record Audio") }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                Log.d("MainActivity", "AlertDialog: 'Save As Is' clicked.")
                                saveScreenshotNoteAsIs() // Uses ViewModel data internally now
                                // notesViewModel.clearScreenshotDialogData() // Done in saveScreenshotNoteAsIs
                            }) { Text("Save As Is") }
                        }
                    )
                }
            }
        }
    }

    // ... onNewIntent
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        Log.d("MainActivity", "onNewIntent called.")
    }

    private fun saveScreenshotNoteAsIs() {
        val path = notesViewModel.photoPathForScreenshotDialog
        val ocr = notesViewModel.ocrTextFromScreenshotDialog

        if (path == null) {
            Log.e("MainActivity", "saveScreenshotNoteAsIs: photoPathForScreenshotDialog from ViewModel is null")
            notesViewModel.clearScreenshotDialogData() // Clear even if path is null
            return
        }

        val note = Note(
            title = "Screenshot Note",
            photoPath = path,
            audioPath = null,
            text = ocr,
            transcribedText = null,
            ocrText = ocr
        )
        notesViewModel.insertNote(note)
        Toast.makeText(this, "Screenshot note saved.", Toast.LENGTH_SHORT).show()
        notesViewModel.clearScreenshotDialogData() // Clear data after saving
        popToHome()
    }

    // REMOVE clearTemporaryNoteData and clearScreenshotDialogData from MainActivity
    // as their logic is now in the ViewModel or handled directly where state is used.

    private fun popToHome() {
        if (::mainNavController.isInitialized) {
            mainNavController.navigate(Screen.Home.route) {
                popUpTo(mainNavController.graph.startDestinationId) { inclusive = true }
                launchSingleTop = true
            }
        }
    }


// Screen sealed class remains the same
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Camera : Screen("camera")
    object Audio : Screen("audio")
    object NoteDetail : Screen("noteDetail/{noteId}") {
        fun createRoute(noteId: Int) = "noteDetail/$noteId"
    }
    object CreateTextNote : Screen("createTextNote")}
}