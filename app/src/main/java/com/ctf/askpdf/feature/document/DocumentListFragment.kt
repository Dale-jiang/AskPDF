package com.ctf.askpdf.feature.document

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import com.ctf.askpdf.R
import com.ctf.askpdf.databinding.FragmentDocumentListBinding
import com.ctf.askpdf.document.model.DocumentFile
import com.ctf.askpdf.document.model.DocumentKind
import com.ctf.askpdf.document.model.DocumentTab
import com.ctf.askpdf.presentation.adapter.DocumentFileAdapter
import com.ctf.askpdf.presentation.base.BaseFragment
import java.io.File

class DocumentListFragment : BaseFragment<FragmentDocumentListBinding>(FragmentDocumentListBinding::inflate) {

    companion object {
        private const val ARG_DOCUMENT_TAB = "arg_document_tab"
        private const val ARG_DOCUMENT_KIND = "arg_document_kind"

        fun newInstance(tab: DocumentTab, kind: DocumentKind): DocumentListFragment {
            return DocumentListFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_DOCUMENT_TAB, tab)
                    putSerializable(ARG_DOCUMENT_KIND, kind)
                }
            }
        }
    }

    private val viewModel by activityViewModels<DocumentHubViewModel>()
    private val documentTab by lazy {
        arguments?.getSerializable(ARG_DOCUMENT_TAB) as? DocumentTab ?: DocumentTab.HOME
    }
    private val documentKind by lazy {
        arguments?.getSerializable(ARG_DOCUMENT_KIND) as? DocumentKind ?: DocumentKind.ALL
    }
    private lateinit var fileAdapter: DocumentFileAdapter

    override fun initView(savedInstanceState: Bundle?) {
        fileAdapter = DocumentFileAdapter(
            context = requireContext(),
            tab = documentTab,
            itemClick = { file ->
                viewModel.markAsRecent(file)
                openDocument(file)
            },
            moreClick = { file ->
                viewModel.toggleCollection(file)
            }
        )
        binding.recyclerView.itemAnimator = null
        binding.recyclerView.adapter = fileAdapter
    }

    override fun observeData() {
        when (documentTab) {
            DocumentTab.HOME -> viewModel.scannedFilesLiveData.observe(viewLifecycleOwner) { submitFiles(it) }
            DocumentTab.RECENT -> viewModel.recentFilesLiveData.observe(viewLifecycleOwner) { submitFiles(it) }
            DocumentTab.COLLECTION -> viewModel.collectionFilesLiveData.observe(viewLifecycleOwner) { submitFiles(it) }
        }
    }

    /**
     * 根据当前二级类型过滤并刷新列表。
     */
    private fun submitFiles(files: List<DocumentFile>) {
        val filtered = if (documentKind == DocumentKind.ALL) files else files.filter { it.resolveType() == documentKind }
        binding.viewEmpty.isVisible = filtered.isEmpty()
        binding.viewEmpty.text = when (documentTab) {
            DocumentTab.HOME -> getString(R.string.no_files_tips)
            DocumentTab.RECENT -> getString(R.string.no_history_yet)
            DocumentTab.COLLECTION -> getString(R.string.no_bookmarks_yet)
        }
        fileAdapter.submitList(filtered) {
            if (filtered.isNotEmpty()) binding.recyclerView.scrollToPosition(0)
        }
    }

    /**
     * 使用系统应用打开文档，失败时只提示不崩溃。
     */
    private fun openDocument(file: DocumentFile) {
        runCatching {
            val uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileProvider",
                File(file.path)
            )
            startActivity(Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, file.mimeType.ifBlank { "*/*" })
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            })
        }.onFailure {
            Toast.makeText(requireContext(), R.string.open_file_failed, Toast.LENGTH_SHORT).show()
        }
    }
}
