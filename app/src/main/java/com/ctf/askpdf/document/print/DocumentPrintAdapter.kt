package com.ctf.askpdf.document.print

import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class DocumentPrintAdapter(
    private val file: File,
    private val jobName: String,
    private val onFinished: () -> Unit = {}
) : PrintDocumentAdapter() {

    /**
     * 提供打印任务的基础文档信息。
     */
    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes?,
        cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback?,
        extras: android.os.Bundle?
    ) {
        if (cancellationSignal?.isCanceled == true) {
            callback?.onLayoutCancelled()
            return
        }
        val info = PrintDocumentInfo.Builder(jobName)
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
            .build()
        callback?.onLayoutFinished(info, true)
    }

    /**
     * 将原文件内容写入系统打印目标。
     */
    override fun onWrite(
        pages: Array<out PageRange>?,
        destination: ParcelFileDescriptor?,
        cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback?
    ) {
        Thread {
            if (cancellationSignal?.isCanceled == true) {
                callback?.onWriteCancelled()
                return@Thread
            }
            runCatching {
                checkNotNull(destination)
                FileInputStream(file).use { input ->
                    FileOutputStream(destination.fileDescriptor).use { output ->
                        input.copyTo(output)
                    }
                }
            }.onSuccess {
                callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
            }.onFailure {
                callback?.onWriteFailed(it.localizedMessage)
            }
        }.start()
    }

    /**
     * 打印流程结束时通知调用方恢复生命周期标记。
     */
    override fun onFinish() {
        onFinished()
    }
}
