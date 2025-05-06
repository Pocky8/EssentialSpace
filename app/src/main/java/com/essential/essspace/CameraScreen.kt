package com.essential.essspace
import android.content.ContentValues
import android.net.Uri
import android.provider.MediaStore
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    onPhotoTaken: (Uri) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    val cameraPermissionState = rememberPermissionState(
        android.Manifest.permission.CAMERA
    )

    // Camera executor
    val cameraExecutor = remember {
        Executors.newSingleThreadExecutor()
    }

    // Camera capture use case
    val imageCapture = remember {
        ImageCapture.Builder().build()
    }

    // UI state
    var isCapturing by remember { mutableStateOf(false) }
    var showPermissionDeniedMessage by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        AnimatedVisibility(
            visible = cameraPermissionState.status.isGranted,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Camera preview
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()

                            // Preview use case
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }

                            // Select back camera
                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                            try {
                                // Unbind all use cases before rebinding
                                cameraProvider.unbindAll()

                                // Bind camera to lifecycle
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview,
                                    imageCapture
                                )
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }, ContextCompat.getMainExecutor(ctx))

                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Camera controls
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 50.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Capture button
                    Button(
                        onClick = {
                            if (isCapturing) return@Button

                            isCapturing = true

                            // Create output options
                            val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
                                .format(System.currentTimeMillis())

                            val contentValues = ContentValues().apply {
                                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                            }

                            // Create output options object
                            val outputOptions = ImageCapture.OutputFileOptions
                                .Builder(context.contentResolver,
                                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                    contentValues)
                                .build()

                            // Capture the image
                            imageCapture.takePicture(
                                outputOptions,
                                cameraExecutor,
                                object : ImageCapture.OnImageSavedCallback {
                                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                        val savedUri = output.savedUri
                                        if (savedUri != null) {
                                            coroutineScope.launch {
                                                isCapturing = false
                                                onPhotoTaken(savedUri)
                                            }
                                        }
                                    }

                                    override fun onError(exception: ImageCaptureException) {
                                        exception.printStackTrace()
                                        isCapturing = false
                                    }
                                }
                            )
                        },
                        shape = CircleShape,
                        modifier = Modifier.size(80.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Camera,
                            contentDescription = "Take photo",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // Close button
                Box(
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.TopEnd)
                ) {
                    IconButton(
                        onClick = { onCancel() },
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close camera",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Permission denied UI
        AnimatedVisibility(
            visible = !cameraPermissionState.status.isGranted || showPermissionDeniedMessage,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Text(
                        "Camera Permission Required",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        "Essential Space needs camera access to capture photos. Please grant the permission to continue.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Button(
                        onClick = {
                            cameraPermissionState.launchPermissionRequest()
                        }
                    ) {
                        Text("Grant Permission")
                    }
                    TextButton(
                        onClick = { onCancel() }
                    ) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}