package com.ctf.askpdf.presentation.base

import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding

abstract class BaseActivity<VB : ViewBinding>(
    private val inflateBinding: (LayoutInflater) -> VB
) : AppCompatActivity() {

    protected lateinit var binding: VB
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = inflateBinding(layoutInflater)
        setContentView(binding.root)
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
}
