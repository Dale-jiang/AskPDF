package com.ctf.askpdf.feature.settings

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.ctf.askpdf.R
import com.ctf.askpdf.databinding.FragmentSettingsStubBinding
import com.ctf.askpdf.feature.merge.MergePdfActivity
import com.ctf.askpdf.feature.split.SplitPdfActivity
import com.ctf.askpdf.presentation.base.BaseFragment

class SettingsPlaceholderFragment : BaseFragment<FragmentSettingsStubBinding>(FragmentSettingsStubBinding::inflate) {

    /**
     * 初始化设置页列表点击事件。
     */
    override fun initView(savedInstanceState: Bundle?) {
        binding.itemShareApp.setOnClickListener { shareAppLink() }
        binding.itemLanguage.setOnClickListener { openLanguageSettings() }
        binding.itemPrivacy.setOnClickListener { openPrivacyPolicy() }
        binding.itemMerge.setOnClickListener { openMergePdfPage() }
        binding.itemSplit.setOnClickListener { openSplitPdfPage() }
    }

    /**
     * 通过系统分享面板分享应用商店链接。
     */
    private fun shareAppLink() {
        runCatching {
            val appLink = "https://play.google.com/store/apps/details?id=${requireContext().packageName}"
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, getString(R.string.check_out_this_app))
                putExtra(Intent.EXTRA_TEXT, getString(R.string.share_app_text, appLink))
            }
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_app)))
        }.onFailure {
            Toast.makeText(requireContext(), R.string.share_app_failed, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 打开内置隐私政策页面。
     */
    private fun openPrivacyPolicy() {
        startActivity(Intent(requireContext(), PolicyViewerActivity::class.java).apply {
            putExtra(PolicyViewerActivity.EXTRA_PAGE_URL, PolicyViewerActivity.DEFAULT_PRIVACY_URL)
        })
    }

    /**
     * 打开多语言设置页面。
     */
    private fun openLanguageSettings() {
        startActivity(Intent(requireContext(), LanguageSettingsActivity::class.java))
    }

    /**
     * 从设置页进入 PDF 合并选择页。
     */
    private fun openMergePdfPage() {
        startActivity(Intent(requireContext(), MergePdfActivity::class.java))
    }

    /**
     * 从设置页进入 PDF 拆分选择页。
     */
    private fun openSplitPdfPage() {
        startActivity(Intent(requireContext(), SplitPdfActivity::class.java))
    }
}
