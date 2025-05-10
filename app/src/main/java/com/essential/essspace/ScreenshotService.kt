package com.essential.essspace

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.essential.essspace.room.Note // Assuming you want to save to Room
import com.essential.essspace.room.NoteRepository // Assuming you want to save to Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class ScreenshotService : Service() {

    private lateinit var mediaProjectionManager: MediaProjectionManager
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private lateinit var windowManager: WindowManager
    private lateinit var handler: Handler
    private lateinit var handlerThread: HandlerThread
    private lateinit var noteRepository: NoteRepository


    companion object {
        const val ACTION_START = "com.essential.essspace.ACTION_START_SCREENSHOT"
        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_DATA = "extra_data"
        private const val NOTIFICATION_CHANNEL_ID = "ScreenshotChannel"
        private const val NOTIFICATION_ID = 123
        private const val VIRTUAL_DISPLAY_NAME = "EssspaceScreenshot"
        private const val TAG = "ScreenshotService"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        noteRepository = NoteRepository(applicationContext) // Initialize repository


        handlerThread = HandlerThread("ScreenshotHandlerThread")
        handlerThread.start()
        handler = Handler(handlerThread.looper)

        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand, action: ${intent?.action}")
        if (intent?.action == ACTION_START) {
            val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
            val data: Intent? = intent.getParcelableExtra(EXTRA_DATA)

            if (resultCode == Activity.RESULT_OK && data != null) {
                Log.d(TAG, "Permission granted, starting foreground service.")
                startForeground(NOTIFICATION_ID, createNotification())

                // Delay slightly to allow system to prepare, then get projection
                handler.postDelayed({
                    try {
                        mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data)
                        if (mediaProjection == null) {
                            Log.e(TAG, "MediaProjection is null after getMediaProjection")
                            stopSelfService("MediaProjection is null after getMediaProjection")
                            return@postDelayed
                        }
                        Log.d(TAG, "MediaProjection obtained.")
                        mediaProjection?.registerCallback(MediaProjectionCallback(), handler)
                        Log.d(TAG, "MediaProjection callback registered.")
                        setupVirtualDisplay()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error starting projection: ${e.message}", e)
                        stopSelfService("Error starting projection: ${e.message}")
                    }
                }, 300) // 300ms delay
            } else {
                Log.e(TAG, "Result code not OK or data is null. ResultCode: $resultCode")
                stopSelfService("Result code not OK or data is null")
            }
        } else {
            Log.w(TAG, "Unknown action or null intent. Action: ${intent?.action}")
            stopSelfService("Unknown action or null intent")
        }
        return START_NOT_STICKY
    }


    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Screenshot Service Channel",
                NotificationManager.IMPORTANCE_LOW // Use LOW to avoid sound/pop-up if not critical
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
            Log.d(TAG, "Notification channel created.")
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Essspace")
            .setContentText("Capturing screenshot...")
            .setSmallIcon(R.mipmap.ic_launcher) // Ensure this icon exists
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun setupVirtualDisplay() {
        Log.d(TAG, "Setting up virtual display.")
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getRealMetrics(displayMetrics)

        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        val screenDensity = displayMetrics.densityDpi

        if (screenWidth <= 0 || screenHeight <= 0) {
            Log.e(TAG, "Invalid screen dimensions: $screenWidth x $screenHeight")
            stopSelfService("Invalid screen dimensions: $screenWidth x $screenHeight")
            return
        }
        Log.d(TAG, "Screen dimensions: $screenWidth x $screenHeight @ $screenDensity dpi")

        imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 2)
        Log.d(TAG, "ImageReader created.")

        imageReader?.setOnImageAvailableListener({ reader ->
            Log.d(TAG, "Image available in ImageReader.")
            var image: android.media.Image? = null
            var bitmap: Bitmap? = null
            var croppedBitmapToSave: Bitmap? = null
            try {
                image = reader.acquireLatestImage()
                if (image != null) {
                    Log.d(TAG, "Image acquired from reader.")
                    val planes = image.planes
                    val buffer = planes[0].buffer
                    val pixelStride = planes[0].pixelStride
                    val rowStride = planes[0].rowStride
                    val rowPadding = rowStride - pixelStride * screenWidth

                    val tempBitmap = Bitmap.createBitmap(
                        screenWidth + rowPadding / pixelStride,
                        screenHeight,
                        Bitmap.Config.ARGB_8888
                    )
                    tempBitmap.copyPixelsFromBuffer(buffer)

                    // Crop to actual screen dimensions if necessary
                    croppedBitmapToSave = if (rowPadding > 0 || tempBitmap.width != screenWidth || tempBitmap.height != screenHeight) {
                        Log.d(TAG, "Cropping bitmap.")
                        Bitmap.createBitmap(tempBitmap, 0, 0, screenWidth, screenHeight)
                    } else {
                        Log.d(TAG, "Using original bitmap (no cropping needed).")
                        tempBitmap // Use the original if no cropping needed
                    }

                    if (croppedBitmapToSave != tempBitmap) { // if a new bitmap was created by cropping
                        tempBitmap.recycle()
                    }

                    saveBitmap(croppedBitmapToSave) // Pass the final bitmap to save
                    Toast.makeText(this, "Screenshot saved!", Toast.LENGTH_SHORT).show()
                    Log.i(TAG, "Screenshot processed and saved.")
                } else {
                    Log.w(TAG, "Acquired image was null.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing image: ${e.message}", e)
                Toast.makeText(this, "Error processing screenshot: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                image?.close()
                // The bitmap passed to saveBitmap should be recycled there if it's a copy,
                // or after use if it's the original.
                // For simplicity, let saveBitmap handle its input.
                // croppedBitmapToSave?.recycle() // Don't recycle here if saveBitmap needs it.
                stopSelfService() // Stop after attempting capture
            }
        }, handler)

        try {
            if (mediaProjection == null) {
                Log.e(TAG, "MediaProjection became null before creating VirtualDisplay")
                stopSelfService("MediaProjection became null before creating VirtualDisplay")
                return
            }
            Log.d(TAG, "Creating virtual display.")
            virtualDisplay = mediaProjection?.createVirtualDisplay(
                VIRTUAL_DISPLAY_NAME,
                screenWidth,
                screenHeight,
                screenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader?.surface,
                null,
                handler
            )
            if (virtualDisplay == null) {
                Log.e(TAG, "Failed to create VirtualDisplay, it's null.")
                stopSelfService("Failed to create VirtualDisplay")
            } else {
                Log.d(TAG, "Virtual display created successfully.")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException creating VirtualDisplay: ${e.message}", e)
            stopSelfService("SecurityException creating VirtualDisplay: ${e.message}")
        } catch (e: IllegalStateException) {
            Log.e(TAG, "IllegalStateException creating VirtualDisplay: ${e.message}", e)
            stopSelfService("IllegalStateException creating VirtualDisplay: ${e.message}")
        }
        catch (e: Exception) {
            Log.e(TAG, "Generic error creating VirtualDisplay: ${e.message}", e)
            stopSelfService("Error creating VirtualDisplay: ${e.message}")
        }
    }


    private fun saveBitmap(bitmapToSave: Bitmap) {
        val screenshotsDir = File(getExternalFilesDir(null), "Screenshots_Essspace")
        if (!screenshotsDir.exists()) {
            screenshotsDir.mkdirs()
        }
        val fileName = "screenshot_${System.currentTimeMillis()}.png"
        val file = File(screenshotsDir, fileName)
        Log.d(TAG, "Attempting to save screenshot to: ${file.absolutePath}")
        try {
            FileOutputStream(file).use { out ->
                bitmapToSave.compress(Bitmap.CompressFormat.PNG, 100, out)
                Log.i(TAG, "Screenshot bitmap compressed and saved to: ${file.absolutePath}")
            }
            // Save to Room Database
            CoroutineScope(Dispatchers.IO).launch {
                val newNote = Note(photoPath = file.absolutePath, audioPath = null, text = "Screenshot: $fileName")
                noteRepository.insertNote(newNote) // Use the initialized repository
                Log.d(TAG, "Screenshot metadata saved to Room database.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving bitmap to file or DB: ${e.message}", e)
        } finally {
            if (!bitmapToSave.isRecycled) {
                // bitmapToSave.recycle() // Recycle after saving if it's no longer needed.
                // Be cautious if any async operations still need it.
                // For this flow, it should be safe to recycle.
            }
        }
    }


    private inner class MediaProjectionCallback : MediaProjection.Callback() {
        override fun onStop() {
            super.onStop()
            Log.w(TAG, "MediaProjection.Callback onStop called.")
            releaseResources()
            // Consider stopping the service if projection stops unexpectedly
            // stopSelfService("MediaProjection stopped via callback")
        }
    }

    private fun releaseResources() {
        Log.d(TAG, "Releasing resources...")
        virtualDisplay?.release()
        virtualDisplay = null
        imageReader?.close() // Close the reader to free up its surface
        imageReader = null
        if (mediaProjection != null) {
            mediaProjection?.unregisterCallback(MediaProjectionCallback()) // Unregister before stopping
            mediaProjection?.stop()
            mediaProjection = null
            Log.d(TAG, "MediaProjection resources released.")
        }
    }

    private fun stopSelfService(reason: String? = null) {
        if (reason != null) {
            Log.e(TAG, "Stopping service: $reason")
            Toast.makeText(this, "Screenshot failed: $reason", Toast.LENGTH_LONG).show()
        } else {
            Log.i(TAG, "Stopping service normally.")
        }
        releaseResources()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
        if (::handlerThread.isInitialized && handlerThread.isAlive) {
            handlerThread.quitSafely()
            Log.d(TAG, "HandlerThread quit.")
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        Log.d(TAG, "onBind called, returning null.")
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy called.")
        stopSelfService("Service destroyed")
    }
}