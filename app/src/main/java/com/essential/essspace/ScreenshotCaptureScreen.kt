package com.essential.essspace

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi

class ScreenshotCaptureActivity : ComponentActivity() {

    private lateinit var mediaProjectionManager: MediaProjectionManager

    @RequiresApi(Build.VERSION_CODES.O)
    private val screenCaptureLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                Log.d("ScreenshotCapture", "Screen capture permission granted.")
                val serviceIntent = Intent(this, ScreenshotService::class.java).apply {
                    action = ScreenshotService.ACTION_START_PROJECTION // New action for service
                    putExtra(ScreenshotService.EXTRA_RESULT_CODE, result.resultCode)
                    putExtra(ScreenshotService.EXTRA_DATA_INTENT, result.data) // Pass the whole intent
                }
                startForegroundService(serviceIntent)
            } else {
                Log.w("ScreenshotCapture", "Screen capture permission denied or failed.")
                Toast.makeText(this, "Screenshot permission denied.", Toast.LENGTH_SHORT).show()
            }
            finish() // Finish this activity regardless of permission outcome
        }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("ScreenshotCapture", "Activity created. Requesting projection.")

        mediaProjectionManager =
            getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        // Request screen capture permission
        screenCaptureLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
        // This activity has no UI, it just launches the permission intent and handles the result.
    }
}