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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun CameraScreen(
    // Updated signature to include OCR text
    onPhotoTaken: (photoPath: String, ocrText: String?) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showProcessingIndicator by remember { mutableStateOf(false) }

    val imageFile = remember { File(context.cacheDir, "captured_image_${System.currentTimeMillis()}.jpg") }

    val photoUri: Uri = remember(imageFile) {
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider", // Make sure this matches your manifest
            imageFile
        )
    }

    val takePictureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            showProcessingIndicator = true
            Log.d("CameraScreen", "Photo taken successfully. Path: ${imageFile.absolutePath}")
            coroutineScope.launch { // Perform OCR in a coroutine
                try {
                    val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
                    if (bitmap != null) {
                        val image = InputImage.fromBitmap(bitmap, 0) // Assuming 0 rotation
                        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

                        recognizer.process(image)
                            .addOnSuccessListener { visionText ->
                                val ocrResult = visionText.text
                                Log.d("CameraScreen", "ML Kit OCR successful. Text: ${ocrResult.take(100)}")
                                showProcessingIndicator = false
                                // Instead of navigating directly to Audio, set the captured data:
                                onPhotoTaken(imageFile.absolutePath, ocrResult.ifBlank { null })
                            }
                            .addOnFailureListener { e ->
                                Log.e("CameraScreen", "ML Kit OCR failed for camera image", e)
                                Toast.makeText(context, "OCR failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                showProcessingIndicator = false
                                onPhotoTaken(imageFile.absolutePath, null) // Proceed with photo, null OCR
                            }
                            .addOnCompleteListener {
                                bitmap.recycle() // Recycle bitmap after processing
                            }
                    } else {
                        Log.e("CameraScreen", "Failed to decode bitmap from file.")
                        Toast.makeText(context, "Failed to process image.", Toast.LENGTH_SHORT).show()
                        showProcessingIndicator = false
                        onPhotoTaken(imageFile.absolutePath, null) // Proceed with photo, null OCR
                    }
                } catch (e: Exception) {
                    Log.e("CameraScreen", "Error processing image for OCR", e)
                    Toast.makeText(context, "Error processing image.", Toast.LENGTH_SHORT).show()
                    showProcessingIndicator = false
                    onPhotoTaken(imageFile.absolutePath, null)
                }
            }
        } else {
            Log.d("CameraScreen", "Photo capture cancelled or failed.")
            onCancel()
        }
    }

    LaunchedEffect(Unit) {
        // Ensure the file exists or can be created before launching
        try {
            if (!imageFile.exists()) {
                imageFile.createNewFile()
            }
            takePictureLauncher.launch(photoUri)
        } catch (e: Exception) {
            Log.e("CameraScreen", "Error preparing image file or launching camera", e)
            Toast.makeText(context, "Could not start camera.", Toast.LENGTH_SHORT).show()
            onCancel()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black), // Keep background black for camera preview
        contentAlignment = Alignment.Center // Center the processing indicator
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
                if (!showProcessingIndicator) { // Allow cancel if not processing
                    onCancel()
                }
            }, modifier = Modifier.padding(16.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color.White)
            }
        }
    }
}