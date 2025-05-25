package com.essential.essspace

import android.graphics.Bitmap
import com.essential.essspace.util.cleanOcrText // Changed import
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.essential.essspace.BitmapUtils
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

@Composable
fun GalleryImagePickerScreen(
    onImageProcessed: (imagePath: String, ocrText: String?) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showProcessingIndicator by remember { mutableStateOf(false) }
    var hasLaunchedPicker by remember { mutableStateOf(false) } // Prevents re-launching picker on recomposition

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            showProcessingIndicator = true
            Log.d("GalleryImagePicker", "Image URI selected: $uri")
            coroutineScope.launch {
                var originalBitmap: Bitmap? = null
                var bitmapForOcr: Bitmap? = null
                var imageFilePath: String? = null // To store the path of the copied file

                try {
                    val tempImageFile = File(context.cacheDir, "gallery_image_${System.currentTimeMillis()}.jpg")
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { inputStream: InputStream ->
                            FileOutputStream(tempImageFile).use { outputStream ->
                                inputStream.copyTo(outputStream)
                            }
                        }
                    }
                    imageFilePath = tempImageFile.absolutePath
                    Log.d("GalleryImagePicker", "Image copied to: $imageFilePath")

                    originalBitmap = BitmapFactory.decodeFile(imageFilePath)
                    if (originalBitmap != null) {
                        bitmapForOcr = BitmapUtils.getResizedBitmapForOcr(originalBitmap) // originalBitmap is recycled by BitmapUtils if resized

                        if (bitmapForOcr == null) {
                            Log.e("GalleryImagePicker", "Bitmap for OCR is null after resize attempt.")
                            Toast.makeText(context, "Failed to process image (resize error).", Toast.LENGTH_SHORT).show()
                            showProcessingIndicator = false
                            onImageProcessed(imageFilePath, null) // Pass path even if OCR part fails
                            return@launch
                        }

                        val image = InputImage.fromBitmap(bitmapForOcr!!, 0) // Use non-null assertion
                        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

                        recognizer.process(image)
                            .addOnSuccessListener { visionText ->
                                val rawOcrResult = visionText.text
                                val cleanedReadableOcrResult = cleanOcrText(rawOcrResult) // Only cleanOcrText
                                Log.d("GalleryImagePicker", "ML Kit OCR successful. Raw: ${rawOcrResult.take(100)}, CleanedReadable: ${cleanedReadableOcrResult.take(100)}")
                                showProcessingIndicator = false
                                onImageProcessed(imageFilePath, cleanedReadableOcrResult) // Pass readable text
                            }
                            .addOnFailureListener { e ->
                                Log.e("GalleryImagePicker", "ML Kit OCR failed for gallery image", e)
                                Toast.makeText(context, "OCR failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                showProcessingIndicator = false
                                onImageProcessed(imageFilePath, null)
                            }
                            .addOnCompleteListener {
                                // This block executes after success or failure.
                                // showProcessingIndicator is set in success/failure.
                                if (bitmapForOcr?.isRecycled == false) {
                                    bitmapForOcr.recycle()
                                    Log.d("GalleryImagePicker", "bitmapForOcr recycled in onComplete.")
                                }
                            }
                    } else {
                        Log.e("GalleryImagePicker", "Failed to decode original bitmap from file: $imageFilePath")
                        Toast.makeText(context, "Failed to decode image.", Toast.LENGTH_SHORT).show()
                        showProcessingIndicator = false
                        onImageProcessed(imageFilePath, null) // Still pass path if file was created
                    }
                } catch (e: Exception) {
                    Log.e("GalleryImagePicker", "Error processing gallery image", e)
                    Toast.makeText(context, "Error processing image: ${e.message}", Toast.LENGTH_LONG).show()
                    showProcessingIndicator = false
                    if (bitmapForOcr?.isRecycled == false) { // Clean up if error occurred after OCR bitmap creation
                        bitmapForOcr.recycle()
                    }
                    // originalBitmap is handled by BitmapUtils or if it was null, nothing to recycle.
                    onCancel() // Critical error, cancel the flow
                }
            }
        } else {
            Log.d("GalleryImagePicker", "Image selection cancelled or no URI returned.")
            onCancel()
        }
    }

    // UI: Shows a processing indicator or a cancel button.
    // The image picker is launched via LaunchedEffect below.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f)), // Semi-transparent background
        contentAlignment = Alignment.Center
    ) {
        if (showProcessingIndicator) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Processing Image...", color = Color.White)
            }
        }

        // General cancel button for the screen.
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopStart) {
            IconButton(onClick = {
                if (!showProcessingIndicator) {
                    Log.d("GalleryImagePickerScreen", "Cancel button clicked on screen.")
                    onCancel()
                }
            }, modifier = Modifier.padding(16.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color.White)
            }
        }
    }

    // LaunchedEffect to trigger the image picker once when the composable enters composition.
    LaunchedEffect(key1 = Unit) { // key1 = Unit ensures it runs only once on initial composition
        if (!hasLaunchedPicker) {
            Log.d("GalleryImagePicker", "Launching image picker via LaunchedEffect.")
            imagePickerLauncher.launch("image/*")
            hasLaunchedPicker = true // Set flag to prevent re-launch if recomposition occurs for other reasons
        }
    }
}