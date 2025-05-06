package com.essential.essspace

import android.Manifest
import android.content.Context
import android.media.MediaRecorder
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.google.accompanist.permissions.*
import java.io.File
import java.io.IOException

class AndroidAudioRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var outputFile: String? = null

    fun startRecording() {
        outputFile = "${context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)?.absolutePath}/audiorecordtest.3gp"
        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setOutputFile(outputFile)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)

            try {
                prepare()
                start()
            } catch (e: IOException) {
                Log.e("AudioRecord", "prepare() failed: ${e.message}")
            }
        }
    }

    fun stopRecording() {
        recorder?.apply {
            stop()
            release()
        }
        recorder = null
    }

    fun getOutputFile(): File? {
        return outputFile?.let { File(it) }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AudioRecordScreen(
    photoUri: String?,
    onComplete: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val audioRecorder = remember { AndroidAudioRecorder(context) }
    val isRecording = remember { mutableStateOf(false) }
    var hasRecorded = remember { mutableStateOf(false) }

    val micPermissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)

    LaunchedEffect(Unit) {
        if (!micPermissionState.status.isGranted) {
            micPermissionState.launchPermissionRequest()
        }
    }

    // Clean up when leaving the screen
    DisposableEffect(key1 = Unit) {
        onDispose {
            if (isRecording.value) {
                audioRecorder.stopRecording()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCancel) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cancel"
                )
            }

            Text(
                text = "Add Voice Note",
                style = MaterialTheme.typography.titleLarge
            )

            TextButton(
                onClick = onComplete,
                enabled = hasRecorded.value && !isRecording.value
            ) {
                Text("Save")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Photo preview
        if (!photoUri.isNullOrEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .clip(RoundedCornerShape(16.dp)),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Image(
                    painter = rememberAsyncImagePainter(Uri.parse(photoUri)),
                    contentDescription = "Captured photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Recording UI
        if (!micPermissionState.status.isGranted) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "Microphone permission is required to record audio.",
                    color = MaterialTheme.colorScheme.error
                )

                Button(
                    onClick = { micPermissionState.launchPermissionRequest() },
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text("Grant Permission")
                }
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                Text(
                    if (isRecording.value) "Recording..."
                    else if (hasRecorded.value) "Recording complete!"
                    else "Tap to start recording",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Button(
                    onClick = {
                        if (isRecording.value) {
                            audioRecorder.stopRecording()
                            isRecording.value = false
                            hasRecorded.value = true
                        } else {
                            audioRecorder.startRecording()
                            isRecording.value = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRecording.value)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.size(80.dp)
                ) {
                    Text(if (isRecording.value) "Stop" else "Record")
                }
            }
        }
    }
}