package com.essential.essspace

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import java.io.File

@Composable
fun CameraScreen(
    onPhotoTaken: (String) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    // create the file in the cache or externalFilesDir
    val imageFile = File(context.cacheDir, "captured_image.jpg")

    val photoUri: Uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider",
        imageFile
    )

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            // Return the file path so AudioRecordScreen can store them together
            onPhotoTaken(imageFile.absolutePath)
        } else {
            onCancel()
        }
    }

    LaunchedEffect(Unit) {
        launcher.launch(photoUri)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.TopStart
    ) {
        IconButton(onClick = onCancel, modifier = Modifier.padding(16.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color.White)
        }
    }
}