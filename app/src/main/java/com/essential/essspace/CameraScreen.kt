package com.essential.essspace

import android.net.Uri
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
import androidx.core.content.FileProvider
import java.io.File

@Composable
fun CameraScreen(
    onPhotoTaken: (Uri) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current

    // Create image file in cache directory
    val imageFile = File(context.cacheDir, "captured_image.jpg")

    // Convert to content:// URI using FileProvider
    val photoUri: Uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider", // MUST match manifest
        imageFile
    )

    // Launcher for camera
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            onPhotoTaken(photoUri)
        } else {
            onCancel()
        }
    }

    // Launch camera on first composition
    LaunchedEffect(Unit) {
        launcher.launch(photoUri)
    }

    // UI with a cancel button in case user comes back manually
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.TopStart
    ) {
        IconButton(
            onClick = onCancel,
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color.White)
        }
    }
}
