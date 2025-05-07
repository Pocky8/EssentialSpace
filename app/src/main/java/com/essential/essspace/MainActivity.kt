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
                                capturedPhotoPath = null
                                mainNavController.navigate("audio")
                            },
                            onTakeScreenshot = {
                                // Directly start ScreenshotCaptureActivity
                                startActivity(Intent(this@MainActivity, ScreenshotCaptureActivity::class.java))
                            },
                            onScreenshotIconClick = {
                                // This is for the top-bar icon in NotesListScreen
                                // Also directly start ScreenshotCaptureActivity
                                startActivity(Intent(this@MainActivity, ScreenshotCaptureActivity::class.java))
                            },
                            onAddNewNoteFromHub = {
                                mainNavController.navigate("camera")
                            }
                        )
                    }

                    composable("camera") {
                        CameraScreen(
                            onPhotoTaken = { photoPath ->
                                capturedPhotoPath = photoPath
                                mainNavController.navigate("audio")
                            },
                            onCancel = {
                                capturedPhotoPath = null
                                mainNavController.popBackStack()
                            }
                        )
                    }

                    composable("audio") {
                        AudioRecordScreen(
                            photoPath = capturedPhotoPath,
                            onComplete = { audioPath ->
                                if (capturedPhotoPath != null || audioPath != null) {
                                    val newNote = Note(
                                        photoPath = capturedPhotoPath,
                                        audioPath = audioPath,
                                        text = "Captured Note"
                                        // timestamp will be set by default in Note data class
                                    )
                                    notesViewModel.insertNote(newNote)
                                }
                                capturedPhotoPath = null
                                mainNavController.navigate("home") {
                                    popUpTo("home") { inclusive = true }
                                }
                            },
                            onCancel = {
                                capturedPhotoPath = null
                                mainNavController.popBackStack()
                            }
                        )
                    }
                }
            }
        }
        handleRoute(intent) // Handle initial intent for other routes if necessary
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
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
                capturedPhotoPath = null
                mainNavController.navigate("audio")
            }
            // "screenshot" case removed from here
        }
        currentIntent.removeExtra("route")
    }
}