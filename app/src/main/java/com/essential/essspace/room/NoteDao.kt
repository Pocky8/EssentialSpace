package com.essential.essspace.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update // Import Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: Note): Long

    @Query("SELECT * FROM notes ORDER BY timestamp DESC")
    fun getAllNotesFlow(): Flow<List<Note>>

    // Get a note by ID, returning a Flow
    @Query("SELECT * FROM notes WHERE id = :id")
    fun getNoteByIdFlow(id: Int): Flow<Note?> // Changed to Int ID and returns Flow

    // Update an existing note
    @Update
    suspend fun update(note: Note)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNoteById(id: Int) // Changed to Int ID
}