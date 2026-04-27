package com.ctf.askpdf.feature.document

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ctf.askpdf.data.askPdfDatabase
import com.ctf.askpdf.document.model.DocumentFile
import com.ctf.askpdf.document.model.supportedMimeTypes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File

class DocumentHubViewModel : ViewModel() {

    val permissionMissingLiveData = MutableLiveData<Boolean>()
    val requestPermissionLiveData = MutableLiveData<Boolean>()
    val scannedFilesLiveData = MutableLiveData<List<DocumentFile>>()
    val recentFilesLiveData = MutableLiveData<List<DocumentFile>>()
    val collectionFilesLiveData = MutableLiveData<List<DocumentFile>>()

    /**
     * 扫描本机文档文件，并合并数据库中的最近打开和收藏状态。
     */
    fun scanDocuments(context: Context) {
        viewModelScope.launch(Dispatchers.IO + SupervisorJob()) {
            val scannedFiles = queryDocuments(context.contentResolver)
            scannedFilesLiveData.postValue(scannedFiles)
        }
    }

    /**
     * 监听最近打开文件列表变化。
     */
    fun observeRecentFiles() {
        viewModelScope.launch(Dispatchers.IO + SupervisorJob()) {
            askPdfDatabase.documentFileDao().observeRecentFiles().collectLatest {
                recentFilesLiveData.postValue(it)
            }
        }
    }

    /**
     * 监听收藏文件列表变化。
     */
    fun observeCollectionFiles() {
        viewModelScope.launch(Dispatchers.IO + SupervisorJob()) {
            askPdfDatabase.documentFileDao().observeCollectionFiles().collectLatest {
                collectionFilesLiveData.postValue(it)
            }
        }
    }

    /**
     * 记录文件打开时间，用于 Recent Files tab 排序。
     */
    fun markAsRecent(file: DocumentFile) {
        viewModelScope.launch(Dispatchers.IO + SupervisorJob()) {
            val saved = askPdfDatabase.documentFileDao().findByPath(file.path) ?: file
            askPdfDatabase.documentFileDao().upsert(
                saved.copy(
                    displayName = file.displayName,
                    mimeType = file.mimeType,
                    size = file.size,
                    dateAdded = file.dateAdded,
                    recentViewTime = System.currentTimeMillis()
                )
            )
        }
    }

    /**
     * 切换收藏状态，影响 Collection tab 的数据。
     */
    fun toggleCollection(file: DocumentFile) {
        viewModelScope.launch(Dispatchers.IO + SupervisorJob()) {
            val saved = askPdfDatabase.documentFileDao().findByPath(file.path) ?: file
            val nextCollected = saved.collected.not()
            askPdfDatabase.documentFileDao().upsert(
                saved.copy(
                    displayName = file.displayName,
                    mimeType = file.mimeType,
                    size = file.size,
                    dateAdded = file.dateAdded,
                    collected = nextCollected,
                    collectedAt = if (nextCollected) System.currentTimeMillis() else 0L
                )
            )
        }
    }

    /**
     * 从 MediaStore 查询支持的文档类型。
     */
    private fun queryDocuments(resolver: ContentResolver): List<DocumentFile> {
        val uri: Uri = MediaStore.Files.getContentUri("external")
        val mimeTypes = supportedMimeTypes.values.flatten().toSet()
        val placeholders = mimeTypes.joinToString(",") { "?" }
        val selection = "${MediaStore.Files.FileColumns.MIME_TYPE} IN ($placeholders)"
        val projection = arrayOf(
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_ADDED,
            MediaStore.Files.FileColumns.MIME_TYPE
        )
        val sortOrder = "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"
        val result = mutableListOf<DocumentFile>()

        resolver.query(uri, projection, selection, mimeTypes.toTypedArray(), sortOrder)?.use { cursor ->
            val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)
            val mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
            while (cursor.moveToNext()) {
                val path = cursor.getString(pathColumn).orEmpty()
                val size = cursor.getLong(sizeColumn)
                val file = File(path)
                if (size <= 0L || file.exists().not() || file.isDirectory || file.canRead().not()) continue
                result.add(
                    DocumentFile(
                        displayName = file.name,
                        path = path,
                        mimeType = cursor.getString(mimeColumn).orEmpty(),
                        size = size,
                        dateAdded = cursor.getLong(dateColumn) * 1000L
                    )
                )
            }
        }
        return result
    }
}
