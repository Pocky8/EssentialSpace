package com.essential.essspace.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface NoteDao {

    // Insert a new note
    @Insert
    suspend fun insert(note: Note)

    // Get all notes ordered by timestamp
    @Query("SELECT * FROM notes ORDER BY timestamp DESC")
    suspend fun getAllNotes(): List<Note>

    // Get a note by ID
    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: Long): Note?

    // Delete a note by ID
    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNoteById(id: Long)
}
