package com.essential.essspace

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.PixelCopy
import android.view.WindowManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class ScreenshotCaptureActivity : Activity() {
    private val REQUEST_PROJECTION = 999
    private val TAG = "ScreenshotCapture"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Requesting screen capture permission.")
        val mpm = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        try {
            startActivityForResult(mpm.createScreenCaptureIntent(), REQUEST_PROJECTION)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting screen capture intent", e)
            Toast.makeText(this, "Could not start screenshot service.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_PROJECTION) {
            if (resultCode == RESULT_OK && data != null) {
                Log.d(TAG, "onActivityResult: Permission granted. Capturing screenshot.")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    captureUsingPixelCopy(resultCode, data)
                } else {
                    Toast.makeText(this, "Screenshot requires Android Oreo (API 26) or higher.", Toast.LENGTH_LONG).show()
                    finish()
                }
            } else {
                Log.w(TAG, "onActivityResult: Permission denied or data is null.")
                Toast.makeText(this, "Screenshot permission was not granted.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun captureUsingPixelCopy(resultCode: Int, data: Intent) {
        val view = window.decorView.rootView
        val displayMetrics = resources.displayMetrics
        val bitmap = Bitmap.createBitmap(
            displayMetrics.widthPixels,
            displayMetrics.heightPixels,
            Bitmap.Config.ARGB_8888
        )

        val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data)

        val virtualDisplay = mediaProjection.createVirtualDisplay(
            "ScreenshotVirtualDisplay",
            displayMetrics.widthPixels,
            displayMetrics.heightPixels,
            displayMetrics.densityDpi,
            WindowManager.LayoutParams.FLAG_SECURE, // Use appropriate flags
            null, // Surface - PixelCopy uses the window's surface
            null, // Callback
            null  // Handler
        )

        // Add a small delay to ensure the screen is ready, especially if there are animations
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                PixelCopy.request(window, bitmap, { copyResult ->
                    virtualDisplay?.release() // Release virtual display
                    mediaProjection?.stop()    // Stop media projection

                    if (copyResult == PixelCopy.SUCCESS) {
                        Log.d(TAG, "PixelCopy success. Saving screenshot.")
                        saveScreenshot(bitmap)
                    } else {
                        Log.e(TAG, "PixelCopy failed with result: $copyResult")
                        Toast.makeText(this, "Screenshot capture failed: $copyResult", Toast.LENGTH_SHORT).show()
                    }
                    finish()
                }, Handler(Looper.getMainLooper())) // Ensure this handler is on the main looper
            } catch (e: Exception) {
                Log.e(TAG, "Error during PixelCopy.request", e)
                virtualDisplay?.release()
                mediaProjection?.stop()
                Toast.makeText(this, "Error taking screenshot.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }, 100) // 100ms delay, adjust if necessary
    }

    private fun saveScreenshot(bitmap: Bitmap) {
        val dir = getExternalFilesDir("screenshots")
        if (dir == null) {
            Log.e(TAG, "External files directory for screenshots is null.")
            Toast.makeText(this, "Could not access storage for screenshots.", Toast.LENGTH_LONG).show()
            return
        }
        if (!dir.exists()) {
            dir.mkdirs()
        }

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "screenshot_$timestamp.png"
        val file = File(dir, fileName)

        try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                out.flush()
            }
            Log.d(TAG, "Screenshot saved to: ${file.absolutePath}")
            Toast.makeText(this, "Screenshot saved to ${file.name}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving screenshot to file", e)
            Toast.makeText(this, "Failed to save screenshot.", Toast.LENGTH_SHORT).show()
        }
    }
}