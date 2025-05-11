package com.essential.essspace

import android.graphics.BitmapFactory
import android.graphics.Bitmap
import com.essential.essspace.BitmapUtils
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
            "${context.packageName}.provider",
            imageFile
        )
    }

    val takePictureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            showProcessingIndicator = true
            Log.d("CameraScreen", "Photo taken successfully. Path: ${imageFile.absolutePath}")
            coroutineScope.launch {
                var originalBitmap: Bitmap? = null
                var bitmapForOcr: Bitmap? = null // Declare bitmapForOcr here
                try {
                    originalBitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
                    if (originalBitmap != null) {
                        // Resize the bitmap for OCR
                        bitmapForOcr = BitmapUtils.getResizedBitmapForOcr(originalBitmap)

                        if (bitmapForOcr == null) {
                            Log.e("CameraScreen", "Bitmap for OCR is null after resize attempt.")
                            Toast.makeText(context, "Failed to process image (resize error).", Toast.LENGTH_SHORT).show()
                            showProcessingIndicator = false
                            onPhotoTaken(imageFile.absolutePath, null)
                            return@launch
                        }

                        val image = InputImage.fromBitmap(bitmapForOcr!!, 0) // Use the (potentially resized) bitmapForOcr
                        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

                        recognizer.process(image)
                            .addOnSuccessListener { visionText ->
                                val ocrResult = visionText.text
                                Log.d("CameraScreen", "ML Kit OCR successful. Text: ${ocrResult.take(100)}")
                                showProcessingIndicator = false
                                onPhotoTaken(imageFile.absolutePath, ocrResult.ifBlank { null })
                            }
                            .addOnFailureListener { e ->
                                Log.e("CameraScreen", "ML Kit OCR failed for camera image", e)
                                Toast.makeText(context, "OCR failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                showProcessingIndicator = false
                                onPhotoTaken(imageFile.absolutePath, null)
                            }
                            .addOnCompleteListener {
                                // Recycle the bitmap that was used for OCR
                                if (bitmapForOcr?.isRecycled == false) {
                                    bitmapForOcr.recycle()
                                    Log.d("CameraScreen", "bitmapForOcr recycled in onComplete.")
                                }
                                // originalBitmap is recycled by BitmapUtils.getResizedBitmapForOcr if a new bitmap was created
                            }
                    } else {
                        Log.e("CameraScreen", "Failed to decode original bitmap from file.")
                        Toast.makeText(context, "Failed to process image.", Toast.LENGTH_SHORT).show()
                        showProcessingIndicator = false
                        onPhotoTaken(imageFile.absolutePath, null)
                    }
                } catch (e: Exception) {
                    Log.e("CameraScreen", "Error processing image for OCR", e)
                    Toast.makeText(context, "Error processing image: ${e.message}", Toast.LENGTH_SHORT).show()
                    showProcessingIndicator = false
                    onPhotoTaken(imageFile.absolutePath, null)
                    // Ensure bitmapForOcr is recycled in case of an earlier exception
                    if (bitmapForOcr?.isRecycled == false) {
                        bitmapForOcr.recycle()
                        Log.d("CameraScreen", "bitmapForOcr recycled in catch block.")
                    }
                    // If originalBitmap was decoded but not passed to BitmapUtils or if BitmapUtils didn't recycle it (e.g. no resize)
                    // and it's different from bitmapForOcr, it might also need recycling here.
                    // However, getResizedBitmapForOcr should handle the original.
                }
            }
        } else {
            Log.d("CameraScreen", "Photo capture cancelled or failed.")
            onCancel()
        }
    }

    LaunchedEffect(Unit) {
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
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (showProcessingIndicator) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Processing Image...", color = Color.White)
            }
        }
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