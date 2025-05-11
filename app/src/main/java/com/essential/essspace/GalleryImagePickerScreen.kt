package com.essential.essspace

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
    var hasLaunchedPicker by remember { mutableStateOf(false) }


    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            showProcessingIndicator = true
            Log.d("GalleryImagePicker", "Image URI selected: $uri")
            coroutineScope.launch {
                try {
                    // Copy URI content to a local file in cache
                    val imageFile = File(context.cacheDir, "gallery_image_${System.currentTimeMillis()}.jpg")
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { inputStream ->
                            FileOutputStream(imageFile).use { outputStream ->
                                inputStream.copyTo(outputStream)
                            }
                        }
                    }
                    Log.d("GalleryImagePicker", "Image copied to: ${imageFile.absolutePath}")

                    val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
                    if (bitmap != null) {
                        val image = InputImage.fromBitmap(bitmap, 0)
                        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

                        recognizer.process(image)
                            .addOnSuccessListener { visionText ->
                                val ocrResult = visionText.text
                                Log.d("GalleryImagePicker", "ML Kit OCR successful. Text: ${ocrResult.take(100)}")
                                showProcessingIndicator = false
                                onImageProcessed(imageFile.absolutePath, ocrResult.ifBlank { null })
                            }
                            .addOnFailureListener { e ->
                                Log.e("GalleryImagePicker", "ML Kit OCR failed for gallery image", e)
                                Toast.makeText(context, "OCR failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                showProcessingIndicator = false
                                onImageProcessed(imageFile.absolutePath, null) // Proceed with image, null OCR
                            }
                            .addOnCompleteListener {
                                if (!bitmap.isRecycled) {
                                    bitmap.recycle()
                                }
                            }
                    } else {
                        Log.e("GalleryImagePicker", "Failed to decode bitmap from file.")
                        Toast.makeText(context, "Failed to process image.", Toast.LENGTH_SHORT).show()
                        showProcessingIndicator = false
                        onImageProcessed(imageFile.absolutePath, null) // Proceed with image, null OCR
                    }
                } catch (e: Exception) {
                    Log.e("GalleryImagePicker", "Error processing gallery image", e)
                    Toast.makeText(context, "Error processing image: ${e.message}", Toast.LENGTH_LONG).show()
                    showProcessingIndicator = false
                    onCancel() // Critical error, cancel the flow
                }
            }
        } else {
            Log.d("GalleryImagePicker", "Image selection cancelled or failed.")
            onCancel()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasLaunchedPicker) {
            imagePickerLauncher.launch("image/*")
            hasLaunchedPicker = true
        }
    }

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
        // IconButton for cancel should be at TopStart
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopStart) {
            IconButton(onClick = {
                if (!showProcessingIndicator) {
                    onCancel()
                }
            }, modifier = Modifier.padding(16.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color.White)
            }
        }
    }
}