package com.essential.essspace

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.essential.essspace.ui.theme.EssentialSpaceTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            EssentialSpaceTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    NavHost(navController = navController, startDestination = "home") {
                        composable("home") {
                            HomeScreen(navController = navController)
                        }
                        composable("camera") {
                            CameraScreen(
                                onPhotoTaken = { uri ->
                                    navController.navigate("record/$uri")
                                },
                                onCancel = {
                                    navController.navigateUp()
                                }
                            )
                        }
                        composable("record/{photoUri}") { backStackEntry ->
                            val photoUri = backStackEntry.arguments?.getString("photoUri")
                            AudioRecordScreen(
                                photoUri = photoUri,
                                onComplete = {
                                    navController.navigate("home") {
                                        popUpTo("home") { inclusive = true }
                                    }
                                },
                                onCancel = {
                                    navController.navigateUp()

                                    
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}