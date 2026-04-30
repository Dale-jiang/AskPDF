package com.ctf.askpdf.feature.settings

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.addCallback
import androidx.core.view.isVisible
import com.ctf.askpdf.R
import com.ctf.askpdf.databinding.ActivityPolicyViewerBinding
import com.ctf.askpdf.presentation.base.BaseActivity

class PolicyViewerActivity : BaseActivity<ActivityPolicyViewerBinding>(ActivityPolicyViewerBinding::inflate) {

    companion object {
        const val EXTRA_PAGE_URL = "com.ctf.askpdf.extra.PAGE_URL"
        const val DEFAULT_PRIVACY_URL = "https://sites.google.com/view/askpdf-privacy/home"
    }

    private val pageUrl by lazy { intent?.getStringExtra(EXTRA_PAGE_URL).orEmpty().ifBlank { DEFAULT_PRIVACY_URL } }

    override fun onAttachedToWindow() = myEnableEdgeToEdge(binding.topBar)

    /**
     * 初始化隐私政策页面标题、返回按钮和 WebView。
     */
    override fun initView(savedInstanceState: Bundle?) {
        binding.title.setText(R.string.privacy_policy)
        binding.btnBack.setOnClickListener { handleBackPressed() }
        onBackPressedDispatcher.addCallback(this) { handleBackPressed() }
        setupPolicyWebView()
    }

    /**
     * 配置 WebView 加载隐私政策链接。
     */
    @SuppressLint("SetJavaScriptEnabled")
    private fun setupPolicyWebView() {
        binding.policyWebView.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webViewClient = WebViewClient()
            webChromeClient = object : WebChromeClient() {
                override fun onReceivedTitle(view: WebView?, title: String?) {
                    binding.title.text = title?.takeIf { it.isNotBlank() } ?: getString(R.string.privacy_policy)
                }

                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    binding.loadProgress.progress = newProgress
                    binding.loadProgress.isVisible = newProgress < 100
                }
            }
            loadUrl(pageUrl)
        }
    }

    /**
     * WebView 有历史时后退网页，否则关闭页面。
     */
    private fun handleBackPressed() {
        if (binding.policyWebView.canGoBack()) {
            binding.policyWebView.goBack()
        } else {
            finish()
        }
    }

    override fun onDestroy() {
        binding.policyWebView.apply {
            stopLoading()
            loadUrl("about:blank")
            clearHistory()
            removeAllViews()
            destroy()
        }
        super.onDestroy()
    }
}
