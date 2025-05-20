package com.essential.essspace.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters // Ensure this import if Converters are in the same package, else adjust

@Entity(tableName = "notes")
// Add @TypeConverters(Converters::class) if your Converters class is defined
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    var title: String? = null,
    val photoPath: String?,
    val audioPath: String?,
    var text: String?, // This will store Markdown content
    var transcribedText: String? = null,
    var ocrText: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    // New PKM fields
    var summary: String? = null, // For AI-generated summary
    var tags: List<String> = emptyList(), // For categorization
    var links: List<String> = emptyList(), // Store IDs or unique titles of linked notes
    var isMarkdown: Boolean = true, // Default to Markdown
    var lastModified: Long = System.currentTimeMillis() // Track last modification
)