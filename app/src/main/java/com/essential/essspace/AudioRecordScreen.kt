package com.essential.essspace

import android.content.Context
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.essential.essspace.room.Note
import com.essential.essspace.room.NoteRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AudioRecordScreen(
    photoUri: String?,
    onComplete: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var audioFile by remember { mutableStateOf<File?>(null) }
    var seconds by remember { mutableStateOf(0) }
    var isRecording by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val tmp = File(context.cacheDir, "AUDIO_$ts.m4a")
        audioFile = tmp
        recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            MediaRecorder(context)
        else @Suppress("DEPRECATION") MediaRecorder()
        recorder!!.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(tmp.absolutePath)
            prepare()
            start()
        }
        isRecording = true
        while (isRecording) {
            delay(1_000)
            seconds++
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            recorder?.run { try { stop() } catch (_: Exception) {}; release() }
            isRecording = false
        }
    }

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            "Recording: ${"%02d:%02d".format(seconds / 60, seconds % 60)}",
            style = MaterialTheme.typography.headlineMedium
        )
        Icon(
            Icons.Default.Mic, contentDescription = null,
            tint = if (isRecording) MaterialTheme.colorScheme.error else LocalContentColor.current,
            modifier = Modifier.size(96.dp)
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Button(
                onClick = {
                    recorder?.run { try { stop() } catch (_: Exception) {}; release() }
                    isRecording = false
                    onCancel()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Close, null)
                Spacer(Modifier.width(8.dp))
                Text("Cancel")
            }
            Button(onClick = {
                recorder?.run { try { stop() } catch (_: Exception) {}; release() }
                isRecording = false
                audioFile?.let { saveNote(context, photoUri, it) }
                onComplete()
            }) {
                Icon(Icons.Default.Check, null)
                Spacer(Modifier.width(8.dp))
                Text("Save")
            }
        }
    }
}

private fun saveNote(
    context: Context,
    photoUriString: String?,
    audioFile: File
) {
    try {
        val outDir = context.getExternalFilesDir("EssSpace")!!.apply { if (!exists()) mkdirs() }
        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        // copy audio
        val audioOut = File(outDir, "AUDIO_$ts.m4a")
        audioFile.copyTo(audioOut, overwrite = true)
        // copy image via ContentResolver
        val photoOut = File(outDir, "PHOTO_$ts.jpg")
        photoUriString
            ?.let { Uri.parse(it) }
            ?.let { uri ->
                context.contentResolver.openInputStream(uri)?.use { input ->
                    photoOut.outputStream().use { output -> input.copyTo(output) }
                }
            }
        // insert into Room
        val note = Note(
            photoPath = photoOut.absolutePath,
            audioPath = audioOut.absolutePath,
            text = null
        )
        CoroutineScope(Dispatchers.IO).launch {
            NoteRepository(context).insertNote(note)
            Log.d("AudioRecordScreen","Inserted note: $note")
        }
    } catch (e: Exception) {
        Log.e("AudioRecordScreen","saveNote failed", e)
    }
}