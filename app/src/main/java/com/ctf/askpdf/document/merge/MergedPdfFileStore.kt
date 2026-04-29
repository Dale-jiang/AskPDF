package com.ctf.askpdf.document.merge

import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import com.ctf.askpdf.document.model.DocumentFile
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility
import com.tom_roush.pdfbox.pdmodel.PDDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object MergedPdfFileStore {

    private const val DEFAULT_NAME = "AskPDF_Merge"
    private const val PDF_EXTENSION = ".pdf"

    /**
     * 合并多个 PDF，并将结果保存到公共 Documents/AskPDF 目录。
     */
    suspend fun merge(
        context: Context,
        files: List<DocumentFile>,
        rawName: String,
        passwords: Map<String, String>
    ): File? = withContext(Dispatchers.IO) {
        runCatching {
            PDFBoxResourceLoader.init(context.applicationContext)
            val targetDir = resolveTargetDirectory()
            if (targetDir.exists().not() && targetDir.mkdirs().not()) return@withContext null
            val outputFile = resolveAvailableFile(targetDir, rawName)
            val mergedDocument = PDDocument()
            try {
                val merger = PDFMergerUtility()
                files.forEach { file ->
                    val password = passwords[file.path].orEmpty()
                    PDDocument.load(File(file.path), password).use { sourceDocument ->
                        sourceDocument.isAllSecurityToBeRemoved = true
                        merger.appendDocument(mergedDocument, sourceDocument)
                    }
                }
                mergedDocument.save(outputFile)
            } finally {
                mergedDocument.close()
            }
            MediaScannerConnection.scanFile(
                context.applicationContext,
                arrayOf(outputFile.absolutePath),
                arrayOf("application/pdf"),
                null
            )
            outputFile
        }.getOrNull()
    }

    /**
     * 生成默认合并文件名。
     */
    fun defaultFileName(): String {
        return "${DEFAULT_NAME}_${System.currentTimeMillis()}"
    }

    /**
     * 获取合并 PDF 的保存目录。
     */
    @Suppress("DEPRECATION")
    private fun resolveTargetDirectory(): File {
        return File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "AskPDF")
    }

    /**
     * 按输入名称生成不冲突的输出文件。
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
     * 清理文件名非法字符，并移除用户手动输入的 .pdf 后缀。
     */
    private fun sanitizeName(rawName: String): String {
        val nameWithoutExtension = rawName.trim().removeSuffix(PDF_EXTENSION)
        val sanitized = nameWithoutExtension.replace(Regex("[\\\\/:*?\"<>|\\x00]"), "_").trim()
        return sanitized.ifBlank { defaultFileName() }
    }
}
