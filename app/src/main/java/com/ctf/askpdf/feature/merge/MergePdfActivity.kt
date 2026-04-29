package com.ctf.askpdf.feature.merge

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.ctf.askpdf.R
import com.ctf.askpdf.databinding.ActivityMergePdfBinding
import com.ctf.askpdf.document.PdfPasswordUtils
import com.ctf.askpdf.document.merge.MergedPdfFileStore
import com.ctf.askpdf.document.model.DocumentFile
import com.ctf.askpdf.document.model.DocumentKind
import com.ctf.askpdf.feature.create.CreatePdfResultActivity
import com.ctf.askpdf.feature.document.DocumentHubViewModel
import com.ctf.askpdf.presentation.base.BaseActivity
import com.ctf.askpdf.presentation.dialog.CreatingPdfDialog
import com.ctf.askpdf.presentation.dialog.PdfPasswordDialog
import com.ctf.askpdf.presentation.dialog.RenameFileDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

class MergePdfActivity : BaseActivity<ActivityMergePdfBinding>(ActivityMergePdfBinding::inflate) {

    companion object {
        const val EXTRA_PRESELECTED_PATH = "extra_preselected_path"
        private const val MIN_MERGE_LOADING_DURATION_MS = 1000L
    }

    private val viewModel by viewModels<DocumentHubViewModel>()
    private val preselectedPath by lazy { intent.getStringExtra(EXTRA_PRESELECTED_PATH) }
    private val mergeAdapter by lazy {
        MergePdfAdapter(this) { updateSelectionState() }
    }
    private val passwords = mutableMapOf<String, String>()
    private var hasScrolledToPreselected = false

    /**
     * 将状态栏安全区应用到顶部栏，保持顶部颜色完整。
     */
    override fun onAttachedToWindow() = myEnableEdgeToEdge(binding.topBar)

    override fun initView(savedInstanceState: Bundle?) {
        onBackPressedDispatcher.addCallback(this) { finish() }
        binding.ivBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        binding.recyclerView.itemAnimator = null
        binding.recyclerView.adapter = mergeAdapter
        binding.btnMerge.setOnClickListener { showOutputNameDialog() }
        updateSelectionState()
        viewModel.scanDocuments(this)
    }

    override fun observeData() {
        viewModel.scannedFilesLiveData.observe(this) { files ->
            val pdfFiles = files.filter { it.resolveType() == DocumentKind.PDF }
            binding.viewEmpty.isVisible = pdfFiles.isEmpty()
            mergeAdapter.submitList(pdfFiles, preselectedPath)
            scrollToPreselectedPdf(pdfFiles)
        }
    }

    /**
     * 根据选中文件数量更新底部操作区。
     */
    private fun updateSelectionState() {
        val selectedCount = mergeAdapter.selectedItems().size
        val canMerge = selectedCount >= 2
        binding.selectionTips.text = getString(R.string.merge_selection_count, selectedCount, MergePdfAdapter.MAX_SELECTED_COUNT)
        binding.btnMerge.isEnabled = canMerge
        binding.btnMerge.alpha = if (canMerge) 1f else 0.45f
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
     * 弹出输出文件命名框。
     */
    private fun showOutputNameDialog() {
        val selectedFiles = mergeAdapter.selectedItems()
        if (selectedFiles.size < 2) {
            Toast.makeText(this, R.string.merge_requires_two, Toast.LENGTH_SHORT).show()
            return
        }
        RenameFileDialog.newInstance(
            initialName = MergedPdfFileStore.defaultFileName(),
            titleRes = R.string.merge_pdf
        ) { rawName, onHandled ->
            if (rawName.isBlank()) {
                Toast.makeText(this, R.string.file_name_empty, Toast.LENGTH_SHORT).show()
                onHandled(false)
                return@newInstance
            }
            onHandled(true)
            preparePasswordsThenMerge(selectedFiles, rawName)
        }.show(supportFragmentManager, "merge_pdf_name")
    }

    /**
     * 逐个检查加密 PDF，必要时请求用户输入密码。
     */
    private fun preparePasswordsThenMerge(selectedFiles: List<DocumentFile>, outputName: String) {
        passwords.clear()
        requestNextPassword(selectedFiles, outputName, 0)
    }

    /**
     * 递归请求缺失密码，全部准备完成后开始合并。
     */
    private fun requestNextPassword(selectedFiles: List<DocumentFile>, outputName: String, index: Int) {
        if (index >= selectedFiles.size) {
            mergeSelectedPdfs(selectedFiles, outputName)
            return
        }
        val file = selectedFiles[index]
        lifecycleScope.launch {
            val needsPassword = withContext(Dispatchers.IO) {
                PdfPasswordUtils.needsPassword(file.path)
            }
            if (needsPassword.not()) {
                requestNextPassword(selectedFiles, outputName, index + 1)
                return@launch
            }
            PdfPasswordDialog.newInstance(file.path, file.displayName) { pass ->
                if (pass.isNullOrBlank()) {
                    Toast.makeText(this@MergePdfActivity, R.string.merge_cancelled, Toast.LENGTH_SHORT).show()
                } else {
                    passwords[file.path] = pass
                    requestNextPassword(selectedFiles, outputName, index + 1)
                }
            }.show(supportFragmentManager, "merge_pdf_password_$index")
        }
    }

    /**
     * 显示加载态并执行 PDF 合并，成功后跳转结果页。
     */
    private fun mergeSelectedPdfs(selectedFiles: List<DocumentFile>, outputName: String) {
        val loadingDialog = CreatingPdfDialog.newInstance(R.string.merging_pdf)
        val startTime = System.currentTimeMillis()
        loadingDialog.show(supportFragmentManager, "merging_pdf")
        lifecycleScope.launch {
            val mergedFile = MergedPdfFileStore.merge(this@MergePdfActivity, selectedFiles, outputName, passwords)
            val remainingDelay = MIN_MERGE_LOADING_DURATION_MS - (System.currentTimeMillis() - startTime)
            if (remainingDelay > 0L) delay(remainingDelay)
            loadingDialog.dismissAllowingStateLoss()
            if (mergedFile == null) {
                Toast.makeText(this@MergePdfActivity, R.string.pdf_merge_failed, Toast.LENGTH_SHORT).show()
                return@launch
            }
            viewModel.addCreatedPdf(this@MergePdfActivity, mergedFile)
            openMergeResultPage(
                DocumentFile(
                    displayName = mergedFile.name,
                    path = mergedFile.absolutePath,
                    mimeType = "application/pdf",
                    size = mergedFile.length(),
                    dateAdded = mergedFile.lastModified()
                )
            )
        }
    }

    /**
     * 打开合并成功结果页。
     */
    private fun openMergeResultPage(file: DocumentFile) {
        startActivity(Intent(this, CreatePdfResultActivity::class.java).apply {
            putExtra(CreatePdfResultActivity.EXTRA_DOCUMENT_FILE, file)
            putExtra(CreatePdfResultActivity.EXTRA_TITLE_RES, R.string.pdf_merge_success_title)
            putExtra(CreatePdfResultActivity.EXTRA_MESSAGE_RES, R.string.pdf_merge_success_message)
        })
        finish()
    }
}
