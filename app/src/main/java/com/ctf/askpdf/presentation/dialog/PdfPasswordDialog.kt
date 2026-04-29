package com.ctf.askpdf.presentation.dialog

import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.ctf.askpdf.R
import com.ctf.askpdf.databinding.DialogPdfPasswordBinding
import com.ctf.askpdf.document.PdfPasswordUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PdfPasswordDialog : DialogFragment() {

    companion object {
        private const val ARG_PATH = "arg_path"
        private const val ARG_DISPLAY_NAME = "arg_display_name"

        fun newInstance(
            path: String,
            displayName: String? = null,
            onResult: (String?) -> Unit
        ): PdfPasswordDialog {
            return PdfPasswordDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_PATH, path)
                    putString(ARG_DISPLAY_NAME, displayName)
                }
                passwordResult = onResult
            }
        }
    }

    private var viewBinding: DialogPdfPasswordBinding? = null
    private var passwordResult: ((String?) -> Unit)? = null
    private val pdfPath by lazy { requireArguments().getString(ARG_PATH).orEmpty() }
    private val displayName by lazy { requireArguments().getString(ARG_DISPLAY_NAME).orEmpty() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        viewBinding = DialogPdfPasswordBinding.inflate(inflater, container, false)
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
        bindDisplayName()
        bindActions()
        focusPasswordInput()
    }

    /**
     * 展示当前需要密码的 PDF 名称，空名称时保持阅读弹窗原样。
     */
    private fun bindDisplayName() {
        viewBinding?.fileNameTips?.apply {
            isVisible = displayName.isNotBlank()
            text = if (displayName.isBlank()) "" else getString(R.string.password_required_for_pdf, displayName)
        }
    }

    /**
     * 绑定密码确认和取消事件，校验成功后回传密码。
     */
    private fun bindActions() {
        viewBinding?.apply {
            btnConfirm.setOnClickListener {
                val pass = editPassword.text?.toString().orEmpty()
                if (pass.isBlank()) {
                    Toast.makeText(requireContext(), R.string.please_enter_password, Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                btnConfirm.isEnabled = false
                viewLifecycleOwner.lifecycleScope.launch {
                    val authenticated = withContext(Dispatchers.IO) {
                        PdfPasswordUtils.authenticatePassword(pdfPath, pass)
                    }
                    btnConfirm.isEnabled = true
                    if (authenticated) {
                        hideKeyboard()
                        passwordResult?.invoke(pass)
                        dismissAllowingStateLoss()
                    } else {
                        Toast.makeText(requireContext(), R.string.incorrect_password, Toast.LENGTH_SHORT).show()
                    }
                }
            }
            btnCancel.setOnClickListener {
                hideKeyboard()
                passwordResult?.invoke(null)
                dismissAllowingStateLoss()
            }
        }
    }

    /**
     * 聚焦密码输入框并主动唤起软键盘。
     */
    private fun focusPasswordInput() {
        viewBinding?.editPassword?.postDelayed({
            viewBinding?.editPassword?.requestFocus()
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.showSoftInput(viewBinding?.editPassword, InputMethodManager.SHOW_IMPLICIT)
        }, 250L)
    }

    /**
     * 隐藏密码输入键盘，避免关闭弹窗后继续占位。
     */
    private fun hideKeyboard() {
        val input = viewBinding?.editPassword ?: return
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(input.windowToken, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewBinding = null
    }
}
