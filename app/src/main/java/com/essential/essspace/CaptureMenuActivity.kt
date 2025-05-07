package com.essential.essspace

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

class CaptureMenuActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Screenshot
                    Button(onClick = {
                        startActivity(Intent(this@CaptureMenuActivity, ScreenshotCaptureActivity::class.java))
                        finish()
                    }) { Text("Screenshot") }
                    // Camera
                    Button(onClick = {
                        val intent = Intent(this@CaptureMenuActivity, MainActivity::class.java)
                            .apply {
                                putExtra("route", "camera")
                                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                            }
                        startActivity(intent)
                        finish()
                    }) {
                        Text("Camera")
                    }
                    // Voice
                    Button(onClick = {
                        val intent = Intent(this@CaptureMenuActivity, MainActivity::class.java)
                            .apply {
                                putExtra("route", "audio")
                                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                            }
                        startActivity(intent)
                        finish()
                    }) {
                        Text("Voice")
                    }
                    // Open Hub
                    Button(onClick = {
                        val intent = Intent(this@CaptureMenuActivity, MainActivity::class.java)
                            .apply {
                                putExtra("route", "home")
                                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                            }
                        startActivity(intent)
                        finish()
                    }) {
                        Text("Open Hub")
                    }
                }
            }
        }
    }
}