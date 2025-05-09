package com.essential.essspace.room

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class NoteRepository(context: Context) {

    private val noteDao = AppDatabase.getDatabase(context.applicationContext).noteDao()

    suspend fun insertNote(note: Note): Long {
        return withContext(Dispatchers.IO) {
            noteDao.insert(note)
        }
    }

    fun getAllNotesFlow(): Flow<List<Note>> {
        return noteDao.getAllNotesFlow()
    }

    // Get a single note by ID as Flow
    fun getNoteByIdFlow(id: Int): Flow<Note?> { // Changed to Int ID
        return noteDao.getNoteByIdFlow(id)
    }

    // Update a note
    suspend fun updateNote(note: Note) {
        withContext(Dispatchers.IO) {
            noteDao.update(note)
        }
    }

    suspend fun deleteNoteById(id: Int) { // Changed to Int ID
        withContext(Dispatchers.IO) {
            noteDao.deleteNoteById(id)
        }
    }
}