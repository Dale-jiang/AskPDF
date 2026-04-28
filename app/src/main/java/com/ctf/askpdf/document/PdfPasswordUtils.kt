package com.ctf.askpdf.document

import com.artifex.mupdf.fitz.Document

object PdfPasswordUtils {

    /**
     * 判断 PDF 是否需要密码，异常文件按不需要密码处理，交给阅读器展示错误。
     */
    fun needsPassword(path: String): Boolean {
        return runCatching {
            val pdfCore = Document.openDocument(path)
            try {
                pdfCore.needsPassword()
            } finally {
                pdfCore.destroy()
            }
        }.getOrDefault(false)
    }

    /**
     * 使用用户输入密码校验 PDF，返回 true 表示密码可打开文档。
     */
    fun authenticatePassword(path: String, pass: String): Boolean {
        return runCatching {
            val pdfCore = Document.openDocument(path)
            try {
                pdfCore.needsPassword().not() || pdfCore.authenticatePassword(pass)
            } finally {
                pdfCore.destroy()
            }
        }.getOrDefault(false)
    }
}
