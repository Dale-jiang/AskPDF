package com.ctf.askpdf.feature.create

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.addCallback
import com.ctf.askpdf.R
import com.ctf.askpdf.databinding.ActivityCreatePdfResultBinding
import com.ctf.askpdf.document.model.DocumentFile
import com.ctf.askpdf.feature.read.PdfReadActivity
import com.ctf.askpdf.presentation.base.BaseActivity
import java.io.File

@Suppress("DEPRECATION")
class CreatePdfResultActivity : BaseActivity<ActivityCreatePdfResultBinding>(ActivityCreatePdfResultBinding::inflate) {

    companion object {
        const val EXTRA_DOCUMENT_FILE = "extra_document_file"
    }

    private val documentFile by lazy { intent?.getParcelableExtra<DocumentFile>(EXTRA_DOCUMENT_FILE) }

    /**
     * 将状态栏安全区应用到顶部栏，保持结果页顶部颜色完整。
     */
    override fun onAttachedToWindow() = myEnableEdgeToEdge(binding.topBar)

    override fun initView(savedInstanceState: Bundle?) {
        val file = documentFile
        if (file == null || File(file.path).exists().not()) {
            Toast.makeText(this, R.string.open_file_failed, Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        onBackPressedDispatcher.addCallback(this) { finish() }
        binding.ivBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        bindResult(file)
    }

    /**
     * 展示创建成功的文件信息，并绑定打开按钮。
     */
    private fun bindResult(file: DocumentFile) {
        binding.resultTitle.setText(R.string.create_success_title)
        binding.resultSubtitle.setText(R.string.create_success_message)
        binding.fileName.text = file.displayName
        binding.filePath.text = file.path
        binding.btnOpen.setOnClickListener {
            startActivity(Intent(this, PdfReadActivity::class.java).apply {
                putExtra(PdfReadActivity.EXTRA_DOCUMENT_FILE, file)
            })
            finish()
        }
    }
}
