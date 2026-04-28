package com.ctf.askpdf.core.locale

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

data class AppLanguage(
    val displayName: String,
    val languageTag: String
)

object AppLanguageConfig {

    val supportedLanguages = listOf(
        AppLanguage("English", "en"),
        AppLanguage("繁體中文", "zh-TW"),
        AppLanguage("日本語", "ja"),
        AppLanguage("한국어", "ko"),
        AppLanguage("Italiano", "it"),
        AppLanguage("Deutsch", "de"),
        AppLanguage("Français", "fr"),
        AppLanguage("Português", "pt"),
        AppLanguage("Español", "es"),
        AppLanguage("ภาษาไทย", "th"),
        AppLanguage("Bahasa Indonesia", "id"),
        AppLanguage("हिन्दी", "hi"),
        AppLanguage("العربية", "ar")
    )

    /**
     * 获取当前应使用的语言，未保存时跟随系统语言，匹配不到则使用英语。
     */
    fun resolveLanguageTag(savedTag: String, systemLocale: Locale = Locale.getDefault()): String {
        if (savedTag.isNotBlank()) {
            findLanguageByTag(savedTag)?.let { return it.languageTag }
        }
        return findLanguageByTag(systemLocale.toLanguageTag())?.languageTag
            ?: supportedLanguages.firstOrNull { it.languageTag.substringBefore("-") == systemLocale.language.normalizeLanguageCode() }?.languageTag
            ?: supportedLanguages.first().languageTag
    }

    /**
     * 将当前语言移动到列表顶部。
     */
    fun sortedWithSelected(selectedTag: String): List<AppLanguage> {
        val selectedLanguage = findLanguageByTag(selectedTag) ?: supportedLanguages.first()
        return listOf(selectedLanguage) + supportedLanguages.filterNot { it.languageTag == selectedLanguage.languageTag }
    }

    /**
     * 用指定语言包装 Context，保证 Activity 布局创建前已应用语言。
     */
    fun wrapContext(context: Context, languageTag: String): Context {
        val locale = Locale.forLanguageTag(languageTag)
        Locale.setDefault(locale)
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(locale)
        configuration.setLayoutDirection(locale)
        return context.createConfigurationContext(configuration)
    }

    /**
     * 按完整 tag 或语言码匹配支持的语言。
     */
    private fun findLanguageByTag(languageTag: String): AppLanguage? {
        val normalizedTag = languageTag.normalizeLanguageCode()
        return supportedLanguages.firstOrNull { it.languageTag.normalizeLanguageCode() == normalizedTag }
            ?: supportedLanguages.firstOrNull {
                it.languageTag.substringBefore("-").normalizeLanguageCode() == normalizedTag.substringBefore("-")
            }
    }

    /**
     * 兼容 Android 对印尼语的历史语言码。
     */
    private fun String.normalizeLanguageCode(): String {
        return lowercase(Locale.US).let { if (it == "in") "id" else it }
    }
}
