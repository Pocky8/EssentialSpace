package com.essential.essspace.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Note::class], version = 4, exportSchema = false) // Incremented version
@TypeConverters(Converters::class) // Add TypeConverters
abstract class AppDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN title TEXT")
            }
        }

        // New migration from version 2 to 3
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN transcribedText TEXT")
                db.execSQL("ALTER TABLE notes ADD COLUMN ocrText TEXT")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN summary TEXT")
                db.execSQL("ALTER TABLE notes ADD COLUMN tags TEXT NOT NULL DEFAULT '[]'") // Store as JSON string
                db.execSQL("ALTER TABLE notes ADD COLUMN links TEXT NOT NULL DEFAULT '[]'") // Store as JSON string
                db.execSQL("ALTER TABLE notes ADD COLUMN isMarkdown INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE notes ADD COLUMN lastModified INTEGER NOT NULL DEFAULT ${System.currentTimeMillis()}")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "essential_space_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4) // Add the new migration
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}