package com.ctf.askpdf.feature.read

import android.os.Bundle
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.core.content.FileProvider
import com.ctf.askpdf.R
import com.ctf.askpdf.databinding.ActivityDocReadBinding
import com.ctf.askpdf.document.model.DocumentFile
import com.ctf.askpdf.document.model.DocumentKind
import com.ctf.askpdf.feature.document.DocumentHubViewModel
import com.ctf.askpdf.presentation.base.BaseActivity
import com.seapeak.docviewer.DocViewerFragment
import com.seapeak.docviewer.config.DocConfig
import com.seapeak.docviewer.config.DocType
import java.io.File

@Suppress("DEPRECATION")
class DocReadActivity : BaseActivity<ActivityDocReadBinding>(ActivityDocReadBinding::inflate) {

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
        openDocument(file)
    }

    /**
     * 创建 DocViewerFragment 并展示 Office 文档。
     */
    private fun openDocument(file: DocumentFile) {
        val uri = FileProvider.getUriForFile(
            this,
            "$packageName.fileProvider",
            File(file.path)
        )
        val docType = when (file.resolveType()) {
            DocumentKind.WORD -> DocType.WORD
            DocumentKind.EXCEL -> DocType.EXCEL
            DocumentKind.PPT -> DocType.PPT
            else -> DocType.PDF
        }
        val fragment = DocViewerFragment.newInstance(DocConfig(url = uri.toString(), type = docType))
        supportFragmentManager.beginTransaction()
            .replace(R.id.doc_reader_container, fragment)
            .commit()
        viewModel.markAsRecent(file)
    }
}
