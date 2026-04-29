package com.ctf.askpdf.presentation.dialog

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.ctf.askpdf.databinding.DialogDeleteConfirmBinding

class DeleteConfirmDialog : DialogFragment() {

    companion object {
        private const val ARG_FILE_NAME = "arg_file_name"

        fun newInstance(fileName: String, onConfirm: () -> Unit): DeleteConfirmDialog {
            return DeleteConfirmDialog().apply {
                arguments = Bundle().apply { putString(ARG_FILE_NAME, fileName) }
                confirmResult = onConfirm
            }
        }
    }

    private var viewBinding: DialogDeleteConfirmBinding? = null
    private var confirmResult: (() -> Unit)? = null
    private val fileName by lazy { requireArguments().getString(ARG_FILE_NAME).orEmpty() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        viewBinding = DialogDeleteConfirmBinding.inflate(inflater, container, false)
        return viewBinding!!.root
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.decorView?.background = null
        dialog?.window?.apply {
            setGravity(Gravity.CENTER)
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.setCanceledOnTouchOutside(false)
        dialog?.setCancelable(false)
        bindActions()
    }

    /**
     * 展示待删除文件名，并绑定取消与确认删除事件。
     */
    private fun bindActions() {
        viewBinding?.apply {
            fileName.text = this@DeleteConfirmDialog.fileName
            btnCancel.setOnClickListener { dismissAllowingStateLoss() }
            btnConfirm.setOnClickListener {
                confirmResult?.invoke()
                dismissAllowingStateLoss()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewBinding = null
    }
}
