package com.essential.essspace

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import java.io.File
import java.io.IOException

@Composable
fun AudioRecordScreen(
    photoPath: String?,  // Renamed from photoUri for clarity
    onComplete: (audioPath: String?) -> Unit, // Callback now includes audioPath
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    var recorder: MediaRecorder? by remember { mutableStateOf(null) }
    var audioFile: File? by remember { mutableStateOf(null) }
    var recording by remember { mutableStateOf(false) }
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasAudioPermission = granted
            if (!granted) {
                Toast.makeText(context, "Audio permission denied", Toast.LENGTH_SHORT).show()
                onCancel() // Or handle appropriately
            }
        }
    )

    LaunchedEffect(key1 = true) {
        if (!hasAudioPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    fun createAudioFile(): File {
        val audioNotesDir = File(context.filesDir, "audio_notes")
        if (!audioNotesDir.exists()) {
            audioNotesDir.mkdirs()
        }
        return File(audioNotesDir, "audio_${System.currentTimeMillis()}.mp4")
    }

    fun startRecording() {
        if (!hasAudioPermission) {
            Toast.makeText(context, "Audio permission required", Toast.LENGTH_SHORT).show()
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }
        audioFile = createAudioFile()
        recorder = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }).apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(audioFile!!.absolutePath)
            try {
                prepare()
                start()
                recording = true
            } catch (e: IOException) {
                Log.e("AudioRecordScreen", "MediaRecorder prepare() failed", e)
                Toast.makeText(context, "Failed to start recording: ${e.message}", Toast.LENGTH_LONG).show()
                // Clean up
                this.release()
                recorder = null
                audioFile?.delete()
                audioFile = null
            }
        }
    }

    fun stopRecording(saveFile: Boolean) {
        try {
            recorder?.apply {
                stop()
                release()
            }
        } catch (e: IllegalStateException) {
            Log.e("AudioRecordScreen", "MediaRecorder stop/release failed", e)
            // It might have already been stopped or not started properly
        }
        recorder = null
        recording = false
        if (!saveFile) {
            audioFile?.delete()
            audioFile = null
        }
    }

    // Cleanup recorder if the composable is disposed while recording
    DisposableEffect(Unit) {
        onDispose {
            if (recording) {
                stopRecording(saveFile = false) // Don't save if screen is left abruptly
            }
        }
    }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (!hasAudioPermission) {
            Text("Audio permission is required to record audio.")
            Button(onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }) {
                Text("Grant Permission")
            }
        } else {
            Button(onClick = {
                if (!recording) {
                    startRecording()
                } else {
                    stopRecording(saveFile = true) // Keep file when user explicitly stops
                }
            }) {
                Text(if (recording) "Stop Recording" else "Record Audio")
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    if (recording) {
                        stopRecording(saveFile = true)
                    }
                    onComplete(audioFile?.absolutePath) // Pass the path
                },
                enabled = !recording || audioFile != null // Enable if not recording OR if recording and file exists
            ) {
                Text("Save & Return")
            }

            Spacer(Modifier.height(16.dp))

            Button(onClick = {
                if (recording) {
                    stopRecording(saveFile = false) // Don't save if cancelled
                }
                onCancel()
            }) {
                Text("Cancel")
            }
        }
    }
}