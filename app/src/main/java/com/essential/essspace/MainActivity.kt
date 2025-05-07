package com.essential.essspace

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.essential.essspace.ui.theme.EssentialSpaceTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            EssentialSpaceTheme {
                val navController = rememberNavController()
                var capturedPhotoUri by remember { mutableStateOf<String?>(null) }

                NavHost(navController, startDestination = "home") {
                    composable("home") {
                        NotesListScreen(
                            onAddNewNote = { navController.navigate("camera") }
                        )
                    }
                    composable("camera") {
                        CameraScreen(
                            onPhotoTaken = { uri ->
                                capturedPhotoUri = uri.toString()
                                navController.navigate("audio")
                            },
                            onCancel = {
                                navController.popBackStack()
                            }
                        )
                    }
                    composable("audio") {
                        AudioRecordScreen(
                            photoUri = capturedPhotoUri,
                            onComplete = {
                                capturedPhotoUri = null
                                navController.navigate("home") {
                                    popUpTo("home") { inclusive = true }
                                }
                            },
                            onCancel = {
                                capturedPhotoUri = null
                                navController.navigate("home") {
                                    popUpTo("home") { inclusive = true }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}