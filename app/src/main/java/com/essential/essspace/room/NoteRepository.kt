package com.essential.essspace.room

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NoteRepository(context: Context) {

    // Get the DAO from AppDatabase
    private val noteDao = AppDatabase.getDatabase(context).noteDao()

    // Insert a new note (with photoUri, audioUri, etc.)
    suspend fun insertNote(note: Note) {
        withContext(Dispatchers.IO) {
            noteDao.insert(note)
        }
    }

    // Get all notes sorted by timestamp
    suspend fun getAllNotes(): List<Note> {
        return withContext(Dispatchers.IO) {
            noteDao.getAllNotes()
        }
    }

    // Retrieve a note by its ID
    suspend fun getNoteById(id: Long): Note? {
        return withContext(Dispatchers.IO) {
            noteDao.getNoteById(id)
        }
    }

    // Delete a note by its ID
    suspend fun deleteNoteById(id: Long) {
        withContext(Dispatchers.IO) {
            noteDao.deleteNoteById(id)
        }
    }
}
