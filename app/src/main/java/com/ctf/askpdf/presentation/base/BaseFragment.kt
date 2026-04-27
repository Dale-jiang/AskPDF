package com.ctf.askpdf.presentation.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding

abstract class BaseFragment<VB : ViewBinding>(
    private val inflateBinding: (LayoutInflater, ViewGroup?, Boolean) -> VB
) : Fragment() {

    private var _binding: VB? = null

    protected val binding: VB
        get() = checkNotNull(_binding) { "Binding is only valid between onCreateView and onDestroyView." }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = inflateBinding(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView(savedInstanceState)
        initData()
        observeData()
    }

    /**
     * 初始化页面控件、点击事件和基础 UI 状态。
     */
    protected open fun initView(savedInstanceState: Bundle?) = Unit

    /**
     * 初始化页面数据、参数解析或首次请求。
     */
    protected open fun initData() = Unit

    /**
     * 注册 LiveData、Flow 或其它状态监听。
     */
    protected open fun observeData() = Unit

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
