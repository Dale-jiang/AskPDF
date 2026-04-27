package com.ctf.askpdf.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.ctf.askpdf.document.model.DocumentFile
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentFileDao {

    @Upsert
    suspend fun upsert(file: DocumentFile)

    @Query("SELECT * FROM document_file WHERE path = :path LIMIT 1")
    suspend fun findByPath(path: String): DocumentFile?

    @Query("SELECT * FROM document_file WHERE recentViewTime > 0 ORDER BY recentViewTime DESC")
    fun observeRecentFiles(): Flow<List<DocumentFile>>

    @Query("SELECT * FROM document_file WHERE collected = 1 ORDER BY collectedAt DESC")
    fun observeCollectionFiles(): Flow<List<DocumentFile>>
}
