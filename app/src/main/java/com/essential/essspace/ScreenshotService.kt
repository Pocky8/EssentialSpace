package com.essential.essspace

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
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
import androidx.core.app.NotificationCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer

class ScreenshotService : Service() {

    companion object {
        const val ACTION_START_PROJECTION = "com.essential.essspace.ACTION_START_PROJECTION"
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_DATA_INTENT = "data_intent"
        const val ACTION_SCREENSHOT_TAKEN_PROMPT_AUDIO = "com.essential.essspace.ACTION_SCREENSHOT_TAKEN_PROMPT_AUDIO"
        private const val NOTIFICATION_CHANNEL_ID = "ScreenshotChannel"
        private const val NOTIFICATION_ID = 123
        private const val VIRTUAL_DISPLAY_NAME = "EssSpaceScreenCapture"
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private var mediaProjectionManager: MediaProjectionManager? = null
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var windowManager: WindowManager? = null
    private var screenDensity: Int = 0
    private var screenWidth: Int = 0
    private var screenHeight: Int = 0
    private var handlerThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null

    private val mediaProjectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            Log.w("ScreenshotService", "MediaProjection.Callback: onStop() called. Projection stopped externally.")
            // Ensure cleanup if projection stops unexpectedly.
            // Check mediaProjection to avoid issues if already stopping/stopped.
            if (this@ScreenshotService.mediaProjection != null) {
                stopSelfService()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager?.defaultDisplay?.getRealMetrics(metrics)


        screenDensity = metrics.densityDpi
        screenWidth = metrics.widthPixels
        screenHeight = metrics.heightPixels

        if (screenWidth == 0 || screenHeight == 0) {
            Log.e("ScreenshotService", "Failed to get valid screen dimensions. Using fallback 1080x1920.")
            screenWidth = 1080
            screenHeight = 1920
        }

        handlerThread = HandlerThread("ScreenshotHandlerThread").apply { start() }
        backgroundHandler = Handler(handlerThread!!.looper)

        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("EssSpace")
            .setContentText("Capturing screenshot...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
        startForeground(NOTIFICATION_ID, notification)
        Log.d("ScreenshotService", "Service created and foregrounded. Screen: $screenWidth x $screenHeight @ $screenDensity dpi")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("ScreenshotService", "onStartCommand received action: ${intent?.action}")
        if (intent?.action == ACTION_START_PROJECTION) {
            val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
            val data: Intent? = intent.getParcelableExtra(EXTRA_DATA_INTENT)

            if (resultCode == Activity.RESULT_OK && data != null) {
                mediaProjection = mediaProjectionManager?.getMediaProjection(resultCode, data)
                if (mediaProjection == null) {
                    Log.e("ScreenshotService", "Failed to get MediaProjection.")
                    stopSelfService()
                    return START_NOT_STICKY
                }
                mediaProjection?.registerCallback(mediaProjectionCallback, backgroundHandler)
                Log.d("ScreenshotService", "MediaProjection obtained and callback registered.")

                serviceScope.launch {
                    delay(300)
                    captureScreen()
                }
            } else {
                Log.e("ScreenshotService", "Result code not OK or data intent is null for projection.")
                stopSelfService()
            }
        } else {
            Log.w("ScreenshotService", "Unknown action or null intent: ${intent?.action}")
            stopSelfService()
        }
        return START_STICKY
    }

    private fun captureScreen() {
        if (mediaProjection == null) {
            Log.e("ScreenshotService", "MediaProjection is null in captureScreen. Cannot capture.")
            stopSelfService()
            return
        }
        if (screenWidth <= 0 || screenHeight <= 0) {
            Log.e("ScreenshotService", "Invalid screen dimensions ($screenWidth x $screenHeight) in captureScreen. Aborting.")
            stopSelfService()
            return
        }

        imageReader?.close()
        imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 2)
        Log.d("ScreenshotService", "ImageReader created for $screenWidth x $screenHeight")

        virtualDisplay?.release()
        try {
            virtualDisplay = mediaProjection?.createVirtualDisplay(
                VIRTUAL_DISPLAY_NAME,
                screenWidth, screenHeight, screenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader?.surface, null, backgroundHandler
            )
        } catch (e: Exception) {
            Log.e("ScreenshotService", "Exception during createVirtualDisplay.", e)
            stopSelfService()
            return
        }

        if (virtualDisplay == null) {
            Log.e("ScreenshotService", "Failed to create VirtualDisplay.")
            stopSelfService()
            return
        }
        Log.d("ScreenshotService", "VirtualDisplay created.")

        fun stopProjection() {
            backgroundHandler?.post {
                virtualDisplay?.release()
                virtualDisplay = null // Nullify after release
                imageReader?.close()
                imageReader = null // Nullify after close
                if (mediaProjection != null) {
                    mediaProjection?.unregisterCallback(mediaProjectionCallback) // Unregister the callback
                    mediaProjection?.stop()
                    mediaProjection = null
                    Log.d("ScreenshotService", "MediaProjection resources released on background thread.")
                }
            }
        }

        imageReader?.setOnImageAvailableListener({ reader ->
            var image: Image? = null
            var bitmap: Bitmap? = null
            var croppedBitmap: Bitmap? = null
            try {
                image = reader.acquireLatestImage()
                if (image != null) {
                    Log.d("ScreenshotService", "Image acquired from ImageReader.")
                    val planes = image.planes
                    val buffer: ByteBuffer = planes[0].buffer
                    val pixelStride = planes[0].pixelStride
                    val rowStride = planes[0].rowStride
                    val rowPadding = rowStride - pixelStride * screenWidth

                    bitmap = Bitmap.createBitmap(
                        screenWidth + rowPadding / pixelStride,
                        screenHeight,
                        Bitmap.Config.ARGB_8888
                    )
                    bitmap.copyPixelsFromBuffer(buffer)
                    croppedBitmap = Bitmap.createBitmap(bitmap, 0, 0, screenWidth, screenHeight)
                    Log.d("ScreenshotService", "Screenshot captured and cropped successfully.")
                    processCapturedBitmap(croppedBitmap)
                } else {
                    Log.w("ScreenshotService", "acquireLatestImage returned null.")
                    stopProjection()
                    stopSelfService()
                }
            } catch (e: Exception) {
                Log.e("ScreenshotService", "Error acquiring or processing image in listener: ${e.message}", e)
                croppedBitmap?.recycle()
                stopProjection()
                stopSelfService()
            } finally {
                image?.close()
                bitmap?.recycle()
            }
        }, backgroundHandler)
    }

    private fun processCapturedBitmap(bitmapToProcess: Bitmap) {
        val photoPath = saveBitmapToFile(bitmapToProcess)
        if (photoPath == null) {
            Log.e("ScreenshotService", "Failed to save screenshot bitmap.")
            bitmapToProcess.recycle()
            stopProjection()
            stopSelfService()
            return
        }

        val image = InputImage.fromBitmap(bitmapToProcess, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val ocrResultText = visionText.text
                Log.d("ScreenshotService", "ML Kit OCR successful. Text: ${ocrResultText.take(100)}")
                sendBroadcastResult(photoPath, ocrResultText.ifBlank { null })
            }
            .addOnFailureListener { e ->
                Log.e("ScreenshotService", "ML Kit OCR failed: ${e.message}", e)
                sendBroadcastResult(photoPath, null)
            }
            .addOnCompleteListener {
                if (!bitmapToProcess.isRecycled) {
                    bitmapToProcess.recycle()
                    Log.d("ScreenshotService", "Bitmap recycled after ML Kit processing.")
                }
                stopProjection()
                stopSelfService()
                Log.d("ScreenshotService", "ML Kit complete. Service shutdown initiated from onCompleteListener.")
            }
    }

    private fun saveBitmapToFile(bitmap: Bitmap): String? {
        val file = File(cacheDir, "screenshot_${System.currentTimeMillis()}.png")
        return try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
            }
            Log.d("ScreenshotService", "Bitmap saved to: ${file.absolutePath}")
            file.absolutePath
        } catch (e: IOException) {
            Log.e("ScreenshotService", "Error saving bitmap to file", e)
            null
        }
    }

    private fun sendBroadcastResult(filePath: String?, ocrText: String?) {
        if (filePath == null) {
            Log.e("ScreenshotService", "Cannot send broadcast, file path is null.")
            return
        }
        val intent = Intent(ACTION_SCREENSHOT_TAKEN_PROMPT_AUDIO)
        intent.putExtra("photoPath", filePath)
        ocrText?.let { intent.putExtra("ocrText", it) }
        try {
            sendBroadcast(intent)
            Log.d("ScreenshotService", "Broadcast sent - Path: $filePath, OCR: ${ocrText?.take(50)}")
        } catch (e: Exception) {
            Log.e("ScreenshotService", "Error sending broadcast", e)
        }
    }

    private fun stopProjection() {
        backgroundHandler?.post {
            virtualDisplay?.release()
            virtualDisplay = null
            imageReader?.close()
            imageReader = null
            if (mediaProjection != null) {
                mediaProjection?.unregisterCallback(mediaProjectionCallback)
                mediaProjection?.stop()
                mediaProjection = null
                Log.d("ScreenshotService", "MediaProjection resources released on background thread.")
            }
        }
    }

    private fun stopSelfService() {
        Log.d("ScreenshotService", "stopSelfService called.")
        backgroundHandler?.post {
            handlerThread?.quitSafely()
            handlerThread = null
            backgroundHandler = null
            Log.d("ScreenshotService", "HandlerThread quit and nulled.")

            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            Log.d("ScreenshotService", "Service stopped and foreground removed.")
        }
        if (backgroundHandler == null && handlerThread == null) {
            // This case might happen if the handler thread died prematurely or was never started.
            // Or if stopSelfService is called multiple times.
            Log.w("ScreenshotService", "Attempting direct stop as handler/thread is null.")
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Log.d("ScreenshotService", "onDestroy called.")
        if (mediaProjection != null || virtualDisplay != null || imageReader != null) {
            Log.w("ScreenshotService", "onDestroy: Resources might still be active. Forcing cleanup.")
            // Ensure projection resources are released directly if not already done by stopProjection via backgroundHandler
            virtualDisplay?.release()
            imageReader?.close()
            mediaProjection?.unregisterCallback(mediaProjectionCallback) // Attempt unregister
            mediaProjection?.stop() // Attempt stop
        }
        handlerThread?.quitSafely() // Ensure thread is quit
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Screenshot Service Channel",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Channel for EssSpace Screenshot Service"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
            Log.d("ScreenshotService", "Notification channel created.")
        }
    }
}