package com.ctf.askpdf.document.create

import android.content.Context
import android.net.Uri
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object CreatedPdfFileStore {

    private const val DEFAULT_NAME = "AskPDF"
    private const val PDF_EXTENSION = ".pdf"

    /**
     * 将扫描器返回的临时 PDF 保存到公共 Documents/AskPDF 目录。
     */
    suspend fun save(context: Context, sourceUri: Uri, rawName: String): File? = withContext(Dispatchers.IO) {
        runCatching {
            val targetDir = resolveTargetDirectory()
            if (targetDir.exists().not() && targetDir.mkdirs().not()) return@withContext null
            val targetFile = resolveAvailableFile(targetDir, rawName)
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext null
            targetFile
        }.getOrNull()
    }

    /**
     * 删除扫描器生成的临时 PDF，取消创建时避免残留。
     */
    fun deleteTemporaryPdf(context: Context, sourceUri: Uri) {
        runCatching { context.contentResolver.delete(sourceUri, null, null) }
    }

    /**
     * 生成默认 PDF 名称，保证用户打开命名弹窗时已有可用文件名。
     */
    fun defaultFileName(): String {
        return "${DEFAULT_NAME}_${System.currentTimeMillis()}"
    }

    /**
     * 获取应用创建 PDF 的保存目录。
     */
    @Suppress("DEPRECATION")
    private fun resolveTargetDirectory(): File {
        return File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "AskPDF")
    }

    /**
     * 根据用户输入生成不冲突的 PDF 文件。
     */
    private fun resolveAvailableFile(targetDir: File, rawName: String): File {
        val cleanName = sanitizeName(rawName)
        var targetFile = File(targetDir, "$cleanName$PDF_EXTENSION")
        var index = 1
        while (targetFile.exists()) {
            targetFile = File(targetDir, "$cleanName ($index)$PDF_EXTENSION")
            index++
        }
        return targetFile
    }

    /**
     * 清理文件系统不支持的字符，并移除用户可能手动输入的 .pdf 后缀。
     */
    private fun sanitizeName(rawName: String): String {
        val nameWithoutExtension = rawName.trim().removeSuffix(PDF_EXTENSION)
        val sanitized = nameWithoutExtension.replace(Regex("[\\\\/:*?\"<>|\\x00]"), "_").trim()
        return sanitized.ifBlank { defaultFileName() }
    }
}
