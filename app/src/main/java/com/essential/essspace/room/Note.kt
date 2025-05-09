package com.essential.essspace.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    var title: String? = null,
    val photoPath: String?,
    val audioPath: String?,
    var text: String?,
    var transcribedText: String? = null, // Added for speech-to-text
    var ocrText: String? = null,         // Added for OCR
    val timestamp: Long = System.currentTimeMillis()
)