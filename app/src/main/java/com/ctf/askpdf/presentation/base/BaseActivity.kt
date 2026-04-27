package com.ctf.askpdf.presentation.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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
        setDensity()
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

    override fun onAttachedToWindow() = myEnableEdgeToEdge()

    fun AppCompatActivity.myEnableEdgeToEdge(topView: ViewGroup? = null, bottomView: ViewGroup? = null,topPadding: Boolean = true, bottomPadding: Boolean = true) {
        try {
            enableEdgeToEdge()
            val listenerView = window.decorView
            val paddingTopTarget = topView ?: listenerView
            val paddingBottomTarget = bottomView ?: listenerView
            ViewCompat.setOnApplyWindowInsetsListener(listenerView) { _, insets ->
                val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                val topInset = if (topPadding) bars.top else 0
                val bottomInset = if (bottomPadding) bars.bottom else 0
                paddingTopTarget.setPadding(0, topInset, 0, 0)
                paddingBottomTarget.setPadding(0, 0, 0, bottomInset)
                insets
            }
        } catch (throwable: Throwable) {
            throwable.printStackTrace()
        }
    }

    @Suppress("DEPRECATION")
    private fun AppCompatActivity.setDensity() {
        resources.displayMetrics.apply {
            density = heightPixels / 765f
            densityDpi = (density * 160).toInt()
            scaledDensity = density
        }
    }
}
