package com.ctf.askpdf.feature.split

import android.content.Intent
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.ctf.askpdf.R
import com.ctf.askpdf.databinding.ActivitySplitPreviewBinding
import com.ctf.askpdf.document.model.DocumentFile
import com.ctf.askpdf.document.split.SplitPdfFileStore
import com.ctf.askpdf.feature.create.CreatePdfResultActivity
import com.ctf.askpdf.feature.document.DocumentHubViewModel
import com.ctf.askpdf.presentation.base.BaseActivity
import com.ctf.askpdf.presentation.dialog.CreatingPdfDialog
import com.ctf.askpdf.presentation.dialog.RenameFileDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Suppress("DEPRECATION")
class SplitPreviewActivity : BaseActivity<ActivitySplitPreviewBinding>(ActivitySplitPreviewBinding::inflate) {

    companion object {
        const val EXTRA_DOCUMENT_FILE = "extra_document_file"
        const val EXTRA_PASSWORD = "extra_password"
        const val EXTRA_PAGE_COUNT = "extra_page_count"
        private const val MIN_SPLIT_LOADING_DURATION_MS = 1000L
    }

    private val viewModel by viewModels<DocumentHubViewModel>()
    private val documentFile by lazy { intent.getParcelableExtra<DocumentFile>(EXTRA_DOCUMENT_FILE) }
    private val password by lazy { intent.getStringExtra(EXTRA_PASSWORD).orEmpty() }
    private val pageCount by lazy { intent.getIntExtra(EXTRA_PAGE_COUNT, 0) }
    private var previewDescriptor: ParcelFileDescriptor? = null
    private var pdfRenderer: PdfRenderer? = null
    private var pageAdapter: SplitPageAdapter? = null

    /**
     * 将状态栏安全区应用到顶部栏，保持顶部颜色完整。
     */
    override fun onAttachedToWindow() = myEnableEdgeToEdge(binding.topBar)

    override fun initView(savedInstanceState: Bundle?) {
        val file = documentFile
        if (file == null || pageCount < 2) {
            Toast.makeText(this, R.string.open_file_failed, Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        onBackPressedDispatcher.addCallback(this) { finish() }
        binding.ivBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        binding.fileName.text = file.displayName
        binding.recyclerView.itemAnimator = null
        binding.btnSplit.setOnClickListener { showOutputNameDialog(file) }
        updateSelectionState()
        loadPagePreview(file)
    }

    /**
     * 异步初始化 PDFBox 渲染器，避免大文件加载阻塞主线程。
     */
    private fun loadPagePreview(file: DocumentFile) {
        binding.recyclerView.isVisible = false
        lifecycleScope.launch {
            val rendererReady = withContext(Dispatchers.IO) {
                setupPagePreview(file)
            }
            if (rendererReady.not()) {
                Toast.makeText(this@SplitPreviewActivity, R.string.open_file_failed, Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }
            pageAdapter = SplitPageAdapter(pageCount, pdfRenderer, lifecycleScope) { updateSelectionState() }
            binding.recyclerView.adapter = pageAdapter
            binding.recyclerView.isVisible = true
            updateSelectionState()
        }
    }

    /**
     * 打开 PDF 并创建原生渲染器，调用方需在 IO 线程执行。
     */
    private fun setupPagePreview(file: DocumentFile): Boolean {
        if (password.isNotBlank()) return true
        return runCatching {
            val descriptor = ParcelFileDescriptor.open(File(file.path), ParcelFileDescriptor.MODE_READ_ONLY)
            previewDescriptor = descriptor
            pdfRenderer = PdfRenderer(descriptor)
        }.isSuccess
    }

    /**
     * 根据选中页数更新底部按钮和提示。
     */
    private fun updateSelectionState() {
        val selectedCount = pageAdapter?.selectedItems()?.size ?: 0
        val canSplit = selectedCount > 0
        binding.selectionTips.text = getString(R.string.split_selected_count, selectedCount, pageCount)
        binding.btnSplit.isEnabled = canSplit
        binding.btnSplit.alpha = if (canSplit) 1f else 0.45f
    }

    /**
     * 弹出输出文件命名框。
     */
    private fun showOutputNameDialog(file: DocumentFile) {
        val selectedPages = pageAdapter?.selectedItems().orEmpty()
        if (selectedPages.isEmpty()) {
            Toast.makeText(this, R.string.split_no_page_selected, Toast.LENGTH_SHORT).show()
            return
        }
        RenameFileDialog.newInstance(
            initialName = SplitPdfFileStore.defaultFileName(),
            titleRes = R.string.split_pdf
        ) { rawName, onHandled ->
            if (rawName.isBlank()) {
                Toast.makeText(this, R.string.file_name_empty, Toast.LENGTH_SHORT).show()
                onHandled(false)
                return@newInstance
            }
            onHandled(true)
            splitSelectedPages(file, rawName, selectedPages)
        }.show(supportFragmentManager, "split_pdf_name")
    }

    /**
     * 显示加载态并执行 PDF 拆分，成功后跳转结果页。
     */
    private fun splitSelectedPages(file: DocumentFile, outputName: String, selectedPages: List<Int>) {
        val loadingDialog = CreatingPdfDialog.newInstance(R.string.splitting_pdf)
        val startTime = System.currentTimeMillis()
        loadingDialog.show(supportFragmentManager, "splitting_pdf")
        lifecycleScope.launch {
            val splitFile = SplitPdfFileStore.split(this@SplitPreviewActivity, file, outputName, password, selectedPages)
            val remainingDelay = MIN_SPLIT_LOADING_DURATION_MS - (System.currentTimeMillis() - startTime)
            if (remainingDelay > 0L) delay(remainingDelay)
            loadingDialog.dismissAllowingStateLoss()
            if (splitFile == null) {
                Toast.makeText(this@SplitPreviewActivity, R.string.pdf_split_failed, Toast.LENGTH_SHORT).show()
                return@launch
            }
            viewModel.addCreatedPdf(this@SplitPreviewActivity, splitFile)
            openSplitResultPage(
                DocumentFile(
                    displayName = splitFile.name,
                    path = splitFile.absolutePath,
                    mimeType = "application/pdf",
                    size = splitFile.length(),
                    dateAdded = splitFile.lastModified()
                )
            )
        }
    }

    /**
     * 打开拆分成功结果页。
     */
    private fun openSplitResultPage(file: DocumentFile) {
        startActivity(Intent(this, CreatePdfResultActivity::class.java).apply {
            putExtra(CreatePdfResultActivity.EXTRA_DOCUMENT_FILE, file)
            putExtra(CreatePdfResultActivity.EXTRA_TITLE_RES, R.string.pdf_split_success_title)
            putExtra(CreatePdfResultActivity.EXTRA_MESSAGE_RES, R.string.pdf_split_success_message)
        })
        finish()
    }

    override fun onDestroy() {
        pdfRenderer?.close()
        pdfRenderer = null
        previewDescriptor?.close()
        previewDescriptor = null
        super.onDestroy()
    }
}
