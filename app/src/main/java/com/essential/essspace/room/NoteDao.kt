package com.essential.essspace.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow // Import Flow

@Dao
interface NoteDao {

    // Insert a new note
    @Insert(onConflict = OnConflictStrategy.REPLACE) // It's good practice to define a conflict strategy
    suspend fun insert(note: Note): Long // Return Long (rowId)

    // Get all notes ordered by timestamp, returning a Flow
    @Query("SELECT * FROM notes ORDER BY timestamp DESC")
    fun getAllNotesFlow(): Flow<List<Note>> // Changed to return Flow

    // Get a note by ID
    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: Long): Note?

    // Delete a note by ID
    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNoteById(id: Long)
}