package com.ctf.askpdf.feature.split

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.ctf.askpdf.R
import com.ctf.askpdf.databinding.ActivitySplitPdfBinding
import com.ctf.askpdf.document.PdfPasswordUtils
import com.ctf.askpdf.document.model.DocumentFile
import com.ctf.askpdf.document.model.DocumentKind
import com.ctf.askpdf.document.split.SplitPdfFileStore
import com.ctf.askpdf.feature.document.DocumentHubViewModel
import com.ctf.askpdf.presentation.base.BaseActivity
import com.ctf.askpdf.presentation.dialog.PdfPasswordDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SplitPdfActivity : BaseActivity<ActivitySplitPdfBinding>(ActivitySplitPdfBinding::inflate) {

    companion object {
        const val EXTRA_PRESELECTED_PATH = "extra_preselected_path"
    }

    private val viewModel by viewModels<DocumentHubViewModel>()
    private val preselectedPath by lazy { intent.getStringExtra(EXTRA_PRESELECTED_PATH) }
    private val splitAdapter by lazy {
        SplitPdfAdapter(this) { updateSelectionState() }
    }
    private var hasScrolledToPreselected = false

    /**
     * 将状态栏安全区应用到顶部栏，保持顶部颜色完整。
     */
    override fun onAttachedToWindow() = myEnableEdgeToEdge(binding.topBar)

    override fun initView(savedInstanceState: Bundle?) {
        onBackPressedDispatcher.addCallback(this) { finish() }
        binding.ivBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        binding.recyclerView.itemAnimator = null
        binding.recyclerView.adapter = splitAdapter
        binding.btnNext.setOnClickListener { prepareSelectedPdf() }
        updateSelectionState()
        viewModel.scanDocuments(this)
    }

    override fun observeData() {
        viewModel.scannedFilesLiveData.observe(this) { files ->
            val pdfFiles = files.filter { it.resolveType() == DocumentKind.PDF }
            binding.viewEmpty.isVisible = pdfFiles.isEmpty()
            splitAdapter.submitList(pdfFiles, preselectedPath)
            scrollToPreselectedPdf(pdfFiles)
        }
    }

    /**
     * 根据是否选中 PDF 更新底部按钮状态。
     */
    private fun updateSelectionState() {
        val canNext = splitAdapter.selectedItem() != null
        binding.btnNext.isEnabled = canNext
        binding.btnNext.alpha = if (canNext) 1f else 0.45f
    }

    /**
     * 从文件弹窗进入时，将列表滚动到预选 PDF 所在位置。
     */
    private fun scrollToPreselectedPdf(pdfFiles: List<DocumentFile>) {
        val targetPath = preselectedPath?.takeIf { it.isNotBlank() } ?: return
        if (hasScrolledToPreselected) return
        val targetIndex = pdfFiles.indexOfFirst { it.path == targetPath }
        if (targetIndex < 0) return
        hasScrolledToPreselected = true
        binding.recyclerView.post {
            val layoutManager = binding.recyclerView.layoutManager as? LinearLayoutManager
            layoutManager?.scrollToPositionWithOffset(targetIndex, 12)
                ?: binding.recyclerView.scrollToPosition(targetIndex)
        }
    }

    /**
     * 检查密码和页数，单页 PDF 直接提示不可拆分。
     */
    private fun prepareSelectedPdf() {
        val file = splitAdapter.selectedItem()
        if (file == null) {
            Toast.makeText(this, R.string.split_select_pdf_first, Toast.LENGTH_SHORT).show()
            return
        }
        lifecycleScope.launch {
            val plainPageCount = SplitPdfFileStore.pageCount(this@SplitPdfActivity, file)
            if (plainPageCount != null) {
                openPreviewIfSplittable(file, "", plainPageCount)
                return@launch
            }
            val needsPassword = withContext(Dispatchers.IO) {
                PdfPasswordUtils.needsPassword(file.path)
            }
            if (needsPassword) {
                PdfPasswordDialog.newInstance(file.path, file.displayName) { pass ->
                    if (pass.isNullOrBlank()) return@newInstance
                    checkPageCountThenOpen(file, pass)
                }.show(supportFragmentManager, "split_pdf_password")
            } else {
                checkPageCountThenOpen(file, "")
            }
        }
    }

    /**
     * 读取页数并进入页码选择页。
     */
    private fun checkPageCountThenOpen(file: DocumentFile, password: String) {
        lifecycleScope.launch {
            val pageCount = SplitPdfFileStore.pageCount(this@SplitPdfActivity, file, password)
            if (pageCount == null) {
                Toast.makeText(this@SplitPdfActivity, R.string.pdf_split_failed, Toast.LENGTH_SHORT).show()
                return@launch
            }
            openPreviewIfSplittable(file, password, pageCount)
        }
    }

    /**
     * 页数满足拆分条件时进入预览页，否则提示单页不可拆分。
     */
    private fun openPreviewIfSplittable(file: DocumentFile, password: String, pageCount: Int) {
        if (pageCount < 2) {
            Toast.makeText(this, R.string.one_page_tips, Toast.LENGTH_SHORT).show()
            return
        }
        startActivity(Intent(this, SplitPreviewActivity::class.java).apply {
            putExtra(SplitPreviewActivity.EXTRA_DOCUMENT_FILE, file)
            putExtra(SplitPreviewActivity.EXTRA_PASSWORD, password)
            putExtra(SplitPreviewActivity.EXTRA_PAGE_COUNT, pageCount)
        })
    }
}
