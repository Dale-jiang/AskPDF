package com.ctf.askpdf.feature.settings

import android.content.Intent
import android.os.Bundle
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.ctf.askpdf.MainActivity
import com.ctf.askpdf.core.locale.AppLanguageConfig
import com.ctf.askpdf.data.local.selectedLanguageTag
import com.ctf.askpdf.databinding.ActivityLanguageSettingsBinding
import com.ctf.askpdf.presentation.base.BaseActivity

class LanguageSettingsActivity : BaseActivity<ActivityLanguageSettingsBinding>(ActivityLanguageSettingsBinding::inflate) {

    private lateinit var visibleLanguageOptions: List<LanguageOption>
    private lateinit var languageAdapter: LanguageOptionAdapter

    override fun onAttachedToWindow() = myEnableEdgeToEdge(binding.topBar)

    /**
     * 初始化语言列表、返回按钮和确认按钮。
     */
    override fun initView(savedInstanceState: Bundle?) {
        onBackPressedDispatcher.addCallback(this) { finish() }
        binding.btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        visibleLanguageOptions = buildVisibleLanguageOptions()
        languageAdapter = LanguageOptionAdapter(visibleLanguageOptions) {}
        languageAdapter.setInitialSelection(0)
        binding.recyclerView.itemAnimator = null
        binding.recyclerView.adapter = languageAdapter
        binding.btnApply.setOnClickListener { applySelectedLanguage() }
    }

    /**
     * 将当前语言移动到列表顶部。
     */
    private fun buildVisibleLanguageOptions(): List<LanguageOption> {
        val selectedTag = AppLanguageConfig.resolveLanguageTag(selectedLanguageTag)
        return AppLanguageConfig.sortedWithSelected(selectedTag).map {
            LanguageOption(it.displayName, it.languageTag)
        }
    }

    /**
     * 保存语言选择并重建主页面。
     */
    private fun applySelectedLanguage() {
        val tag = visibleLanguageOptions[languageAdapter.selectedIndex].languageTag
        selectedLanguageTag = tag
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
        startActivity(Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        })
    }
}
