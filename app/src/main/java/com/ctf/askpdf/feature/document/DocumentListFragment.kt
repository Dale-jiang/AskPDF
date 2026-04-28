package com.ctf.askpdf.feature.document

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.print.PrintManager
import android.text.InputType
import android.widget.Toast
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import com.ctf.askpdf.R
import com.ctf.askpdf.app.AppLifecycleUtils
import com.ctf.askpdf.databinding.DialogDocumentFileActionsBinding
import com.ctf.askpdf.databinding.FragmentDocumentListBinding
import com.ctf.askpdf.document.model.DocumentFile
import com.ctf.askpdf.document.model.DocumentKind
import com.ctf.askpdf.document.model.DocumentTab
import com.ctf.askpdf.document.print.DocumentPrintAdapter
import com.ctf.askpdf.presentation.adapter.DocumentFileAdapter
import com.ctf.askpdf.presentation.base.BaseActivity
import com.ctf.askpdf.presentation.base.BaseFragment
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
                if (documentTab == DocumentTab.COLLECTION) {
                    viewModel.toggleCollection(file)
                } else {
                    showDocumentActionSheet(file)
                }
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

    override fun onResume() {
        super.onResume()
        AppLifecycleUtils.markNavigatingToSetting(false)
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

    /**
     * 展示文件更多操作弹窗，并绑定各操作入口。
     */
    private fun showDocumentActionSheet(file: DocumentFile) {
        val sheetBinding = DialogDocumentFileActionsBinding.inflate(layoutInflater)
        val dialog = BottomSheetDialog(requireContext()).apply {
            setContentView(sheetBinding.root)
            setOnShowListener {
                findViewById<android.widget.FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
                    ?.background = ColorDrawable(Color.TRANSPARENT)
            }
        }
        sheetBinding.dialogFileIcon.setImageResource(file.resolveType()?.iconRes ?: R.drawable.ic_pdf)
        sheetBinding.dialogFileName.text = file.displayName
        sheetBinding.dialogFilePath.text = file.path
        sheetBinding.btnCollection.setImageResource(
            if (file.collected || documentTab == DocumentTab.COLLECTION) R.drawable.ic_collection_yes else R.drawable.ic_collection_no
        )
        val isPdf = file.resolveType() == DocumentKind.PDF
        sheetBinding.btnPrint.isVisible = isPdf
        sheetBinding.printDivider.isVisible = isPdf
        sheetBinding.btnCollection.setOnClickListener {
            sheetBinding.btnCollection.isEnabled = false
            viewModel.toggleCollection(file) { updatedFile ->
                sheetBinding.btnCollection.setImageResource(
                    if (updatedFile.collected) R.drawable.ic_collection_yes else R.drawable.ic_collection_no
                )
                dialog.dismiss()
            }
        }
        sheetBinding.btnRename.setOnClickListener {
            dialog.dismiss()
            showRenameDialog(file)
        }
        sheetBinding.btnShare.setOnClickListener {
            dialog.dismiss()
            shareDocument(file)
        }
        sheetBinding.btnPrint.setOnClickListener {
            dialog.dismiss()
            printDocument(file)
        }
        sheetBinding.btnDelete.setOnClickListener {
            dialog.dismiss()
            deleteDocument(file)
        }
        dialog.show()
    }

    /**
     * 弹出重命名输入框，并提交新的文件名。
     */
    private fun showRenameDialog(file: DocumentFile) {
        val inputView = AppCompatEditText(requireContext()).apply {
            setSingleLine(true)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            hint = getString(R.string.file_name)
            setText(file.displayName.substringBeforeLast('.', file.displayName))
            setSelection(0, text?.length ?: 0)
            setPadding(40, 30, 40, 30)
        }
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.rename_file)
            .setView(inputView)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.confirm, null)
            .show()
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
            val nextName = inputView.text?.toString().orEmpty()
            viewModel.renameDocument(requireContext(), file, nextName) { success, errorRes ->
                if (success) {
                    dialog.dismiss()
                } else if (errorRes != null) {
                    Toast.makeText(requireContext(), errorRes, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * 通过系统分享面板发送当前文件。
     */
    private fun shareDocument(file: DocumentFile) {
        runCatching {
            val uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileProvider",
                File(file.path)
            )
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = file.mimeType.ifBlank { "*/*" }
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(sendIntent, getString(R.string.share)))
        }.onFailure {
            Toast.makeText(requireContext(), R.string.share_file_failed, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 调起系统打印服务打印 PDF 文件。
     */
    private fun printDocument(file: DocumentFile) {
        runCatching {
            AppLifecycleUtils.markNavigatingToSetting(true)
            val printContext = (requireActivity() as? BaseActivity<*>)?.printContext ?: requireActivity()
            val printManager = printContext.getSystemService(Context.PRINT_SERVICE) as PrintManager
            printManager.print(
                file.displayName,
                DocumentPrintAdapter(File(file.path), file.displayName) {
                    AppLifecycleUtils.markNavigatingToSetting(false)
                },
                null
            )
        }.onFailure {
            AppLifecycleUtils.markNavigatingToSetting(false)
            Toast.makeText(requireContext(), R.string.print_file_failed, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 删除当前文件，并在失败时提示用户。
     */
    private fun deleteDocument(file: DocumentFile) {
        viewModel.deleteDocument(requireContext(), file) { success, errorRes ->
            if (success.not() && errorRes != null) {
                Toast.makeText(requireContext(), errorRes, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
