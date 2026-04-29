package com.ctf.askpdf.presentation.base

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.viewbinding.ViewBinding
import com.ctf.askpdf.core.locale.AppLanguageConfig
import com.ctf.askpdf.data.local.selectedLanguageTag

abstract class BaseActivity<VB : ViewBinding>(private val inflateBinding: (LayoutInflater) -> VB) : AppCompatActivity() {

    var printContext: Context? = null

    protected lateinit var binding: VB
        private set

    override fun attachBaseContext(newBase: Context) {
        printContext = newBase
        val languageTag = AppLanguageConfig.resolveLanguageTag(selectedLanguageTag)
        super.attachBaseContext(AppLanguageConfig.wrapContext(newBase, languageTag))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = inflateBinding(layoutInflater)
        setContentView(binding.root)
        setDensity()
        applyNavigationBarVisibility()
        initView(savedInstanceState)
        initData()
        observeData()
    }

    override fun onResume() {
        super.onResume()
        applyNavigationBarVisibility()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) applyNavigationBarVisibility()
    }

    /**
     * 是否隐藏虚拟导航栏，主页重写为 false。
     */
    protected open fun shouldHideNavigationBar(): Boolean = true

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
            applyNavigationBarVisibility()
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

    /**
     * 仅控制虚拟导航栏显隐，保留状态栏。
     */
    private fun applyNavigationBarVisibility() {
        runCatching {
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (shouldHideNavigationBar()) {
                controller.hide(WindowInsetsCompat.Type.navigationBars())
            } else {
                controller.show(WindowInsetsCompat.Type.navigationBars())
            }
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
