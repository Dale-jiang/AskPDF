package com.ctf.askpdf.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ctf.askpdf.document.model.DocumentFile

@Database(entities = [DocumentFile::class], version = 1, exportSchema = false)
abstract class AskPdfDatabase : RoomDatabase() {

    abstract fun documentFileDao(): DocumentFileDao

    companion object {
        @Volatile
        private var instance: AskPdfDatabase? = null

        fun getInstance(context: Context): AskPdfDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AskPdfDatabase::class.java,
                    "askpdf_document.db"
                ).build().also { instance = it }
            }
        }
    }
}
