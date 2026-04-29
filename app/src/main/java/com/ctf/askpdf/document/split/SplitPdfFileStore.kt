package com.ctf.askpdf.document.split

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.media.MediaScannerConnection
import android.os.Environment
import android.os.ParcelFileDescriptor
import com.ctf.askpdf.document.model.DocumentFile
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object SplitPdfFileStore {

    private const val DEFAULT_NAME = "AskPDF_Split"
    private const val PDF_EXTENSION = ".pdf"

    /**
     * 读取 PDF 页数，password 为空时按普通 PDF 读取。
     */
    suspend fun pageCount(context: Context, file: DocumentFile, password: String = ""): Int? = withContext(Dispatchers.IO) {
        runCatching {
            if (password.isBlank()) {
                ParcelFileDescriptor.open(File(file.path), ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
                    PdfRenderer(descriptor).use { renderer -> renderer.pageCount }
                }
            } else {
                PDFBoxResourceLoader.init(context.applicationContext)
                PDDocument.load(File(file.path), password).use { it.numberOfPages }
            }
        }.getOrNull()
    }

    /**
     * 按页码拆分 PDF，并将选中页输出为一个新 PDF。
     */
    suspend fun split(
        context: Context,
        file: DocumentFile,
        rawName: String,
        password: String,
        pages: List<Int>
    ): File? = withContext(Dispatchers.IO) {
        runCatching {
            PDFBoxResourceLoader.init(context.applicationContext)
            val targetDir = resolveTargetDirectory()
            if (targetDir.exists().not() && targetDir.mkdirs().not()) return@withContext null
            val outputFile = resolveAvailableFile(targetDir, rawName)
            PDDocument.load(File(file.path), password).use { sourceDocument ->
                sourceDocument.isAllSecurityToBeRemoved = true
                val targetDocument = PDDocument()
                try {
                    val validPages = pages.distinct().sorted().filter { it in 0 until sourceDocument.numberOfPages }
                    if (validPages.isEmpty()) return@withContext null
                    validPages.forEach { pageIndex ->
                        targetDocument.importPage(sourceDocument.getPage(pageIndex))
                    }
                    targetDocument.save(outputFile)
                } finally {
                    targetDocument.close()
                }
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
     * 生成默认拆分文件名。
     */
    fun defaultFileName(): String {
        return "${DEFAULT_NAME}_${System.currentTimeMillis()}"
    }

    /**
     * 获取拆分 PDF 的保存目录。
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
