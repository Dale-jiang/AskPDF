package com.ctf.askpdf.feature.read

import android.os.Bundle
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.ctf.askpdf.R
import com.ctf.askpdf.databinding.ActivityPdfReadBinding
import com.ctf.askpdf.document.PdfPasswordUtils
import com.ctf.askpdf.document.model.DocumentFile
import com.ctf.askpdf.feature.document.DocumentHubViewModel
import com.ctf.askpdf.presentation.base.BaseActivity
import com.ctf.askpdf.presentation.dialog.PdfPasswordDialog
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle
import com.github.barteksc.pdfviewer.util.FitPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Suppress("DEPRECATION")
class PdfReadActivity : BaseActivity<ActivityPdfReadBinding>(ActivityPdfReadBinding::inflate) {

    companion object {
        const val EXTRA_DOCUMENT_FILE = "extra_document_file"
    }

    private val documentFile by lazy { intent?.getParcelableExtra<DocumentFile>(EXTRA_DOCUMENT_FILE) }
    private val viewModel by viewModels<DocumentHubViewModel>()

    /**
     * 将状态栏安全区应用到顶部栏，保持与多语言页一致。
     */
    override fun onAttachedToWindow() = myEnableEdgeToEdge(binding.topBar)

    override fun initView(savedInstanceState: Bundle?) {
        val file = documentFile
        if (file == null || File(file.path).exists().not()) {
            Toast.makeText(this, R.string.open_file_failed, Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        binding.title.text = file.displayName
        binding.ivBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        onBackPressedDispatcher.addCallback(this) { finish() }
        preparePdf(file)
    }

    /**
     * 检查 PDF 密码状态，需要密码时先弹窗输入。
     */
    private fun preparePdf(file: DocumentFile) {
        lifecycleScope.launch {
            val needPassword = withContext(Dispatchers.IO) {
                PdfPasswordUtils.needsPassword(file.path)
            }
            if (needPassword) {
                PdfPasswordDialog.newInstance(file.path) { pass ->
                    if (pass.isNullOrBlank()) {
                        finish()
                    } else {
                        loadPdf(file, pass)
                    }
                }.show(supportFragmentManager, "pdf_password")
            } else {
                loadPdf(file, null)
            }
        }
    }

    /**
     * 使用 AndroidPdfViewer 加载 PDF 文件，可携带已验证密码。
     */
    private fun loadPdf(file: DocumentFile, pass: String?) {
        val configurator = binding.pdfView.fromFile(File(file.path))
            .enableSwipe(true)
            .swipeHorizontal(false)
            .enableDoubletap(true)
            .enableAnnotationRendering(true)
            .scrollHandle(DefaultScrollHandle(this))
            .enableAntialiasing(true)
            .spacing(3)
            .autoSpacing(false)
            .pageFitPolicy(FitPolicy.WIDTH)
            .fitEachPage(false)
            .pageSnap(false)
            .pageFling(false)
            .nightMode(false)
            .onError {
                Toast.makeText(this, R.string.open_file_failed, Toast.LENGTH_SHORT).show()
            }
        if (pass.isNullOrBlank().not()) configurator.password(pass)
        configurator.load()
        viewModel.markAsRecent(file)
    }

    override fun onDestroy() {
        binding.pdfView.recycle()
        super.onDestroy()
    }
}
