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
import com.essential.essspace.TextCleanupUtils
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.essential.essspace.BitmapUtils // Make sure this path is correct
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import java.io.FileOutputStream

class ScreenshotService : Service() {
    private var hasCapturedImage = false
    private lateinit var mediaProjectionManager: MediaProjectionManager
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private lateinit var windowManager: WindowManager
    private lateinit var serviceHandler: Handler
    private lateinit var handlerThread: HandlerThread
    private lateinit var textRecognizer: com.google.mlkit.vision.text.TextRecognizer

    companion object {
        const val ACTION_START = "com.essential.essspace.ACTION_START_SCREENSHOT"
        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_DATA = "extra_data"
        private const val NOTIFICATION_CHANNEL_ID = "ScreenshotChannel"
        private const val NOTIFICATION_ID = 123
        private const val VIRTUAL_DISPLAY_NAME = "EssspaceScreenshot"
        private const val TAG = "ScreenshotService"

        const val ACTION_SCREENSHOT_PROCESSED = "com.essential.essspace.ACTION_SCREENSHOT_PROCESSED"
        const val EXTRA_PHOTO_PATH = "extra_photo_path"
        const val EXTRA_OCR_TEXT = "extra_ocr_text"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        handlerThread = HandlerThread("ScreenshotHandlerThread")
        handlerThread.start()
        serviceHandler = Handler(handlerThread.looper)

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
                serviceHandler.postDelayed({
                    try {
                        mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data)
                        if (mediaProjection == null) {
                            Log.e(TAG, "MediaProjection is null after getMediaProjection")
                            stopSelfService("MediaProjection is null")
                            return@postDelayed
                        }
                        Log.d(TAG, "MediaProjection obtained.")
                        mediaProjection?.registerCallback(MediaProjectionCallback(), serviceHandler)
                        setupVirtualDisplay()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error starting projection: ${e.message}", e)
                        stopSelfService("Error starting projection: ${e.message}")
                    }
                }, 300)
            } else {
                Log.e(TAG, "Result code not OK or data is null. ResultCode: $resultCode")
                stopSelfService("Permission denied or data null")
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
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(serviceChannel)
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val display = windowManager.defaultDisplay
            display.getRealMetrics(displayMetrics)
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(displayMetrics)
        }

        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        val screenDensity = displayMetrics.densityDpi

        if (screenWidth <= 0 || screenHeight <= 0) {
            Log.e(TAG, "Invalid screen dimensions: $screenWidth x $screenHeight")
            stopSelfService("Invalid screen dimensions")
            return
        }
        Log.d(TAG, "Screen dimensions: $screenWidth x $screenHeight @ $screenDensity dpi")

        imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 2)
        Log.d(TAG, "ImageReader created.")

        imageReader?.setOnImageAvailableListener({ reader ->
            if (hasCapturedImage) {
                Log.d(TAG, "Image available, but already captured one. Ignoring and closing.")
                reader.acquireLatestImage()?.close()
                return@setOnImageAvailableListener
            }
            Log.d(TAG, "Image available in ImageReader.")
            var image: android.media.Image? = null
            var tempBitmap: Bitmap? = null
            var finalBitmapToProcess: Bitmap? = null
            try {
                image = reader.acquireLatestImage()
                if (image != null) {
                    hasCapturedImage = true // Set flag: we are processing this image.
                    Log.d(TAG, "Image acquired from reader.")
                    val planes = image.planes
                    val buffer = planes[0].buffer
                    val pixelStride = planes[0].pixelStride
                    val rowStride = planes[0].rowStride
                    val rowPadding = rowStride - pixelStride * screenWidth

                    tempBitmap = Bitmap.createBitmap(
                        screenWidth + rowPadding / pixelStride,
                        screenHeight,
                        Bitmap.Config.ARGB_8888
                    )
                    tempBitmap.copyPixelsFromBuffer(buffer)

                    finalBitmapToProcess = if (rowPadding > 0 || tempBitmap.width != screenWidth || tempBitmap.height != screenHeight) {
                        Log.d(TAG, "Cropping bitmap from ${tempBitmap.width}x${tempBitmap.height} to ${screenWidth}x${screenHeight}.")
                        Bitmap.createBitmap(tempBitmap, 0, 0, screenWidth, screenHeight)
                    } else {
                        tempBitmap
                    }

                    if (finalBitmapToProcess != tempBitmap && tempBitmap != null && !tempBitmap.isRecycled) {
                        Log.d(TAG, "Recycling tempBitmap after cropping.")
                        tempBitmap.recycle()
                    }
                    tempBitmap = null // Avoid re-recycling

                    if (finalBitmapToProcess != null) {
                        imageReader?.setOnImageAvailableListener(null, serviceHandler) // Stop listening
                        Handler(Looper.getMainLooper()).post {
                            Toast.makeText(applicationContext, "Screenshot is being saved...", Toast.LENGTH_SHORT).show()
                        }
                        performOcrAndSaveFile(finalBitmapToProcess) // This bitmap will be recycled by performOcrAndSaveFile/saveFileAndBroadcast
                    } else {
                        Log.w(TAG, "Final bitmap to process is null.")
                        stopSelfService("Final bitmap was null")
                    }
                } else {
                    Log.w(TAG, "Acquired image was null. Waiting for next if any.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing image: ${e.message}", e)
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(this, "Error processing screenshot: ${e.message}", Toast.LENGTH_LONG).show()
                }
                finalBitmapToProcess?.recycle()
                tempBitmap?.recycle()
                stopSelfService("Error processing image")
            } finally {
                image?.close()
            }
        }, serviceHandler)

        try {
            if (mediaProjection == null) {
                Log.e(TAG, "MediaProjection became null before creating VirtualDisplay")
                stopSelfService("MediaProjection became null")
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
                serviceHandler
            )
            if (virtualDisplay == null) {
                Log.e(TAG, "Failed to create VirtualDisplay, it's null.")
                stopSelfService("Failed to create VirtualDisplay")
            } else {
                Log.d(TAG, "Virtual display created successfully.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating VirtualDisplay: ${e.message}", e)
            stopSelfService("Error creating VirtualDisplay: ${e.message}")
        }
    }

    private fun performOcrAndSaveFile(originalBitmapToSave: Bitmap) {
        var bitmapForOcr: Bitmap? = null
        var ocrAttempted = false
        try {
            // Create a copy of the original bitmap to be resized for OCR.
            // The originalBitmapToSave itself will be saved to file.
            val copyForResize = originalBitmapToSave.copy(originalBitmapToSave.config, false)
            bitmapForOcr = BitmapUtils.getResizedBitmapForOcr(copyForResize) // copyForResize is recycled by BitmapUtils if resized.

            if (bitmapForOcr == null) { // Fallback if resizing somehow fails
                Log.e(TAG, "Bitmap for OCR is null after resize attempt. Using a direct copy for OCR.")
                bitmapForOcr = originalBitmapToSave.copy(originalBitmapToSave.config, false)
            }

            val inputImage = InputImage.fromBitmap(bitmapForOcr!!, 0) // Use non-null assertion after check/fallback
            ocrAttempted = true

            textRecognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    val rawOcrResult = visionText.text
                    val cleanedOcrResult = TextCleanupUtils.cleanOcrText(rawOcrResult)
                    Log.d(TAG, "OCR successful. Raw: ${rawOcrResult.take(150)}, Cleaned: ${cleanedOcrResult?.take(150)}")
                    saveFileAndBroadcast(originalBitmapToSave, cleanedOcrResult) // Pass cleaned text
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "OCR failed", e)
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(applicationContext, "OCR failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                    saveFileAndBroadcast(originalBitmapToSave, null)
                }
                .addOnCompleteListener {
                    // Recycle the bitmap used for OCR (which was a copy or resized version)
                    // Ensure it's not the same instance as originalBitmapToSave (shouldn't be with .copy())
                    if (bitmapForOcr != originalBitmapToSave && bitmapForOcr?.isRecycled == false) {
                        bitmapForOcr.recycle()
                        Log.d(TAG, "Recycled bitmapForOcr (copy/resized) in complete listener.")
                    }
                    // originalBitmapToSave is passed to saveFileAndBroadcast and recycled there.
                }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during OCR preparation or processing: ${e.message}", e)
            if (!ocrAttempted) { // If error before textRecognizer.process
                saveFileAndBroadcast(originalBitmapToSave, null) // Save original without OCR
            }
            // Clean up bitmapForOcr if it was created and an error occurred before onCompleteListener
            if (bitmapForOcr != originalBitmapToSave && bitmapForOcr?.isRecycled == false) {
                bitmapForOcr.recycle()
                Log.d(TAG, "Recycled bitmapForOcr (copy/resized) in catch block.")
            }
        }
    }

    private fun saveFileAndBroadcast(bitmapToSave: Bitmap, ocrText: String?) {
        val screenshotsDir = File(getExternalFilesDir(null), "Screenshots_Essspace")
        if (!screenshotsDir.exists()) {
            screenshotsDir.mkdirs()
        }
        val fileName = "screenshot_${System.currentTimeMillis()}.png"
        val file = File(screenshotsDir, fileName)
        Log.d(TAG, "Attempting to save screenshot to: ${file.absolutePath}")
        var success = false
        try {
            FileOutputStream(file).use { out ->
                bitmapToSave.compress(Bitmap.CompressFormat.PNG, 100, out)
                Log.i(TAG, "Screenshot bitmap compressed and saved to: ${file.absolutePath}")
            }
            success = true
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(applicationContext, "Screenshot captured and processed!", Toast.LENGTH_SHORT).show()
            }
            val processedIntent = Intent(ACTION_SCREENSHOT_PROCESSED).apply {
                putExtra(EXTRA_PHOTO_PATH, file.absolutePath)
                putExtra(EXTRA_OCR_TEXT, ocrText)
                setPackage(packageName)
            }
            sendBroadcast(processedIntent)
            Log.d(TAG, "Broadcast sent. OCR text length: ${ocrText?.length ?: "null"}")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving bitmap: ${e.message}", e)
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(applicationContext, "Failed to save screenshot file.", Toast.LENGTH_LONG).show()
            }
        } finally {
            if (!bitmapToSave.isRecycled) {
                bitmapToSave.recycle()
                Log.d(TAG, "Recycled bitmapToSave in saveFileAndBroadcast.")
            }
            stopSelfService(if(success) "Processing complete." else "File save error or OCR issue.")
        }
    }

    private inner class MediaProjectionCallback : MediaProjection.Callback() {
        override fun onStop() {
            super.onStop()
            Log.w(TAG, "MediaProjection.Callback onStop called.")
            // This can be called if the projection is stopped externally or screen is locked.
            // Ensure resources are cleaned up.
            serviceHandler.post { releaseResources() }
        }
    }

    private fun releaseResources() {
        Log.d(TAG, "Releasing resources...")
        if (::serviceHandler.isInitialized) { // Check if initialized
            serviceHandler.removeCallbacksAndMessages(null)
        }
        virtualDisplay?.release()
        virtualDisplay = null
        imageReader?.close()
        imageReader = null
        mediaProjection?.unregisterCallback(MediaProjectionCallback())
        mediaProjection?.stop()
        mediaProjection = null
        hasCapturedImage = false // Reset for next potential capture
        Log.d(TAG, "Resources released.")
    }

    private fun stopSelfService(reason: String? = null) {
        if (reason != null) {
            Log.i(TAG, "Stopping service: $reason")
        }
        releaseResources()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
        Log.d(TAG, "stopSelf() called.")
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy called.")
        releaseResources()
        if (::handlerThread.isInitialized && handlerThread.isAlive) {
            handlerThread.quitSafely()
            Log.d(TAG, "HandlerThread quit.")
        }
    }
}