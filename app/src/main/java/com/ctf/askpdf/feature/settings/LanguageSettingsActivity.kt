package com.ctf.askpdf.feature.settings

import android.content.Intent
import android.os.Bundle
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.ctf.askpdf.MainActivity
import com.ctf.askpdf.data.local.selectedLanguageTag
import com.ctf.askpdf.databinding.ActivityLanguageSettingsBinding
import com.ctf.askpdf.presentation.base.BaseActivity
import java.util.Locale

class LanguageSettingsActivity : BaseActivity<ActivityLanguageSettingsBinding>(ActivityLanguageSettingsBinding::inflate) {

    private val allLanguageOptions = listOf(
        LanguageOption("English", "en"),
        LanguageOption("繁體中文", "zh-TW"),
        LanguageOption("日本語", "ja"),
        LanguageOption("한국어", "ko"),
        LanguageOption("Italiano", "it"),
        LanguageOption("Deutsch", "de"),
        LanguageOption("Français", "fr"),
        LanguageOption("Português", "pt"),
        LanguageOption("Español", "es"),
        LanguageOption("ภาษาไทย", "th"),
        LanguageOption("Bahasa Indonesia", "id"),
        LanguageOption("हिन्दी", "hi"),
        LanguageOption("العربية", "ar")
    )
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
     * 获取当前应选中的语言，未保存时跟随系统语言，匹配不到则使用英语。
     */
    private fun resolveCurrentLanguage(): LanguageOption {
        if (selectedLanguageTag.isNotBlank()) {
            findLanguageByTag(selectedLanguageTag)?.let { return it }
        }
        val systemLocale = Locale.getDefault()
        return findLanguageByTag(systemLocale.toLanguageTag())
            ?: allLanguageOptions.firstOrNull { it.languageTag.substringBefore("-") == systemLocale.language.normalizeLanguageCode() }
            ?: allLanguageOptions.first()
    }

    /**
     * 将当前选中语言移动到列表顶部。
     */
    private fun buildVisibleLanguageOptions(): List<LanguageOption> {
        val selectedOption = resolveCurrentLanguage()
        return listOf(selectedOption) + allLanguageOptions.filterNot { it.languageTag == selectedOption.languageTag }
    }

    /**
     * 按完整 tag 或语言码匹配支持的语言。
     */
    private fun findLanguageByTag(languageTag: String): LanguageOption? {
        val normalizedTag = languageTag.normalizeLanguageCode()
        return allLanguageOptions.firstOrNull { it.languageTag.normalizeLanguageCode() == normalizedTag }
            ?: allLanguageOptions.firstOrNull {
                it.languageTag.substringBefore("-").normalizeLanguageCode() == normalizedTag.substringBefore("-")
            }
    }

    /**
     * 兼容 Android 对印尼语的历史语言码。
     */
    private fun String.normalizeLanguageCode(): String {
        return lowercase(Locale.US).let { if (it == "in") "id" else it }
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
