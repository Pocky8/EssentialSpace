package com.essential.essspace

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.navigation.NavType
import androidx.navigation.compose.*
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
                        HomeScreen(navController = navController)
                    }
                    composable("camera") {
                        CameraScreen(
                            onPhotoTaken = { photoUri ->
                                capturedPhotoUri = photoUri.toString()
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
                                navController.navigate("home") {
                                    popUpTo("home") { inclusive = true }
                                }
                            },
                            onCancel = {
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