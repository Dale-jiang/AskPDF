package com.ctf.askpdf.presentation.dialog

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.ctf.askpdf.R
import com.ctf.askpdf.databinding.DialogCreatingPdfBinding

class CreatingPdfDialog : DialogFragment() {

    companion object {
        private const val ARG_MESSAGE_RES = "arg_message_res"

        fun newInstance(messageRes: Int = R.string.creating_pdf): CreatingPdfDialog {
            return CreatingPdfDialog().apply {
                arguments = Bundle().apply { putInt(ARG_MESSAGE_RES, messageRes) }
            }
        }
    }

    private var viewBinding: DialogCreatingPdfBinding? = null
    private val messageRes by lazy { requireArguments().getInt(ARG_MESSAGE_RES, R.string.creating_pdf) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        viewBinding = DialogCreatingPdfBinding.inflate(inflater, container, false)
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
        viewBinding?.textMessage?.setText(messageRes)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewBinding = null
    }
}
