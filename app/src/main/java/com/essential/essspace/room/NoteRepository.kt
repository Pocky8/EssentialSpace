package com.essential.essspace.room

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow // Import Flow
import kotlinx.coroutines.withContext

class NoteRepository(context: Context) {

    private val noteDao = AppDatabase.getDatabase(context.applicationContext).noteDao() // Use applicationContext

    suspend fun insertNote(note: Note): Long { // Return Long
        return withContext(Dispatchers.IO) {
            noteDao.insert(note)
        }
    }

    // Get all notes as a Flow
    fun getAllNotesFlow(): Flow<List<Note>> { // No suspend, returns Flow directly
        return noteDao.getAllNotesFlow()
    }

    suspend fun getNoteById(id: Long): Note? {
        return withContext(Dispatchers.IO) {
            noteDao.getNoteById(id)
        }
    }

    suspend fun deleteNoteById(id: Long) {
        withContext(Dispatchers.IO) {
            noteDao.deleteNoteById(id)
        }
    }
}