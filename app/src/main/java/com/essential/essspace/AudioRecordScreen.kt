package com.essential.essspace

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
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
import com.essential.essspace.util.SummarizationUtils
import java.util.Locale

@Composable
fun AudioRecordScreen(
    photoPath: String?, // Keep photoPath if you want to associate transcription with a photo
    onComplete: (audioPath: String?, transcribedText: String?) -> Unit,
    onSkipAudio: () -> Unit,
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
                val rawTranscribedText = speechResults[0]
                Log.d("AudioRecordScreen", "Transcription successful: $rawTranscribedText")
                val bulletedText = SummarizationUtils.summarizeAndBulletText(rawTranscribedText)
                Log.d("AudioRecordScreen", "Bulleted Text: $bulletedText")
                onComplete(null, bulletedText) // audioPath is null
            } else {
                Log.d("AudioRecordScreen", "No speech results found.")
                Toast.makeText(context, "No transcription results.", Toast.LENGTH_SHORT).show()
                onComplete(null, null)
            }
        } else {
            Log.d("AudioRecordScreen", "Speech recognition cancelled or failed. ResultCode: ${result.resultCode}")
            val errorMessage = if (result.resultCode == Activity.RESULT_CANCELED) "Speech input cancelled." else "Speech input failed."
            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
            onComplete(null, null)
        }
    }

    fun startSystemSpeechToText() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Toast.makeText(context, "Speech recognition not available.", Toast.LENGTH_LONG).show()
            onComplete(null, null)
            return
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toString())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now to transcribe...")
        }
        try {
            Log.d("AudioRecordScreen", "Launching RecognizerIntent for transcription.")
            speechRecognitionLauncher.launch(intent)
            // showProcessingMessage = true; // System dialog is usually sufficient
        } catch (e: Exception) {
            Log.e("AudioRecordScreen", "Error launching RecognizerIntent", e)
            Toast.makeText(context, "Could not start speech recognition.", Toast.LENGTH_SHORT).show()
            showProcessingMessage = false
            onComplete(null, null)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (photoPath != null) {
            Text("Transcribe audio for the photo", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))
        } else {
            Text("Transcribe Audio", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))
        }

        if (showProcessingMessage) { // General processing message
            Text("Processing speech...")
            Spacer(Modifier.height(16.dp))
            CircularProgressIndicator()
            Spacer(Modifier.height(16.dp))
        }

        Button(onClick = {
            // No recording, directly start speech to text
            startSystemSpeechToText()
        }) {
            Icon(
                imageVector = Icons.Filled.Mic,
                contentDescription = "Start Transcription"
            )
            Spacer(Modifier.width(8.dp))
            Text("Start Transcription")
        }

        Spacer(Modifier.height(20.dp))
        OutlinedButton(onClick = {
            Log.d("AudioRecordScreen", "Skip Transcription button clicked.")
            onSkipAudio()
        }) {
            Text("Skip Transcription")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = {
            Log.d("AudioRecordScreen", "Cancel button clicked.")
            onCancel()
        }) {
            Text("Cancel")
        }
    }
}