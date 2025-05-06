package com.essential.essspace.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val photoPath: String?,
    val audioPath: String?,
    val text: String?,
    val timestamp: Long = System.currentTimeMillis()  // <-- Add this
)


