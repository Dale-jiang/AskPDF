package com.ctf.askpdf.feature.document

import android.content.ContentResolver
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ctf.askpdf.R
import com.ctf.askpdf.data.askPdfDatabase
import com.ctf.askpdf.document.model.DocumentFile
import com.ctf.askpdf.document.model.supportedMimeTypes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
     * 重命名文档文件，并同步数据库和首页列表。
     */
    fun renameDocument(context: Context, file: DocumentFile, rawName: String, onResult: (Boolean, Int?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO + SupervisorJob()) {
            val sourceFile = File(file.path)
            val targetName = resolveRenameTargetName(sourceFile.name, rawName.trim())
            val targetFile = File(sourceFile.parentFile, targetName)
            val errorRes = when {
                rawName.isBlank() -> R.string.file_name_empty
                sourceFile.exists().not() -> R.string.rename_file_failed
                targetFile.exists() && targetFile.absolutePath != sourceFile.absolutePath -> R.string.file_name_exists
                targetFile.absolutePath == sourceFile.absolutePath -> null
                sourceFile.renameTo(targetFile).not() -> R.string.rename_file_failed
                else -> null
            }
            if (errorRes == null) {
                val renamedFile = if (targetFile.exists()) targetFile else sourceFile
                val saved = askPdfDatabase.documentFileDao().findByPath(file.path)
                val updated = (saved ?: file).copy(
                    displayName = renamedFile.name,
                    path = renamedFile.absolutePath,
                    mimeType = file.mimeType,
                    size = renamedFile.length().takeIf { it > 0L } ?: file.size,
                    dateAdded = file.dateAdded
                )
                if (saved != null || file.recentViewTime > 0L || file.collected) {
                    askPdfDatabase.documentFileDao().upsert(updated)
                }
                replaceScannedFile(file.path, updated)
                scanChangedFiles(context, file.path, renamedFile.absolutePath)
            }
            withContext(Dispatchers.Main) {
                onResult(errorRes == null, errorRes)
            }
        }
    }

    /**
     * 删除文档文件，并从数据库和当前列表中移除。
     */
    fun deleteDocument(context: Context, file: DocumentFile, onResult: (Boolean, Int?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO + SupervisorJob()) {
            val targetFile = File(file.path)
            val deleted = targetFile.exists().not() || targetFile.delete()
            if (deleted) {
                askPdfDatabase.documentFileDao().deleteByPath(file.path)
                removeScannedFile(file.path)
                scanChangedFiles(context, file.path)
            }
            withContext(Dispatchers.Main) {
                onResult(deleted, if (deleted) null else R.string.delete_file_failed)
            }
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

    /**
     * 根据输入名称补齐原文件扩展名。
     */
    private fun resolveRenameTargetName(originalName: String, inputName: String): String {
        val extension = originalName.substringAfterLast('.', missingDelimiterValue = "")
        if (extension.isBlank() || inputName.endsWith(".$extension", ignoreCase = true)) return inputName
        return "$inputName.$extension"
    }

    /**
     * 更新首页扫描列表中的单个文件。
     */
    private fun replaceScannedFile(oldPath: String, updatedFile: DocumentFile) {
        val current = scannedFilesLiveData.value.orEmpty()
        if (current.none { it.path == oldPath }) return
        scannedFilesLiveData.postValue(current.map { if (it.path == oldPath) updatedFile else it })
    }

    /**
     * 从首页扫描列表移除已删除文件。
     */
    private fun removeScannedFile(path: String) {
        val current = scannedFilesLiveData.value.orEmpty()
        if (current.none { it.path == path }) return
        scannedFilesLiveData.postValue(current.filterNot { it.path == path })
    }

    /**
     * 通知系统媒体库刷新发生变化的文件路径。
     */
    private fun scanChangedFiles(context: Context, vararg paths: String) {
        MediaScannerConnection.scanFile(context.applicationContext, paths, null, null)
    }
}
