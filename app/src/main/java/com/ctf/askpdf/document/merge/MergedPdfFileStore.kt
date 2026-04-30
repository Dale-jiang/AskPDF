package com.ctf.askpdf.document.merge

import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import com.ctf.askpdf.document.model.DocumentFile
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.io.MemoryUsageSetting
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
            val memoryUsage = createDiskBackedMemoryUsage(context)
            if (passwords.isEmpty()) {
                mergePlainFiles(files, outputFile, memoryUsage)
            } else {
                mergePasswordProtectedFiles(files, outputFile, passwords, memoryUsage)
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
     * 使用 PDFBox 优化模式合并普通 PDF，源文件可提前关闭，减少大文件内存占用。
     */
    private fun mergePlainFiles(
        files: List<DocumentFile>,
        outputFile: File,
        memoryUsage: MemoryUsageSetting
    ) {
        PDFMergerUtility().apply {
            setDocumentMergeMode(PDFMergerUtility.DocumentMergeMode.OPTIMIZE_RESOURCES_MODE)
            destinationFileName = outputFile.absolutePath
            files.forEach { addSource(File(it.path)) }
            mergeDocuments(memoryUsage)
        }
    }

    /**
     * 合并带密码的 PDF，加载和目标文档都使用临时文件缓存以承载大文件。
     */
    private fun mergePasswordProtectedFiles(
        files: List<DocumentFile>,
        outputFile: File,
        passwords: Map<String, String>,
        memoryUsage: MemoryUsageSetting
    ) {
        PDDocument(memoryUsage).use { mergedDocument ->
            val merger = PDFMergerUtility()
            files.forEach { file ->
                val password = passwords[file.path].orEmpty()
                PDDocument.load(File(file.path), password, memoryUsage).use { sourceDocument ->
                    sourceDocument.isAllSecurityToBeRemoved = true
                    merger.appendDocument(mergedDocument, sourceDocument)
                }
            }
            mergedDocument.save(outputFile)
        }
    }

    /**
     * 创建 PDFBox 临时文件缓存目录，避免大 PDF 合并时全部占用 JVM 堆内存。
     */
    private fun createDiskBackedMemoryUsage(context: Context): MemoryUsageSetting {
        val tempDir = File(context.cacheDir, "pdfbox_merge_cache").apply { mkdirs() }
        return MemoryUsageSetting.setupTempFileOnly().setTempDir(tempDir)
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
