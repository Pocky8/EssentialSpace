package com.essential.essspace

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.essential.essspace.room.Note
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

                NavHost(navController = mainNavController, startDestination = "home") {
                    composable("home") {
                        HubScreen(
                            onNavigateToCamera = {
                                mainNavController.navigate("camera")
                            },
                            onNavigateToAudio = {
                                // Navigate to audio recording, potentially without a photo
                                capturedPhotoPath = null // Ensure no prior photo is used
                                mainNavController.navigate("audio")
                            },
                            onTakeScreenshot = {
                                // This is for the screenshot option from the new menu
                                startActivity(
                                    Intent(this@MainActivity, MainActivity::class.java).apply {
                                        putExtra("route", "screenshot")
                                        // Consider flags if ScreenshotCaptureActivity is a standard activity
                                        // addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                    }
                                )
                            },
                            onScreenshotIconClick = {
                                // This is for the top-bar icon in NotesListScreen
                                startActivity(
                                    Intent(this@MainActivity, MainActivity::class.java).apply {
                                        putExtra("route", "screenshot")
                                    }
                                )
                            },
                            onAddNewNoteFromHub = { // This is for the FAB in NotesListScreen
                                mainNavController.navigate("camera") // Or open the new menu
                            }
                        )
                    }

                    composable("camera") {
                        CameraScreen( // Ensure CameraScreen.kt is implemented
                            onPhotoTaken = { photoPath ->
                                capturedPhotoPath = photoPath
                                mainNavController.navigate("audio")
                            },
                            onCancel = {
                                capturedPhotoPath = null // Clear if cancelled
                                mainNavController.popBackStack()
                            }
                        )
                    }

                    composable("audio") {
                        AudioRecordScreen(
                            photoPath = capturedPhotoPath, // Can be null if 'Audio Only' was selected
                            onComplete = { audioPath -> // audioPath can be null
                                if (capturedPhotoPath != null || audioPath != null) {
                                    val newNote = Note(
                                        photoPath = capturedPhotoPath,
                                        audioPath = audioPath,
                                        text = "Captured Note"
                                    )
                                    notesViewModel.insertNote(newNote)
                                }
                                capturedPhotoPath = null // Clear after use
                                mainNavController.navigate("home") {
                                    popUpTo("home") { inclusive = true }
                                }
                            },
                            onCancel = {
                                capturedPhotoPath = null // Clear if cancelled
                                mainNavController.popBackStack() // Or navigate to home
                            }
                        )
                    }
                    // You might add a dedicated "screenshot_capture_intent" route if needed
                    // composable("screenshot_capture_intent") { /* ... */ }
                }
            }
        }
        handleRoute(intent) // Handle initial intent
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent) // Update the activity's intent
        handleRoute(intent)
    }

    private fun handleRoute(currentIntent: Intent?) {
        val routeArg = currentIntent?.getStringExtra("route") ?: return

        if (!::mainNavController.isInitialized || mainNavController.currentDestination?.route == routeArg) {
            currentIntent.removeExtra("route")
            return
        }

        when (routeArg) {
            "camera" -> mainNavController.navigate("camera")
            "audio" -> {
                capturedPhotoPath = null // Ensure no photo if navigating directly to audio
                mainNavController.navigate("audio")
            }
            "screenshot" -> {
                // This will launch ScreenshotCaptureActivity via onNewIntent if MainActivity is already running
                // or directly if it's a fresh launch.
                // For clarity, you might want ScreenshotCaptureActivity to be launched directly
                // without routing through MainActivity's onNewIntent for this specific action.
                startActivity(Intent(this, ScreenshotCaptureActivity::class.java))
            }
        }
        currentIntent.removeExtra("route")
    }
}