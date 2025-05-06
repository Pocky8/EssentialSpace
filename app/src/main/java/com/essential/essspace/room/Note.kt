package com.essential.essspace.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val photoUri: String,
    val audioUri: String,
    val textNote: String? = null,
    val timestamp: Long = System.currentTimeMillis()  // Timestamp to order notes
)
