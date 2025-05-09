package com.essential.essspace

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer // Keep for isRecognitionAvailable
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.util.Locale

@Composable
fun AudioRecordScreen(
    photoPath: String?, // Kept for context if needed for title generation
    onComplete: (audioPath: String?, transcribedText: String?) -> Unit, // audioPath will always be null
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    var showProcessingMessage by remember { mutableStateOf(false) }

    val speechRecognitionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        showProcessingMessage = false
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val speechResults = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!speechResults.isNullOrEmpty()) {
                val transcribedText = speechResults[0]
                Log.d("AudioRecordScreen", "Transcription successful via RecognizerIntent: $transcribedText")
                onComplete(null, transcribedText) // audioPath is null
            } else {
                Log.d("AudioRecordScreen", "RecognizerIntent: No speech results found.")
                Toast.makeText(context, "No transcription results.", Toast.LENGTH_SHORT).show()
                onComplete(null, null)
            }
        } else {
            Log.d("AudioRecordScreen", "RecognizerIntent: Speech recognition cancelled or failed. ResultCode: ${result.resultCode}")
            val errorMessage = if (result.resultCode == Activity.RESULT_CANCELED) {
                "Speech input cancelled."
            } else {
                "Speech input failed. Please try again."
            }
            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
            onComplete(null, null)
        }
    }

    fun startSystemSpeechToText() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Toast.makeText(context, "Speech recognition not available on this device.", Toast.LENGTH_LONG).show()
            onCancel()
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toString())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now to create a note...")
        }

        try {
            Log.d("AudioRecordScreen", "Launching RecognizerIntent activity.")
            speechRecognitionLauncher.launch(intent)
            showProcessingMessage = true
        } catch (e: Exception) {
            Log.e("AudioRecordScreen", "Error launching RecognizerIntent activity", e)
            Toast.makeText(context, "Could not start speech recognition service.", Toast.LENGTH_SHORT).show()
            showProcessingMessage = false
            onComplete(null, null)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("AudioRecordScreen", "RECORD_AUDIO permission granted for RecognizerIntent.")
            startSystemSpeechToText()
        } else {
            Log.w("AudioRecordScreen", "RECORD_AUDIO permission denied for RecognizerIntent.")
            Toast.makeText(context, "Audio permission needed for transcription.", Toast.LENGTH_LONG).show()
            onCancel()
        }
    }

    fun requestPermissionAndStart() {
        when (PackageManager.PERMISSION_GRANTED) {
            context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) -> {
                startSystemSpeechToText()
            }
            else -> {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (photoPath != null) {
            Text("Transcribe note for the photo", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))
        }

        if (showProcessingMessage) {
            Text("Listening via system dialog...")
            Spacer(Modifier.height(16.dp))
            CircularProgressIndicator()
        } else {
            Button(onClick = { requestPermissionAndStart() }) {
                Icon(Icons.Filled.Mic, contentDescription = "Start Transcription")
                Spacer(Modifier.width(8.dp))
                Text("Transcribe Speech to Note")
            }
        }

        Spacer(Modifier.height(20.dp))
        OutlinedButton(onClick = {
            Log.d("AudioRecordScreen", "Cancel button clicked.")
            onCancel()
        }) {
            Text("Cancel")
        }
    }
}