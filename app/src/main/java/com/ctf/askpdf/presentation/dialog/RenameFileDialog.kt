package com.ctf.askpdf.presentation.dialog

import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.DialogFragment
import com.ctf.askpdf.databinding.DialogRenameFileBinding

class RenameFileDialog : DialogFragment() {

    companion object {
        private const val ARG_INITIAL_NAME = "arg_initial_name"

        fun newInstance(initialName: String, onConfirm: (String, (Boolean) -> Unit) -> Unit): RenameFileDialog {
            return RenameFileDialog().apply {
                arguments = Bundle().apply { putString(ARG_INITIAL_NAME, initialName) }
                confirmResult = onConfirm
            }
        }
    }

    private var viewBinding: DialogRenameFileBinding? = null
    private var confirmResult: ((String, (Boolean) -> Unit) -> Unit)? = null
    private val initialName by lazy { requireArguments().getString(ARG_INITIAL_NAME).orEmpty() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        viewBinding = DialogRenameFileBinding.inflate(inflater, container, false)
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
        bindInitialName()
        bindActions()
        focusFileNameInput()
    }

    /**
     * 填充当前文件名并选中文本，方便用户直接编辑。
     */
    private fun bindInitialName() {
        viewBinding?.editFileName?.apply {
            setText(initialName)
            setSelection(0, text?.length ?: 0)
        }
    }

    /**
     * 绑定取消和确认按钮，确认结果由外部执行重命名后回调。
     */
    private fun bindActions() {
        viewBinding?.apply {
            btnCancel.setOnClickListener {
                hideKeyboard()
                dismissAllowingStateLoss()
            }
            btnConfirm.setOnClickListener {
                val nextName = editFileName.text?.toString().orEmpty()
                btnConfirm.isEnabled = false
                confirmResult?.invoke(nextName) { success ->
                    btnConfirm.isEnabled = true
                    if (success) {
                        hideKeyboard()
                        dismissAllowingStateLoss()
                    }
                }
            }
        }
    }

    /**
     * 聚焦文件名输入框并主动唤起软键盘。
     */
    private fun focusFileNameInput() {
        viewBinding?.editFileName?.postDelayed({
            viewBinding?.editFileName?.requestFocus()
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.showSoftInput(viewBinding?.editFileName, InputMethodManager.SHOW_IMPLICIT)
        }, 250L)
    }

    /**
     * 隐藏文件名输入键盘，避免关闭弹窗后继续占位。
     */
    private fun hideKeyboard() {
        val input = viewBinding?.editFileName ?: return
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(input.windowToken, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewBinding = null
    }
}
